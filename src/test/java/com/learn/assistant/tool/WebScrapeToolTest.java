package com.learn.assistant.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebScrapeToolTest {

    private final WebScrapeTool tool = new WebScrapeTool(new PublicHttp());

    @Test
    void rejectsPrivateAddress() {
        assertTrue(tool.scrape("http://127.0.0.1").contains("不允许访问内网地址"));
    }

    @Test
    void scrapesPublicPageText() {
        String text = tool.scrape("https://example.com");

        assertTrue(text.contains("Example Domain"));
    }
}
