package com.learn.assistant.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentIngestService {

    private final PdfDocumentSource pdfDocumentSource;

    private final TokenTextSplitter tokenTextSplitter;

    private final VectorStore vectorStore;

    public DocumentIngestService(PdfDocumentSource pdfDocumentSource, TokenTextSplitter tokenTextSplitter,
            VectorStore vectorStore) {
        this.pdfDocumentSource = pdfDocumentSource;
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
    }

    public int ingest() {
        List<Document> documents = pdfDocumentSource.read();
        if (documents.isEmpty()) {
            return 0;
        }
        List<Document> chunks = tokenTextSplitter.apply(documents);
        vectorStore.add(chunks);
        return chunks.size();
    }
}
