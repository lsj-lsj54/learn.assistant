package com.learn.assistant.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceDownloadToolTest {

    @TempDir
    Path tempDir;

    @Test
    void rejectsPrivateAddress() {
        ResourceDownloadTool tool = new ResourceDownloadTool(new TempProjectPaths(tempDir), new PublicHttp());

        assertTrue(tool.downloadImage("http://127.0.0.1/a.png", "a.png").contains("不允许访问内网地址"));
    }

    @Test
    void rejectsNonImagePage() {
        ResourceDownloadTool tool = new ResourceDownloadTool(new TempProjectPaths(tempDir), new PublicHttp());

        assertTrue(tool.downloadImage("https://example.com", "page.html").contains("该地址不是图片"));
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
