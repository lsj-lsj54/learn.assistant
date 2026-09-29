package com.learn.assistant.domain.vo;

public record FileIngestResponse(String file, String status, int addedCount, int skippedCount, String message) {
}
