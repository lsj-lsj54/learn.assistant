package com.learn.assistant.service;

import com.learn.assistant.chat.ChatAnswer;
import com.learn.assistant.chat.ChatMode;
import com.learn.assistant.chat.ChatPiece;
import reactor.core.publisher.Flux;

public interface ChatService {

    ChatAnswer reply(String message, String conversationId, ChatMode mode);

    Flux<ChatPiece> stream(String message, String conversationId, ChatMode mode);

    void clear(String conversationId);
}
