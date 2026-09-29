package com.learn.assistant.rag;

import com.learn.assistant.tool.ProjectPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Order(0)
public class PdfDocumentSource implements DocumentSource {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentSource.class);

    private static final String PDF_LOCATION = "classpath:pdf/**/*.pdf";

    public static final String SOURCE_FILE = "source_file";

    private final ProjectPaths projectPaths;

    public PdfDocumentSource() {
        this(new ProjectPaths("src/main/resources/pdf", "res"));
    }

    @Autowired
    public PdfDocumentSource(ProjectPaths projectPaths) {
        this.projectPaths = projectPaths;
    }

    @Override
    public SourceRead load() {
        List<Document> documents = new ArrayList<>();
        List<SourceRead.Failure> failures = new ArrayList<>();
        for (Map.Entry<String, Resource> entry : collect().entrySet()) {
            SourceRead one = readResource(entry.getKey(), entry.getValue());
            documents.addAll(one.documents());
            failures.addAll(one.failures());
        }
        return new SourceRead(documents, failures);
    }

    public List<Document> read() {
        return load().documents();
    }

    public SourceRead readFile(Path path, String sourceName) {
        String safe = normalizePdfName(sourceName);
        if (path == null || !Files.isRegularFile(path)) {
            return new SourceRead(List.of(), List.of(new SourceRead.Failure(safe, "文件不存在")));
        }
        return readResource(safe, new FileSystemResource(path));
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
        }
        catch (IOException ignored) {
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
        Path classesPdf = projectPaths.root().resolve("target").resolve("classes").resolve("pdf")
                .toAbsolutePath().normalize();
        Path classesCopy = classesPdf.resolve(safe).normalize();
        if (classesCopy.startsWith(classesPdf) && Files.deleteIfExists(classesCopy)) {
            removed = true;
        }
        return removed;
    }

    private SourceRead readResource(String name, Resource resource) {
        try {
            List<Document> pages = new PagePdfDocumentReader(resource).read();
            for (Document page : pages) {
                page.getMetadata().put(SOURCE_FILE, name);
            }
            return SourceRead.documents(pages);
        }
        catch (RuntimeException exception) {
            log.warn("读取 PDF 失败: {}", name, exception);
            return new SourceRead(List.of(), List.of(new SourceRead.Failure(name, failureMessage(exception))));
        }
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

    Map<String, Resource> collect() {
        Map<String, Resource> found = new LinkedHashMap<>();
        addClasspath(found);
        addFiles(found);
        return found;
    }

    private void addClasspath(Map<String, Resource> found) {
        for (Resource resource : classpathResources()) {
            try {
                found.put(classpathKey(resource), resource);
            }
            catch (IOException exception) {
                log.warn("读取 PDF 目录失败: {}", exception.getMessage());
            }
        }
    }

    private List<Resource> classpathResources() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(PDF_LOCATION);
            List<Resource> readable = new ArrayList<>();
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    readable.add(resource);
                }
            }
            return readable;
        }
        catch (IOException exception) {
            log.warn("读取 PDF 目录失败: {}", exception.getMessage());
            return List.of();
        }
    }

    private void addFiles(Map<String, Resource> found) {
        Path root = projectPaths.pdfDirectory();
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .forEach(path -> found.put(root.relativize(path).toString().replace('\\', '/'),
                            new FileSystemResource(path)));
        }
        catch (IOException exception) {
            log.warn("读取 PDF 目录失败: {}", exception.getMessage());
        }
    }

    private static String classpathKey(Resource resource) throws IOException {
        String path;
        try {
            URI uri = resource.getURI();
            path = uri.toString();
        }
        catch (IOException exception) {
            path = resource.getDescription();
        }
        path = path.replace('\\', '/');
        int marker = path.lastIndexOf("/pdf/");
        if (marker >= 0) {
            return path.substring(marker + "/pdf/".length());
        }
        return resource.getFilename() == null ? path : resource.getFilename();
    }
}
