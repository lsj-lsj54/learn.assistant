package com.learn.assistant.rag.etl.e;

import com.learn.assistant.tool.ProjectPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** 从 classpath 和资料目录读取文本，转成文档。 */
public class TextExtract implements Extract {

  private static final Logger log = LoggerFactory.getLogger(TextExtract.class);

  private static final String TEXT_LOCATION = "classpath:text/**/*.txt";

  public static final String SOURCE_FILE = PdfExtract.SOURCE_FILE;

  private final ProjectPaths projectPaths;

  public TextExtract(ProjectPaths projectPaths) {
    this.projectPaths = projectPaths;
  }

  @Override
  public boolean supports(String name) {
    return name != null && name.toLowerCase(Locale.ROOT).endsWith(".txt");
  }

  @Override
  public SourceRead load() {
    List<Document> documents = new ArrayList<>();
    List<SourceRead.Failure> failures = new ArrayList<>();
    for (Map.Entry<String, Resource> entry : collect().entrySet()) {
      SourceRead one = read(entry.getKey(), entry.getValue());
      documents.addAll(one.documents());
      failures.addAll(one.failures());
    }
    return new SourceRead(documents, failures);
  }

  public List<Document> read() {
    return load().documents();
  }

  public SourceRead read(String name, Resource resource) {
    try {
      List<Document> pages = new TextReader(resource).read();
      for (Document page : pages) {
        page.getMetadata().put(SOURCE_FILE, name);
      }
      return SourceRead.documents(pages);
    } catch (RuntimeException exception) {
      log.warn("读取文本失败: {}", name, exception);
      return new SourceRead(
          List.of(), List.of(new SourceRead.Failure(name, failureMessage(exception))));
    }
  }

  public Map<String, Resource> collect() {
    Map<String, Resource> found = new LinkedHashMap<>();
    addClasspath(found);
    addFiles(found);
    return found;
  }

  private Path directory() {
    return projectPaths.pdfDirectory().resolveSibling("text");
  }

  private void addClasspath(Map<String, Resource> found) {
    for (Resource resource : classpathResources()) {
      try {
        found.put(classpathKey(resource), resource);
      } catch (IOException exception) {
        log.warn("读取文本目录失败: {}", exception.getMessage());
      }
    }
  }

  private List<Resource> classpathResources() {
    try {
      Resource[] resources = new PathMatchingResourcePatternResolver().getResources(TEXT_LOCATION);
      List<Resource> readable = new ArrayList<>();
      for (Resource resource : resources) {
        if (resource.isReadable()) {
          readable.add(resource);
        }
      }
      return readable;
    } catch (IOException exception) {
      log.warn("读取文本目录失败: {}", exception.getMessage());
      return List.of();
    }
  }

  private void addFiles(Map<String, Resource> found) {
    Path root = directory();
    if (!Files.isDirectory(root)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(
              path ->
                  Files.isRegularFile(path)
                      && path.getFileName().toString().toLowerCase().endsWith(".txt"))
          .forEach(
              path ->
                  found.put(
                      root.relativize(path).toString().replace('\\', '/'),
                      new FileSystemResource(path)));
    } catch (IOException exception) {
      log.warn("读取文本目录失败: {}", exception.getMessage());
    }
  }

  private static String classpathKey(Resource resource) throws IOException {
    String path;
    try {
      URI uri = resource.getURI();
      path = uri.toString();
    } catch (IOException exception) {
      path = resource.getDescription();
    }
    path = path.replace('\\', '/');
    int marker = path.lastIndexOf("/text/");
    if (marker >= 0) {
      return path.substring(marker + "/text/".length());
    }
    return resource.getFilename() == null ? path : resource.getFilename();
  }

  private static String failureMessage(RuntimeException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return "无法读取";
    }
    String oneLine = message.replace('\n', ' ').trim();
    if (oneLine.length() > 120) {
      return oneLine.substring(0, 120);
    }
    return oneLine;
  }
}
