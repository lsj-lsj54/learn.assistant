package com.learn.assistant.rag;

import com.learn.assistant.service.DocumentIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIngestService implements DocumentIngestor {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);

    private final List<DocumentSource> documentSources;

    private final TokenTextSplitter tokenTextSplitter;

    private final VectorStore vectorStore;

    private final Object ingestLock = new Object();

    public DocumentIngestService(List<DocumentSource> documentSources, TokenTextSplitter tokenTextSplitter,
            VectorStore vectorStore) {
        this.documentSources = List.copyOf(documentSources);
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
    }

    @Override
    public int ingest() {
        synchronized (ingestLock) {
            List<Document> documents = readAll();
            if (documents.isEmpty()) {
                log.info("没有可导入的文档");
                return 0;
            }
            List<Document> chunks = tokenTextSplitter.apply(documents);
            vectorStore.add(chunks);
            log.info("已导入 {} 段", chunks.size());
            return chunks.size();
        }
    }

    private List<Document> readAll() {
        List<Document> documents = new ArrayList<>();
        for (DocumentSource source : documentSources) {
            List<Document> batch = read(source);
            if (batch == null || batch.isEmpty()) {
                continue;
            }
            documents.addAll(batch);
        }
        return documents;
    }

    private static List<Document> read(DocumentSource source) {
        try {
            return source.read();
        }
        catch (RuntimeException exception) {
            log.warn("读取文档失败: {}", source.getClass().getSimpleName(), exception);
            throw exception;
        }
    }
}
