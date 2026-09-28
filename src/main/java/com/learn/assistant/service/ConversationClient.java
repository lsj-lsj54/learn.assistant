package com.learn.assistant.service;

public interface ConversationClient {

    String chat(String message, String conversationId);
}
