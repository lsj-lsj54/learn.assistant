package com.learn.assistant.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.assistant.properties.ToolProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSearchToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchFailsWhenApiKeyMissing() {
        String result = new WebSearchTool(new ToolProperties()).search("junit");

        assertEquals("搜索失败: 未配置博查 API Key", result);
    }

    @Test
    void formatsTitlesAndLinks() throws Exception {
        String result = WebSearchTool.formatResults("""
                {
                  "webPages": {
                    "value": [
                      {"name": "JUnit 5", "url": "https://junit.org/junit5/"},
                      {"name": "", "url": "https://example.com/skip"},
                      {"name": "第二页", "url": "https://example.com/2"}
                    ]
                  }
                }
                """, objectMapper);

        assertTrue(result.contains("JUnit 5"));
        assertTrue(result.contains("https://junit.org/junit5/"));
        assertTrue(result.contains("第二页"));
        assertFalse(result.contains("skip"));
    }

    @Test
    void readsWrappedBochaPayload() throws Exception {
        String result = WebSearchTool.formatResults("""
                {"code":200,"data":{"webPages":{"value":[{"name":"标题","url":"https://example.com/a"}]}}}
                """, objectMapper);

        assertEquals("网页：\n标题\nhttps://example.com/a", result);
    }

    @Test
    void separatesImageLinksForDownload() throws Exception {
        String result = WebSearchTool.formatResults("""
                {
                  "webPages": {"value": [{"name": "介绍页", "url": "https://example.com/page"}]},
                  "images": {"value": [{"name": "威龙", "contentUrl": "https://img.example.com/a.jpg", "hostPageUrl": "https://example.com/page"}]}
                }
                """, objectMapper);

        assertTrue(result.indexOf("图片直链") < result.indexOf("网页"));
        assertTrue(result.contains("网页：\n介绍页\nhttps://example.com/page"));
        assertTrue(result.contains("图片直链：\n威龙\nhttps://img.example.com/a.jpg"));
        assertFalse(result.contains("hostPageUrl"));
    }

    @Test
    void reportsApiErrorWhenNoPages() throws Exception {
        String result = WebSearchTool.formatResults("""
                {"code":401,"msg":"Invalid API key"}
                """, objectMapper);

        assertEquals("搜索失败: Invalid API key", result);
    }
}
