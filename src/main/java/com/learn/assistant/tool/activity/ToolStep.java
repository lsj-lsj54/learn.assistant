package com.learn.assistant.tool.activity;

public record ToolStep(String name, String arguments, String result) {

    public String wire() {
        return "{\"name\":\"" + escape(name) + "\",\"arguments\":\"" + escape(arguments) + "\",\"result\":\""
                + escape(result) + "\"}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }
}
