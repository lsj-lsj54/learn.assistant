package com.learn.assistant.rag;

import com.learn.assistant.rag.etl.e.ExtractRouter;
import com.learn.assistant.rag.etl.e.PdfExtract;
import com.learn.assistant.tool.ProjectPaths;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Order(0)
public class PdfDocumentSource implements DocumentSource {

  public static final String SOURCE_FILE = PdfExtract.SOURCE_FILE;

  private final ProjectPaths projectPaths;

  private final PdfExtract extract;

  private final ExtractRouter router;

  public PdfDocumentSource() {
    this(new ProjectPaths("src/main/resources/pdf", "res"));
  }

  @Autowired
  public PdfDocumentSource(ProjectPaths projectPaths) {
    this.projectPaths = projectPaths;
    this.extract = new PdfExtract(projectPaths);
    this.router = new ExtractRouter(extract);
  }

  @Override
  public SourceRead load() {
    return router.load();
  }

  public List<Document> read() {
    return extract.read();
  }

  public SourceRead readFile(Path path, String sourceName) {
    String safe = normalizePdfName(sourceName);
    if (path == null || !Files.isRegularFile(path)) {
      return new SourceRead(List.of(), List.of(new SourceRead.Failure(safe, "文件不存在")));
    }
    return extract.read(safe, new FileSystemResource(path));
  }

  public String normalizePdfName(String raw) {
    String safe = projectPaths.safeRelative(raw);
    if (!safe.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      throw new IllegalArgumentException("只接受 PDF 文件");
    }
    return safe;
  }

  public void place(Path source, String relative) throws IOException {
    String safe = normalizePdfName(relative);
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

  public boolean deleteStored(String relative) throws IOException {
    String safe = normalizePdfName(relative);
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

  Map<String, Resource> collect() {
    return extract.collect();
  }
}
