package com.learn.assistant.rag;

import com.learn.assistant.config.RagConfig;
import com.learn.assistant.properties.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentIngestServiceTest {

    @Test
    void returnsZeroWhenNoPdf() {
        PdfDocumentSource source = mock(PdfDocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.read()).thenReturn(List.of());
        DocumentIngestService service = service(List.of(source), vectorStore, mock(JdbcTemplate.class));

        assertEquals(0, service.ingest());
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void splitsAndStoresDocuments() {
        PdfDocumentSource source = mock(PdfDocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.read()).thenReturn(List.of(new Document("第一句。第二句。第三句。第四句。第五句。")));
        TokenTextSplitter splitter = splitter();
        DocumentIngestService service = new DocumentIngestService(List.of(source), splitter, vectorStore,
                mock(JdbcTemplate.class), new PgVectorStoreProperties());

        int count = service.ingest();

        assertEquals(splitter.apply(List.of(new Document("第一句。第二句。第三句。第四句。第五句。"))).size(), count);
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == count));
    }

    @Test
    void mergesEveryDocumentSource() {
        DocumentSource first = mock(DocumentSource.class);
        DocumentSource second = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(first.read()).thenReturn(List.of(new Document("第一句。第二句。")));
        when(second.read()).thenReturn(List.of(new Document("第三句。第四句。第五句。")));
        TokenTextSplitter splitter = splitter();
        DocumentIngestService service = service(List.of(first, second), vectorStore, mock(JdbcTemplate.class));

        int count = service.ingest();

        List<Document> expected = splitter.apply(List.of(
                new Document("第一句。第二句。"),
                new Document("第三句。第四句。第五句。")));
        assertEquals(expected.size(), count);
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == count));
    }

    @Test
    void clearsVectorTableWithoutTouchingOtherSql() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update("DELETE FROM public.vector_store")).thenReturn(4);
        DocumentIngestService service = service(List.of(), mock(VectorStore.class), jdbcTemplate);

        assertEquals(4, service.clear());
        verify(jdbcTemplate).update("DELETE FROM public.vector_store");
    }

    @Test
    void rejectsUnsafeVectorTableName() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PgVectorStoreProperties properties = new PgVectorStoreProperties();
        properties.setTableName("vector_store;drop");
        DocumentIngestService service = new DocumentIngestService(List.of(), splitter(), mock(VectorStore.class),
                jdbcTemplate, properties);

        assertThrows(IllegalArgumentException.class, service::clear);
        verify(jdbcTemplate, never()).update(org.mockito.ArgumentMatchers.anyString());
    }

    private static DocumentIngestService service(List<DocumentSource> sources, VectorStore vectorStore,
            JdbcTemplate jdbcTemplate) {
        return new DocumentIngestService(sources, splitter(), vectorStore, jdbcTemplate, new PgVectorStoreProperties());
    }

    private static TokenTextSplitter splitter() {
        RagProperties properties = new RagProperties();
        properties.getChunk().setChunkSize(8);
        properties.getChunk().setMinChunkSizeChars(1);
        properties.getChunk().setMinChunkLengthToEmbed(1);
        return new RagConfig().tokenTextSplitter(properties);
    }
}
