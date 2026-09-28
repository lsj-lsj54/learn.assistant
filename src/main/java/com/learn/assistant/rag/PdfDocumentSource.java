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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Order(0)
public class PdfDocumentSource implements DocumentSource {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentSource.class);

    private static final String PDF_LOCATION = "classpath:pdf/**/*.pdf";

    private final ProjectPaths projectPaths;

    public PdfDocumentSource() {
        this(new ProjectPaths("src/main/resources/pdf", "res"));
    }

    @Autowired
    public PdfDocumentSource(ProjectPaths projectPaths) {
        this.projectPaths = projectPaths;
    }

    @Override
    public List<Document> read() {
        List<Document> documents = new ArrayList<>();
        for (Resource resource : collect().values()) {
            try {
                documents.addAll(new PagePdfDocumentReader(resource).read());
            }
            catch (RuntimeException exception) {
                log.warn("读取 PDF 失败: {}", resource, exception);
            }
        }
        return documents;
    }

    Map<String, Resource> collect() {
        Map<String, Resource> found = new LinkedHashMap<>();
        addClasspath(found);
        addFiles(found);
        return found;
    }

    private void addClasspath(Map<String, Resource> found) {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(PDF_LOCATION);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                found.put(classpathKey(resource), resource);
            }
        }
        catch (IOException exception) {
            log.warn("读取 PDF 目录失败: {}", exception.getMessage());
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
