package com.learn.assistant.rag;

import com.learn.assistant.service.DocumentIngestor;
import com.learn.assistant.service.IngestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DocumentIngestService implements DocumentIngestor {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestService.class);

    private final List<DocumentSource> documentSources;

    private final TokenTextSplitter tokenTextSplitter;

    private final VectorStore vectorStore;

    private final JdbcTemplate jdbcTemplate;

    private final PgVectorStoreProperties vectorStoreProperties;

    private final StoredChunkLookup storedChunkLookup;

    private final Object storeLock = new Object();

    public DocumentIngestService(List<DocumentSource> documentSources, TokenTextSplitter tokenTextSplitter,
            VectorStore vectorStore, JdbcTemplate jdbcTemplate, PgVectorStoreProperties vectorStoreProperties,
            StoredChunkLookup storedChunkLookup) {
        this.documentSources = List.copyOf(documentSources);
        this.tokenTextSplitter = tokenTextSplitter;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.vectorStoreProperties = vectorStoreProperties;
        this.storedChunkLookup = storedChunkLookup;
    }

    @Override
    public int clear() {
        synchronized (storeLock) {
            String sql = "DELETE FROM " + VectorTables.qualified(vectorStoreProperties);
            int deleted = jdbcTemplate.update(sql);
            log.info("已清空向量 {} 条", deleted);
            return deleted;
        }
    }

    @Override
    public IngestResult ingest() {
        synchronized (storeLock) {
            int removed = collapseDuplicates();
            if (removed > 0) {
                log.info("已删掉库里重复的 {} 条", removed);
            }
            List<Document> documents = readAll();
            if (documents.isEmpty()) {
                log.info("没有可导入的文档");
                return new IngestResult(0, 0);
            }
            int replaced = deleteBySourceFile(documents);
            if (replaced > 0) {
                log.info("已替换同名资料 {} 条", replaced);
            }
            List<Document> chunks = tokenTextSplitter.apply(documents);
            IngestResult result = storeNewChunks(chunks);
            log.info("已导入 {} 段，跳过 {} 段重复", result.added(), result.skipped());
            return result;
        }
    }

    private IngestResult storeNewChunks(List<Document> chunks) {
        List<String> texts = new ArrayList<>();
        for (Document chunk : chunks) {
            if (chunk.getText() != null && !chunk.getText().isBlank()) {
                texts.add(chunk.getText());
            }
        }
        Set<String> seen = new HashSet<>();
        for (String text : storedChunkLookup.findPresent(texts)) {
            if (text != null) {
                seen.add(ChunkFingerprint.hash(text));
            }
        }
        List<Document> fresh = new ArrayList<>();
        int skipped = 0;
        for (Document chunk : chunks) {
            String text = chunk.getText();
            if (text == null || text.isBlank()) {
                skipped++;
                continue;
            }
            if (!seen.add(ChunkFingerprint.hash(text))) {
                skipped++;
                continue;
            }
            fresh.add(ChunkFingerprint.stamp(chunk));
        }
        if (!fresh.isEmpty()) {
            vectorStore.add(fresh);
        }
        return new IngestResult(fresh.size(), skipped);
    }

    private int deleteBySourceFile(List<Document> documents) {
        Set<String> files = new LinkedHashSet<>();
        for (Document document : documents) {
            Object value = document.getMetadata().get(PdfDocumentSource.SOURCE_FILE);
            if (value instanceof String file && !file.isBlank()) {
                files.add(file);
            }
        }
        if (files.isEmpty()) {
            return 0;
        }
        String sql = "DELETE FROM " + VectorTables.qualified(vectorStoreProperties)
                + " WHERE metadata->>'source_file' = ?";
        int removed = 0;
        for (String file : files) {
            removed += jdbcTemplate.update(sql, file);
        }
        return removed;
    }

    private int collapseDuplicates() {
        String table = VectorTables.qualified(vectorStoreProperties);
        String sql = "DELETE FROM " + table + " AS extra USING " + table + " AS kept "
                + "WHERE extra.content = kept.content AND extra.ctid > kept.ctid";
        return jdbcTemplate.update(sql);
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
