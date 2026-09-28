package com.learn.assistant.service;

import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ConversationClient conversationClient;

    public ChatService(ConversationClient conversationClient) {
        this.conversationClient = conversationClient;
    }

    public String reply(String message, String conversationId) {
        return conversationClient.chat(message, conversationId);
    }
}
