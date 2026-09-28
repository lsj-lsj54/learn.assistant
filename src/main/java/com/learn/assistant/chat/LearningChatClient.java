package com.learn.assistant.chat;

import com.learn.assistant.service.ConversationClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

@Component
public class LearningChatClient implements ConversationClient {

    private final ChatClient chatClient;

    public LearningChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
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
