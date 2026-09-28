package com.learn.assistant.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Order(0)
public class PdfDocumentSource implements DocumentSource {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentSource.class);

    private static final String PDF_LOCATION = "classpath:pdf/*.pdf";

    @Override
    public List<Document> read() {
        Resource[] resources = resolve();
        List<Document> documents = new ArrayList<>();
        for (Resource resource : resources) {
            documents.addAll(new PagePdfDocumentReader(resource).read());
        }
        return documents;
    }

    private Resource[] resolve() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(PDF_LOCATION);
            return Arrays.stream(resources).filter(Resource::isReadable).toArray(Resource[]::new);
        }
        catch (IOException exception) {
            log.warn("读取 PDF 目录失败: {}", exception.getMessage());
            return new Resource[0];
        }
    }
}
