package com.learn.assistant.service.impl;

import com.learn.assistant.chat.ChatAnswer;
import com.learn.assistant.chat.ChatMode;
import com.learn.assistant.chat.ChatPiece;
import com.learn.assistant.service.ChatService;
import com.learn.assistant.chat.ConversationClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatServiceImpl implements ChatService {

    private final ConversationClient conversationClient;

    private final ChatMemory chatMemory;

    public ChatServiceImpl(ConversationClient conversationClient, ChatMemory chatMemory) {
        this.conversationClient = conversationClient;
        this.chatMemory = chatMemory;
    }

    @Override
    public ChatAnswer reply(String message, String conversationId, ChatMode mode) {
        return conversationClient.chat(message, conversationId, mode);
    }

    @Override
    public Flux<ChatPiece> stream(String message, String conversationId, ChatMode mode) {
        return conversationClient.stream(message, conversationId, mode);
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        chatMemory.clear(conversationId);
    }
}
