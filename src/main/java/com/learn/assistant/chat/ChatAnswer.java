package com.learn.assistant.chat;

import com.learn.assistant.domain.vo.ChatSource;

import java.util.List;

public record ChatAnswer(String reply, List<ChatSource> sources) {

    public ChatAnswer {
        sources = sources == null ? List.of() : List.copyOf(sources);
        reply = reply == null ? "" : reply;
    }
}
