package com.learn.assistant.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class LearningChatClient {

    private final ChatClient chatClient;

    public LearningChatClient(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是学习助手，用简洁的中文回答。")
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
