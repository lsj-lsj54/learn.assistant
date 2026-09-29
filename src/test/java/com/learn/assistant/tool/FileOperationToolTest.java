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
        tool.writeText("res/notes/a.txt", "你好");

        String listing = tool.listFiles("res/notes");

        assertTrue(listing.contains("a.txt"));
    }

    @Test
    void listFilesOnEmptyDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("res").resolve("empty"));

        assertEquals("目录为空", tool().listFiles("res/empty"));
    }

    @Test
    void listFilesRejectsFilePath() {
        FileOperationTool tool = tool();
        tool.writeText("res/a.txt", "你好");

        assertTrue(tool.listFiles("res/a.txt").startsWith("不是目录"));
    }

    @Test
    void readText() {
        FileOperationTool tool = tool();
        tool.writeText("pdf/a.txt", "你好");

        assertEquals("你好", tool.readText("pdf/a.txt"));
    }

    @Test
    void readTextWhenMissing() {
        assertTrue(tool().readText("res/missing.txt").startsWith("文件不存在"));
    }

    @Test
    void writeText() {
        String result = tool().writeText("res/notes/a.txt", "你好");

        assertTrue(result.startsWith("已写入"));
        assertEquals("你好", tool().readText("res/notes/a.txt"));
        assertTrue(Files.exists(tempDir.resolve("res").resolve("notes").resolve("a.txt")));
    }

    @Test
    void writeTextRejectsPathOutsideDataDirectories() {
        assertTrue(tool().writeText("src/main/java/Note.java", "x").contains("只能操作 pdf 或 res 目录"));
        assertTrue(tool().writeText("res/../outside.txt", "x").contains("路径超出允许目录"));
    }

    @Test
    void writeTextRejectsEnvFile() {
        assertTrue(tool().writeText("res/.env", "secret").contains("不允许操作 .env"));
        assertTrue(tool().writeText(".env", "secret").contains("只能操作 pdf 或 res 目录"));
    }

    @Test
    void deleteFile() {
        FileOperationTool tool = tool();
        tool.writeText("res/a.txt", "你好");

        assertTrue(tool.deleteFile("res/a.txt").startsWith("已删除"));
        assertTrue(tool.readText("res/a.txt").startsWith("文件不存在"));
    }

    @Test
    void deleteFileRejectsDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("res").resolve("notes"));

        assertEquals("不能删除目录", tool().deleteFile("res/notes"));
    }

    @Test
    void deleteFileWhenMissing() {
        assertTrue(tool().deleteFile("pdf/missing.txt").startsWith("文件不存在"));
    }

    private FileOperationTool tool() {
        return new FileOperationTool(new TempProjectPaths(tempDir));
    }

    private static final class TempProjectPaths extends ProjectPaths {

        private final Path root;

        private TempProjectPaths(Path root) {
            super("src/main/resources/pdf", "res");
            this.root = root;
        }

        @Override
        public Path root() {
            return root;
        }
    }
}
