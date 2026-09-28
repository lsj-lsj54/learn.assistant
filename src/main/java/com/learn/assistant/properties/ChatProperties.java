package com.learn.assistant.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learn.chat")
public class ChatProperties {

    public static final String DEFAULT_SYSTEM_PROMPT = """
            你是学习助手，用简洁的中文回答。记住同一段对话里用户说过的信息，并在后续问题中使用。
            一件事需要多个工具时，在同一次回答里按顺序调用。用户要下载文件时，先搜索拿到直链，再调用下载工具。PDF 保存到 pdf 目录，其他资源保存到 res。保存路径必须是「分类/文件名」。先根据内容选择已有分类，例如威龙放进游戏，泰山放进风景；没有合适分类就新建一个简短中文分类。不要只把链接留给用户，也不要改去抓取网页。
            不要根据以前的失败判断工具不可用。每次都重新调用工具，只有这次工具返回未配置时才能说密钥没有配置。
            """;

    public static final int DEFAULT_MAX_MEMORY_MESSAGES = 20;

    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    private int maxMemoryMessages = DEFAULT_MAX_MEMORY_MESSAGES;

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getMaxMemoryMessages() {
        return maxMemoryMessages;
    }

    public void setMaxMemoryMessages(int maxMemoryMessages) {
        this.maxMemoryMessages = maxMemoryMessages;
    }

    public String systemPromptOrDefault() {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return systemPrompt;
    }

    public void validate() {
        if (maxMemoryMessages < 1) {
            throw new IllegalStateException("learn.chat.max-memory-messages 必须大于 0");
        }
    }
}
