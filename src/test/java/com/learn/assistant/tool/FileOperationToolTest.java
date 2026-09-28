package com.learn.assistant.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOperationToolTest {

    @TempDir
    Path tempDir;

    @Test
    void listFiles() {
        FileOperationTool tool = tool();
        tool.writeText("notes/a.txt", "你好");

        String listing = tool.listFiles("notes");

        assertTrue(listing.contains("a.txt"));
    }

    @Test
    void listFilesOnEmptyDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("empty"));

        assertEquals("目录为空", tool().listFiles("empty"));
    }

    @Test
    void listFilesRejectsFilePath() {
        FileOperationTool tool = tool();
        tool.writeText("a.txt", "你好");

        assertTrue(tool.listFiles("a.txt").startsWith("不是目录"));
    }

    @Test
    void readText() {
        FileOperationTool tool = tool();
        tool.writeText("a.txt", "你好");

        assertEquals("你好", tool.readText("a.txt"));
    }

    @Test
    void readTextWhenMissing() {
        assertTrue(tool().readText("missing.txt").startsWith("文件不存在"));
    }

    @Test
    void writeText() {
        String result = tool().writeText("notes/a.txt", "你好");

        assertTrue(result.startsWith("已写入"));
        assertEquals("你好", tool().readText("notes/a.txt"));
    }

    @Test
    void writeTextRejectsPathOutsideProject() {
        assertTrue(tool().writeText("../outside.txt", "x").contains("路径超出允许目录"));
    }

    @Test
    void writeTextRejectsEnvFile() {
        assertTrue(tool().writeText(".env", "secret").contains("不允许操作 .env"));
    }

    @Test
    void deleteFile() {
        FileOperationTool tool = tool();
        tool.writeText("a.txt", "你好");

        assertTrue(tool.deleteFile("a.txt").startsWith("已删除"));
        assertTrue(tool.readText("a.txt").startsWith("文件不存在"));
    }

    @Test
    void deleteFileRejectsDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("notes"));

        assertEquals("不能删除目录", tool().deleteFile("notes"));
    }

    @Test
    void deleteFileWhenMissing() {
        assertTrue(tool().deleteFile("missing.txt").startsWith("文件不存在"));
    }

    private FileOperationTool tool() {
        return new FileOperationTool(new TempProjectPaths(tempDir));
    }

    private static final class TempProjectPaths extends ProjectPaths {

        private final Path root;

        private TempProjectPaths(Path root) {
            this.root = root;
        }

        @Override
        public Path root() {
            return root;
        }
    }
}
