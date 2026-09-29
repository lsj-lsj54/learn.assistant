package com.learn.assistant.rag;

import com.learn.assistant.config.RagConfig;
import com.learn.assistant.properties.RagProperties;
import com.learn.assistant.service.IngestResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

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

        IngestResult result = service.ingest();

        assertEquals(0, result.added());
        assertEquals(0, result.skipped());
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void splitsAndStoresDocuments() {
        PdfDocumentSource source = mock(PdfDocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.read()).thenReturn(List.of(new Document("第一句。第二句。第三句。第四句。第五句。")));
        TokenTextSplitter splitter = splitter();
        DocumentIngestService service = new DocumentIngestService(List.of(source), splitter, vectorStore,
                mock(JdbcTemplate.class), new PgVectorStoreProperties(), contents -> Set.of());

        int count = service.ingest().added();

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

        int count = service.ingest().added();

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
                jdbcTemplate, properties, contents -> Set.of());

        assertThrows(IllegalArgumentException.class, service::clear);
        verify(jdbcTemplate, never()).update(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deletesRowsThatAlreadyShareTheSameText() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        String sql = "DELETE FROM public.vector_store AS extra USING public.vector_store AS kept "
                + "WHERE extra.content = kept.content AND extra.ctid > kept.ctid";
        when(jdbcTemplate.update(sql)).thenReturn(3);
        DocumentSource source = mock(DocumentSource.class);
        when(source.read()).thenReturn(List.of());
        DocumentIngestService service = service(List.of(source), mock(VectorStore.class), jdbcTemplate);

        IngestResult result = service.ingest();

        assertEquals(0, result.added());
        verify(jdbcTemplate).update(sql);
    }

    @Test
    void keepsOneCopyWhenTheSameTextAppearsTwice() {
        DocumentSource source = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.read()).thenReturn(List.of(new Document("同一段资料。"), new Document("同一段资料。")));
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(), vectorStore,
                mock(JdbcTemplate.class), new PgVectorStoreProperties(), contents -> Set.of());

        IngestResult result = service.ingest();

        assertEquals(1, result.added());
        assertEquals(1, result.skipped());
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == 1
                && "同一段资料。".equals(chunks.get(0).getText())
                && ChunkFingerprint.hash("同一段资料。").equals(chunks.get(0).getMetadata().get(ChunkFingerprint.METADATA_KEY))
                && chunks.get(0).getId().equals(ChunkFingerprint.stamp(new Document("同一段资料。")).getId())));
    }

    @Test
    void skipsTextAlreadyStoredFromAnotherDocument() {
        DocumentSource source = mock(DocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.read()).thenReturn(List.of(new Document("已经在库里。"), new Document("这次是新的。")));
        DocumentIngestService service = new DocumentIngestService(List.of(source), wideSplitter(), vectorStore,
                mock(JdbcTemplate.class), new PgVectorStoreProperties(), contents -> Set.of("已经在库里。"));

        IngestResult result = service.ingest();

        assertEquals(1, result.added());
        assertEquals(1, result.skipped());
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == 1
                && "这次是新的。".equals(chunks.get(0).getText())));
    }

    private static DocumentIngestService service(List<DocumentSource> sources, VectorStore vectorStore,
            JdbcTemplate jdbcTemplate) {
        return new DocumentIngestService(sources, splitter(), vectorStore, jdbcTemplate, new PgVectorStoreProperties(),
                contents -> Set.of());
    }

    private static TokenTextSplitter splitter() {
        return splitter(8);
    }

    private static TokenTextSplitter wideSplitter() {
        return splitter(500);
    }

    private static TokenTextSplitter splitter(int chunkSize) {
        RagProperties properties = new RagProperties();
        properties.getChunk().setChunkSize(chunkSize);
        properties.getChunk().setMinChunkSizeChars(1);
        properties.getChunk().setMinChunkLengthToEmbed(1);
        return new RagConfig().tokenTextSplitter(properties);
    }
}
