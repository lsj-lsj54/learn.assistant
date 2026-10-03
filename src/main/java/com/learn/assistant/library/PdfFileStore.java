package com.learn.assistant.library;

import com.learn.assistant.tool.path.ProjectPaths;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Component
public class PdfFileStore implements FileStore {

  static final int MAX_PDF_BYTES = 20 * 1024 * 1024;

  private final ProjectPaths projectPaths;

  public PdfFileStore(ProjectPaths projectPaths) {
    this.projectPaths = projectPaths;
  }

  @Override
  public boolean supports(String path) {
    return path != null && path.toLowerCase(Locale.ROOT).endsWith(".pdf");
  }

  @Override
  public String normalize(String path) {
    String safe = projectPaths.safeRelative(path);
    if (!safe.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      throw new IllegalArgumentException("只接受 PDF 文件");
    }
    return safe;
  }

  @Override
  public void check(byte[] content) {
    if (content.length > MAX_PDF_BYTES) {
      throw new IllegalArgumentException("PDF 超过 20MB");
    }
    if (!isPdf(content)) {
      throw new IllegalArgumentException("文件不是 PDF");
    }
  }

  @Override
  public void place(Path source, String relative) throws IOException {
    String safe = normalize(relative);
    Path target = projectPaths.resolveWithin(projectPaths.pdfDirectory(), safe);
    Path parent = target.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    try {
      Files.deleteIfExists(source);
    } catch (IOException ignored) {
      // 副本已经写入资料目录，临时文件留给系统清理
    }
  }

  @Override
  public boolean delete(String relative) throws IOException {
    String safe = normalize(relative);
    boolean removed = false;
    Path library = projectPaths.pdfDirectory().toAbsolutePath().normalize();
    Path target = projectPaths.resolveWithin(library, safe);
    if (Files.deleteIfExists(target)) {
      removed = true;
    }
    Path classesPdf =
        projectPaths
            .root()
            .resolve("target")
            .resolve("classes")
            .resolve("pdf")
            .toAbsolutePath()
            .normalize();
    Path classesCopy = classesPdf.resolve(safe).normalize();
    if (classesCopy.startsWith(classesPdf) && Files.deleteIfExists(classesCopy)) {
      removed = true;
    }
    return removed;
  }

  private static boolean isPdf(byte[] content) {
    return content.length >= 4
        && content[0] == '%'
        && content[1] == 'P'
        && content[2] == 'D'
        && content[3] == 'F';
  }
}
