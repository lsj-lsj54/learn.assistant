package com.learn.assistant.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learn.chat")
public class ChatProperties {

    public static final String DEFAULT_SYSTEM_PROMPT = "你是学习助手，用简洁的中文回答。";

    public static final int DEFAULT_MAX_MEMORY_MESSAGES = 20;

    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    private int maxMemoryMessages = DEFAULT_MAX_MEMORY_MESSAGES;

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getMaxMemoryMessages() {
        return maxMemoryMessages;
    }

    public void setMaxMemoryMessages(int maxMemoryMessages) {
        this.maxMemoryMessages = maxMemoryMessages;
    }

    public String systemPromptOrDefault() {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return systemPrompt;
    }

    public void validate() {
        if (maxMemoryMessages < 1) {
            throw new IllegalStateException("learn.chat.max-memory-messages 必须大于 0");
        }
    }
}
