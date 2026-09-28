package com.learn.assistant.controller;

import com.learn.assistant.domain.vo.DocumentIngestResponse;
import com.learn.assistant.service.DocumentIngestor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestor documentIngestor;

    public DocumentController(DocumentIngestor documentIngestor) {
        this.documentIngestor = documentIngestor;
    }

    @PostMapping
    public DocumentIngestResponse ingest() {
        return new DocumentIngestResponse(documentIngestor.ingest());
    }
}
