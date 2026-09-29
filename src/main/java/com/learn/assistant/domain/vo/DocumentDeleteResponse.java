package com.learn.assistant.domain.vo;

public record DocumentDeleteResponse(String file, int deletedCount, boolean fileRemoved) {
}
