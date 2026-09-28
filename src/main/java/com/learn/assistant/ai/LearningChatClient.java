package com.learn.assistant.ai;

import com.learn.assistant.tool.FileOperationTool;
import com.learn.assistant.tool.PdfWriteTool;
import com.learn.assistant.tool.ResourceDownloadTool;
import com.learn.assistant.tool.WebScrapeTool;
import com.learn.assistant.tool.WebSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;

@Component
public class LearningChatClient {

    private final ChatClient chatClient;

    public LearningChatClient(ChatClient.Builder builder, RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
            PdfWriteTool pdfWriteTool, FileOperationTool fileOperationTool, WebSearchTool webSearchTool,
            ResourceDownloadTool resourceDownloadTool, WebScrapeTool webScrapeTool) {
        this.chatClient = builder
                .defaultSystem("你是学习助手，用简洁的中文回答。")
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .defaultTools(pdfWriteTool, fileOperationTool, webSearchTool, resourceDownloadTool, webScrapeTool)
                .build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
