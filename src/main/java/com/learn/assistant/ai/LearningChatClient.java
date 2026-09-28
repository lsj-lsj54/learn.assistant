package com.learn.assistant.ai;

import com.learn.assistant.tool.FileOperationTool;
import com.learn.assistant.tool.PdfWriteTool;
import com.learn.assistant.tool.ResourceDownloadTool;
import com.learn.assistant.tool.WebScrapeTool;
import com.learn.assistant.tool.WebSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Component;

@Component
public class LearningChatClient {

    private final ChatClient chatClient;

    public LearningChatClient(ChatClient.Builder builder, MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor, PdfWriteTool pdfWriteTool,
            FileOperationTool fileOperationTool, WebSearchTool webSearchTool,
            ResourceDownloadTool resourceDownloadTool, WebScrapeTool webScrapeTool) {
        this.chatClient = builder
                .defaultSystem("你是学习助手，用简洁的中文回答。")
                .defaultAdvisors(messageChatMemoryAdvisor, retrievalAugmentationAdvisor)
                .defaultTools(pdfWriteTool, fileOperationTool, webSearchTool, resourceDownloadTool, webScrapeTool)
                .build();
    }

    public String chat(String message, String conversationId) {
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
