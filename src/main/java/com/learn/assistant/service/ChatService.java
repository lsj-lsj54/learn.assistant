package com.learn.assistant.service;

import com.learn.assistant.chat.ChatMode;
import com.learn.assistant.chat.ChatPiece;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private final ConversationClient conversationClient;

    private final ChatMemory chatMemory;

    public ChatService(ConversationClient conversationClient, ChatMemory chatMemory) {
        this.conversationClient = conversationClient;
        this.chatMemory = chatMemory;
    }

    public ChatAnswer reply(String message, String conversationId, ChatMode mode) {
        return conversationClient.chat(message, conversationId, mode);
    }

    public Flux<ChatPiece> stream(String message, String conversationId, ChatMode mode) {
        return conversationClient.stream(message, conversationId, mode);
    }

    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        chatMemory.clear(conversationId);
    }
}
