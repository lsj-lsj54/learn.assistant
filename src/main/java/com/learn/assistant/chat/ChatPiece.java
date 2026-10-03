package com.learn.assistant.chat;

import com.learn.assistant.domain.vo.ChatSource;
import com.learn.assistant.tool.activity.ToolStep;

public record ChatPiece(Kind kind, String text, ChatSource source, ToolStep tool) {

    public enum Kind {
        STATUS,
        DELTA,
        SOURCE,
        TOOL
    }

    public static ChatPiece status(String text) {
        return new ChatPiece(Kind.STATUS, text, null, null);
    }

    public static ChatPiece delta(String text) {
        return new ChatPiece(Kind.DELTA, text, null, null);
    }

    public static ChatPiece source(ChatSource source) {
        return new ChatPiece(Kind.SOURCE, null, source, null);
    }

    public static ChatPiece tool(ToolStep tool) {
        return new ChatPiece(Kind.TOOL, null, null, tool);
    }
}
