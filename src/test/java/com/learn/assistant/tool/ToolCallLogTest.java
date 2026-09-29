package com.learn.assistant.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.model.tool.internal.ToolCallReactiveContextHolder;
import reactor.util.context.Context;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void publishesTheFinishedCallForTheCurrentAnswer() {
        ToolCallLog.useDirectory(folder);
        ToolActivity activity = new ToolActivity();
        ToolCallReactiveContextHolder.setContext(Context.of(ToolActivity.CONTEXT_KEY, activity));
        try {
            ToolCallLog.record("WebSearchTool.search", "query=泰山", () -> "没有搜索到结果");
            List<ToolStep> steps = activity.drain();
            assertEquals(1, steps.size());
            assertEquals("搜索", steps.get(0).name());
            assertEquals("query=泰山", steps.get(0).arguments());
            assertEquals("没有搜索到结果", steps.get(0).result());
            assertTrue(steps.get(0).wire().contains("\"name\":\"搜索\""));
            assertTrue(activity.drain().isEmpty());
        }
        finally {
            ToolCallReactiveContextHolder.clearContext();
        }
    }

    @Test
    void publishesAFailureBeforeRethrowing() {
        ToolCallLog.useDirectory(folder);
        ToolActivity activity = new ToolActivity();
        ToolCallReactiveContextHolder.setContext(Context.of(ToolActivity.CONTEXT_KEY, activity));
        try {
            assertThrows(IllegalStateException.class,
                    () -> ToolCallLog.record("FileOperationTool.readText", "relativePath=a.txt", () -> {
                        throw new IllegalStateException("读不了");
                    }));
            List<ToolStep> steps = activity.drain();
            assertEquals("读取文件", steps.get(0).name());
            assertEquals("异常: 读不了", steps.get(0).result());
        }
        finally {
            ToolCallReactiveContextHolder.clearContext();
        }
    }
}
