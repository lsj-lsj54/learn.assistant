package com.learn.assistant.service;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ConversationClient conversationClient;

    private final ChatMemory chatMemory;

    public ChatService(ConversationClient conversationClient, ChatMemory chatMemory) {
        this.conversationClient = conversationClient;
        this.chatMemory = chatMemory;
    }

    public String reply(String message, String conversationId) {
        return conversationClient.chat(message, conversationId);
    }

    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        chatMemory.clear(conversationId);
    }
}
