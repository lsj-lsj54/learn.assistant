package com.learn.assistant.chat;

import com.learn.assistant.domain.vo.ChatSource;

public record ChatPiece(Kind kind, String text, ChatSource source) {

    public enum Kind {
        STATUS,
        DELTA,
        SOURCE
    }

    public static ChatPiece status(String text) {
        return new ChatPiece(Kind.STATUS, text, null);
    }

    public static ChatPiece delta(String text) {
        return new ChatPiece(Kind.DELTA, text, null);
    }

    public static ChatPiece source(ChatSource source) {
        return new ChatPiece(Kind.SOURCE, null, source);
    }
}
