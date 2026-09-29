package com.learn.assistant.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message, @NotBlank String conversationId, String mode) {
}
