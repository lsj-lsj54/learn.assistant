package com.learn.assistant.tool;

import java.util.ArrayList;
import java.util.List;
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

    public String safeRelative(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String unified = raw.trim().replace('\\', '/');
        while (unified.startsWith("/")) {
            unified = unified.substring(1);
        }
        if (unified.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("文件名不合法");
        }
        List<String> kept = new ArrayList<>();
        for (String piece : unified.split("/")) {
            if (piece.isBlank() || ".".equals(piece)) {
                continue;
            }
            if ("..".equals(piece)) {
                throw new IllegalArgumentException("路径超出允许目录");
            }
            String cleaned = piece.replaceAll("[\\\\:*?\"<>|]", "_").trim();
            if (cleaned.isBlank() || ".".equals(cleaned) || "..".equals(cleaned)) {
                throw new IllegalArgumentException("文件名不合法");
            }
            if (".env".equals(cleaned)) {
                throw new IllegalArgumentException("不允许操作 .env");
            }
            kept.add(cleaned);
        }
        if (kept.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        return String.join("/", kept);
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
