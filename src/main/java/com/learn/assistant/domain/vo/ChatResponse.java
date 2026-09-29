package com.learn.assistant.domain.vo;

import java.util.List;

public record ChatResponse(String reply, List<ChatSource> sources) {
}
