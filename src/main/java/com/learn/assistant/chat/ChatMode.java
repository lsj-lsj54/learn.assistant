package com.learn.assistant.chat;

public enum ChatMode {

    STUDY,

    TASK;

    public static ChatMode from(String raw) {
        if (raw != null && (raw.equalsIgnoreCase("task") || "办事情".equals(raw))) {
            return TASK;
        }
        return STUDY;
    }

    public String waitingStatus() {
        return this == TASK ? "正在思考" : "正在检索";
    }
}
