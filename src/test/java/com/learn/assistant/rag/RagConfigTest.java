package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RagConfigTest {

    @Test
    void splitterBreaksTextIntoChunks() {
        RagProperties properties = new RagProperties();
        properties.getChunk().setChunkSize(8);
        properties.getChunk().setMinChunkSizeChars(1);
        properties.getChunk().setMinChunkLengthToEmbed(1);
        TokenTextSplitter splitter = new RagConfig().tokenTextSplitter(properties);

        List<Document> chunks = splitter.apply(List.of(new Document("第一句。第二句。第三句。第四句。第五句。")));

        assertTrue(chunks.size() > 1);
    }

    @Test
    void batchingStrategyGroupsDocuments() {
        RagProperties properties = new RagProperties();
        properties.getBatch().setMaxTokenCount(2);
        properties.getBatch().setReservePercentage(0);
        BatchingStrategy strategy = new RagConfig().batchingStrategy(properties);

        List<List<Document>> batches = strategy.batch(List.of(
                new Document("a"),
                new Document("b"),
                new Document("c")));

        assertTrue(batches.size() > 1);
    }
}
