package com.learn.assistant.service;

import com.learn.assistant.ai.LearningChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final LearningChatClient learningChatClient;

    public ChatService(LearningChatClient learningChatClient) {
        this.learningChatClient = learningChatClient;
    }

    public String reply(String message, String conversationId) {
        return learningChatClient.chat(message, conversationId);
    }
}
