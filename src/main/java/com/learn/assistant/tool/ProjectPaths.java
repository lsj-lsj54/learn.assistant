package com.learn.assistant.tool;

import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ProjectPaths {

    public Path root() {
        return Path.of("").toAbsolutePath().normalize();
    }

    public Path pdfDirectory() {
        return root().resolve("src/main/resources/pdf");
    }

    public Path downloadDirectory() {
        return root().resolve("res");
    }

    public Path resolveWithin(Path base, String relativePath) {
        Path resolved = (relativePath == null || relativePath.isBlank())
                ? base
                : base.resolve(relativePath).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("路径超出允许目录");
        }
        Path fileName = resolved.getFileName();
        if (fileName != null && ".env".equals(fileName.toString())) {
            throw new IllegalArgumentException("不允许操作 .env");
        }
        return resolved;
    }
}
