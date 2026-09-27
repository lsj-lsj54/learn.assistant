package com.learn.assistant.controller;

import com.learn.assistant.domain.vo.DocumentIngestResponse;
import com.learn.assistant.rag.DocumentIngestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestService documentIngestService;

    public DocumentController(DocumentIngestService documentIngestService) {
        this.documentIngestService = documentIngestService;
    }

    @PostMapping
    public DocumentIngestResponse ingest() {
        return new DocumentIngestResponse(documentIngestService.ingest());
    }
}
