package com.learn.assistant.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class ProjectPaths {

    private static final Path DEFAULT_PDF_DIRECTORY = Path.of("src/main/resources/pdf");

    private static final Path DEFAULT_DOWNLOAD_DIRECTORY = Path.of("res");

    private final Path pdfRelative;

    private final Path downloadRelative;

    public ProjectPaths() {
        this(DEFAULT_PDF_DIRECTORY, DEFAULT_DOWNLOAD_DIRECTORY);
    }

    @Autowired
    public ProjectPaths(
            @Value("${learn.paths.pdf-dir:src/main/resources/pdf}") String pdfDir,
            @Value("${learn.paths.download-dir:res}") String downloadDir) {
        this(Path.of(pdfDir), Path.of(downloadDir));
    }

    private ProjectPaths(Path pdfRelative, Path downloadRelative) {
        this.pdfRelative = pdfRelative;
        this.downloadRelative = downloadRelative;
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
