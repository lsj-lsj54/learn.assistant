package com.learn.assistant.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolCallLogTest {

    @TempDir
    Path folder;

    @AfterEach
    void restoreLogDirectory() {
        ToolCallLog.useDirectory(Path.of("log"));
    }

    @Test
    void appendsTheCallToTheLogFolder() throws Exception {
        ToolCallLog.useDirectory(folder);

        String result = ToolCallLog.record("WebSearchTool.search", "query=泰山", () -> "没有搜索到结果");

        String log = Files.readString(folder.resolve("tool.log"));
        assertTrue(result.contains("没有搜索到结果"));
        assertTrue(log.contains("WebSearchTool.search"));
        assertTrue(log.contains("query=泰山"));
        assertTrue(log.contains("没有搜索到结果"));
    }
}
