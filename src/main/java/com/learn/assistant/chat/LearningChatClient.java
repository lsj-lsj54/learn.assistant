package com.learn.assistant.chat;

import com.learn.assistant.prompts.ChatPrompts;
import com.learn.assistant.properties.ChatProperties;
import com.learn.assistant.service.ChatAnswer;
import com.learn.assistant.service.ConversationClient;
import com.learn.assistant.tool.activity.ToolActivity;
import com.learn.assistant.tool.activity.ToolNames;
import com.learn.assistant.tool.activity.ToolStep;
import com.learn.assistant.tool.config.AssistantToolCatalog;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class LearningChatClient implements ConversationClient {

    private final ChatClient chatClient;

    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    private final AssistantToolCatalog assistantToolCatalog;

    private final ChatProperties chatProperties;

    public LearningChatClient(ChatClient chatClient, MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor, AssistantToolCatalog assistantToolCatalog,
            ChatProperties chatProperties) {
        this.chatClient = chatClient;
        this.messageChatMemoryAdvisor = messageChatMemoryAdvisor;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.assistantToolCatalog = assistantToolCatalog;
        this.chatProperties = chatProperties;
    }

    @Override
    public ChatAnswer chat(String message, String conversationId, ChatMode mode) {
        ChatClientResponse response = prompt(message, conversationId, mode).call().chatClientResponse();
        return new ChatAnswer(text(response), ChatSources.from(response));
    }

    @Override
    public Flux<ChatPiece> stream(String message, String conversationId, ChatMode mode) {
        ToolActivity activity = new ToolActivity();
        AtomicBoolean sourcesSent = new AtomicBoolean();
        AtomicBoolean generating = new AtomicBoolean();
        Flux<ChatPiece> answer = prompt(message, conversationId, mode)
                .stream()
                .chatClientResponse()
                .contextWrite(context -> context.put(ToolActivity.CONTEXT_KEY, activity))
                .concatMap(response -> Flux.fromIterable(pieces(response, sourcesSent, generating, activity)))
                .concatWith(Flux.defer(() -> Flux.fromIterable(toolPieces(activity))))
                .onErrorResume(error -> Flux.concat(Flux.defer(() -> Flux.fromIterable(toolPieces(activity))),
                        Flux.error(error)));
        return Flux.concat(Flux.just(ChatPiece.status(mode.waitingStatus())), answer);
    }

    private ChatClient.ChatClientRequestSpec prompt(String message, String conversationId, ChatMode mode) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(mode == ChatMode.TASK ? chatProperties.systemPromptOrDefault() : ChatPrompts.STUDY)
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(messageChatMemoryAdvisor);
        if (mode == ChatMode.STUDY) {
            return spec.advisors(retrievalAugmentationAdvisor);
        }
        return spec.tools(assistantToolCatalog.toArray());
    }

    private static List<ChatPiece> pieces(ChatClientResponse response, AtomicBoolean sourcesSent,
            AtomicBoolean generating, ToolActivity activity) {
        List<ChatPiece> pieces = new ArrayList<>();
        pieces.addAll(toolPieces(activity));
        if (sourcesSent.compareAndSet(false, true)) {
            for (var source : ChatSources.from(response)) {
                pieces.add(ChatPiece.source(source));
            }
        }
        AssistantMessage output = output(response);
        if (output == null) {
            return pieces;
        }
        if (output.hasToolCalls()) {
            pieces.add(ChatPiece.status(toolStatus(output)));
        }
        String text = output.getText();
        if (text != null && !text.isEmpty()) {
            if (generating.compareAndSet(false, true)) {
                pieces.add(ChatPiece.status("正在生成"));
            }
            pieces.add(ChatPiece.delta(text));
        }
        return pieces;
    }

    private static List<ChatPiece> toolPieces(ToolActivity activity) {
        List<ChatPiece> pieces = new ArrayList<>();
        for (ToolStep step : activity.drain()) {
            pieces.add(ChatPiece.tool(step));
        }
        return pieces;
    }

    private static String toolStatus(AssistantMessage output) {
        List<String> names = new ArrayList<>();
        for (AssistantMessage.ToolCall call : output.getToolCalls()) {
            String name = ToolNames.display(call.name());
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        if (names.isEmpty()) {
            return "工具";
        }
        return String.join("、", names);
    }

    private static String text(ChatClientResponse response) {
        AssistantMessage output = output(response);
        if (output == null || output.getText() == null) {
            return "";
        }
        return output.getText();
    }

    private static AssistantMessage output(ChatClientResponse response) {
        if (response == null) {
            return null;
        }
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null) {
            return null;
        }
        return chatResponse.getResult().getOutput();
    }
}
