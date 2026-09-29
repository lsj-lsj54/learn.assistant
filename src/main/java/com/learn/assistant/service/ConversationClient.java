package com.learn.assistant.service;

import reactor.core.publisher.Flux;

public interface ConversationClient {

    String chat(String message, String conversationId);

    Flux<String> stream(String message, String conversationId);
}
