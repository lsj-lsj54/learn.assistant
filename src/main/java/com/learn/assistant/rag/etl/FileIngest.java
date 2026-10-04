package com.learn.assistant.rag.etl;

public record FileIngest(String file, String status, int added, int skipped, String message) {

    public static final String ADDED = "added";

    public static final String SKIPPED = "skipped";

    public static final String FAILED = "failed";

    public static FileIngest added(String file, int added, int skipped) {
        return new FileIngest(file, ADDED, added, skipped, null);
    }

    public static FileIngest skipped(String file, int skipped, String message) {
        return new FileIngest(file, SKIPPED, 0, skipped, message);
    }

    public static FileIngest failed(String file, String message) {
        String text = message == null || message.isBlank() ? "无法读取" : message;
        return new FileIngest(file == null ? "" : file, FAILED, 0, 0, text);
    }
}
