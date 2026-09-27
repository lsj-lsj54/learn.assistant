package com.learn.assistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PdfDocumentSource {

    private static final String PDF_LOCATION = "classpath:pdf/*.pdf";

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
            return new Resource[0];
        }
    }
}
