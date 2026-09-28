package com.learn.assistant.domain.vo;

public record AppSettingsResponse(
        int scrapeMaxChars,
        int readMaxChars,
        int maxMemoryMessages,
        boolean allowEmptyContext) {
}
