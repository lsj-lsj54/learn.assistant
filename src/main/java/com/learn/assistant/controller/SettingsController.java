package com.learn.assistant.controller;

import com.learn.assistant.domain.vo.AppSettingsResponse;
import com.learn.assistant.properties.ChatProperties;
import com.learn.assistant.properties.RagProperties;
import com.learn.assistant.properties.ToolProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final ToolProperties toolProperties;

    private final ChatProperties chatProperties;

    private final RagProperties ragProperties;

    public SettingsController(ToolProperties toolProperties, ChatProperties chatProperties, RagProperties ragProperties) {
        this.toolProperties = toolProperties;
        this.chatProperties = chatProperties;
        this.ragProperties = ragProperties;
    }

    @GetMapping
    public AppSettingsResponse settings() {
        return new AppSettingsResponse(
                ToolProperties.positive(toolProperties.getScrapeMaxChars(), 8_000),
                ToolProperties.positive(toolProperties.getReadMaxChars(), 100_000),
                chatProperties.getMaxMemoryMessages(),
                ragProperties.getRetrieval().isAllowEmptyContext());
    }
}
