package com.learn.assistant.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSearchToolTest {

    @Test
    void searchReturnsResultsOrFailureMessage() {
        String result = new WebSearchTool().search("junit");

        assertFalse(result.isBlank());
        assertTrue(result.contains("http") || result.startsWith("搜索失败") || result.equals("没有搜索到结果"));
    }
}
