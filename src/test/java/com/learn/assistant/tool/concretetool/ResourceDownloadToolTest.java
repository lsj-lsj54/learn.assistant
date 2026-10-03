package com.learn.assistant.tool.concretetool;

import com.learn.assistant.tool.http.PublicHttp;
import com.learn.assistant.tool.path.ProjectPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceDownloadToolTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsPrivateAddress() {
        ResourceDownloadTool tool = new ResourceDownloadTool(new TempProjectPaths(tempDir), new PublicHttp());

        assertTrue(tool.download("http://127.0.0.1/a.png", "a.png").contains("不允许访问内网地址"));
    }

    @Test
    void savesNonPdfIntoRes() {
        ResourceDownloadTool tool = new ResourceDownloadTool(new TempProjectPaths(tempDir), new PublicHttp());

        String result = tool.download("https://example.com", "站点/page.html");

        assertTrue(result.startsWith("已下载到"));
        assertTrue(result.contains("res"));
        assertTrue(result.contains("站点"));
        assertTrue(result.contains("page.html"));
    }

    @Test
    void asksToChooseOrCreateCategory() throws Exception {
        Files.createDirectories(tempDir.resolve("res/游戏"));
        ResourceDownloadTool tool = new ResourceDownloadTool(new TempProjectPaths(tempDir), new PublicHttp());

        String result = tool.download("https://example.com/vyron.png", "威龙.png");

        assertTrue(result.contains("已有分类：游戏"));
        assertTrue(result.contains("风景/泰山.jpg"));
        assertTrue(Files.notExists(tempDir.resolve("res/威龙.png")));
    }

    @Test
    void keepsSubfoldersAndRejectsParentPath() {
        ProjectPaths paths = new ProjectPaths("src/main/resources/pdf", "res");

        assertEquals("游戏/威龙.png", paths.safeRelative("/游戏/威龙.png"));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paths.safeRelative("../outside.png"));
        assertTrue(exception.getMessage().contains("路径超出允许目录"));
    }

    @Test
    void storesPdfSeparatelyFromOtherResources() {
        ResourceDownloadTool tool = new ResourceDownloadTool(new TempProjectPaths(tempDir), new PublicHttp());

        assertTrue(tool.directoryFor("application/pdf", "note.bin").getFileName().toString().equals("pdf"));
        assertTrue(tool.directoryFor("image/png", "note.pdf").getFileName().toString().equals("pdf"));
        assertTrue(tool.directoryFor("image/png", "photo.png").getFileName().toString().equals("res"));
        assertTrue(tool.directoryFor("text/plain", "notes.txt").getFileName().toString().equals("res"));
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
