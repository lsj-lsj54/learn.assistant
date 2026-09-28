package com.learn.assistant.tool;

import java.nio.file.Path;

public class ProjectPaths {

    private final Path pdfRelative;

    private final Path downloadRelative;

    public ProjectPaths(String pdfDir, String downloadDir) {
        this.pdfRelative = Path.of(pdfDir);
        this.downloadRelative = Path.of(downloadDir);
    }

    public Path root() {
        return Path.of("").toAbsolutePath().normalize();
    }

    public Path pdfDirectory() {
        return root().resolve(pdfRelative);
    }

    public Path downloadDirectory() {
        return root().resolve(downloadRelative);
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
