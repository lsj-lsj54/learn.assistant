package com.learn.assistant.chat;

import reactor.core.publisher.Flux;

public interface ConversationClient {

    ChatAnswer chat(String message, String conversationId, ChatMode mode);

    Flux<ChatPiece> stream(String message, String conversationId, ChatMode mode);
}
