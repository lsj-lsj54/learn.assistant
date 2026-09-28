package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        DocumentIngestService service = new DocumentIngestService(List.of(source), splitter(), vectorStore);

        assertEquals(0, service.ingest());
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void splitsAndStoresDocuments() {
        PdfDocumentSource source = mock(PdfDocumentSource.class);
        VectorStore vectorStore = mock(VectorStore.class);
        when(source.read()).thenReturn(List.of(new Document("第一句。第二句。第三句。第四句。第五句。")));
        TokenTextSplitter splitter = splitter();
        DocumentIngestService service = new DocumentIngestService(List.of(source), splitter, vectorStore);

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
        DocumentIngestService service = new DocumentIngestService(List.of(first, second), splitter, vectorStore);

        int count = service.ingest();

        List<Document> expected = splitter.apply(List.of(
                new Document("第一句。第二句。"),
                new Document("第三句。第四句。第五句。")));
        assertEquals(expected.size(), count);
        verify(vectorStore).add(org.mockito.ArgumentMatchers.argThat(chunks -> chunks.size() == count));
    }

    private static TokenTextSplitter splitter() {
        RagProperties properties = new RagProperties();
        properties.getChunk().setChunkSize(8);
        properties.getChunk().setMinChunkSizeChars(1);
        properties.getChunk().setMinChunkLengthToEmbed(1);
        return new RagConfig().tokenTextSplitter(properties);
    }
}
