package com.learn.assistant.ai;

import com.learn.assistant.service.ConversationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;

@Component
public class LearningChatClient implements ConversationClient {

    private static final Logger log = LoggerFactory.getLogger(LearningChatClient.class);

    private final ChatClient chatClient;

    public LearningChatClient(ChatClient.Builder builder, MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor, AssistantToolCatalog assistantToolCatalog,
            ChatProperties chatProperties) {
        chatProperties.validate();
        Object[] tools = assistantToolCatalog.toArray();
        log.info("已注册 {} 个工具", tools.length);
        this.chatClient = builder
                .defaultSystem(chatProperties.systemPromptOrDefault())
                .defaultAdvisors(messageChatMemoryAdvisor, retrievalAugmentationAdvisor)
                .defaultTools(tools)
                .build();
    }

    @Override
    public String chat(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
