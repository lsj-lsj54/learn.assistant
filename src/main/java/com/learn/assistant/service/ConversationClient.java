package com.learn.assistant.service;

import com.learn.assistant.chat.ChatMode;
import com.learn.assistant.chat.ChatPiece;
import reactor.core.publisher.Flux;

public interface ConversationClient {

    ChatAnswer chat(String message, String conversationId, ChatMode mode);

    Flux<ChatPiece> stream(String message, String conversationId, ChatMode mode);
}
