package com.learn.assistant.config;

import com.learn.assistant.properties.RagProperties;
import com.learn.assistant.rag.QueryTransformerContributor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RagConfigTest {

    private final RagConfig config = new RagConfig();

    @Test
    void splitterBreaksTextIntoChunks() {
        RagProperties properties = new RagProperties();
        properties.getChunk().setChunkSize(8);
        properties.getChunk().setMinChunkSizeChars(1);
        properties.getChunk().setMinChunkLengthToEmbed(1);
        TokenTextSplitter splitter = config.tokenTextSplitter(properties);

        List<Document> chunks = splitter.apply(List.of(new Document("第一句。第二句。第三句。第四句。第五句。")));

        assertTrue(chunks.size() > 1);
    }

    @Test
    void batchingStrategyGroupsDocuments() {
        RagProperties properties = new RagProperties();
        properties.getBatch().setMaxTokenCount(2);
        properties.getBatch().setReservePercentage(0);
        BatchingStrategy strategy = config.batchingStrategy(properties);

        List<List<Document>> batches = strategy.batch(List.of(
                new Document("a"),
                new Document("b"),
                new Document("c")));

        assertTrue(batches.size() > 1);
    }

    @Test
    void buildsAdvisorWithEmptyContextRejected() {
        List<QueryTransformerContributor> contributors = new ArrayList<>(List.of(
                config.compressionQueryTransformerContributor(),
                config.rewriteQueryTransformerContributor(),
                config.translationQueryTransformerContributor()));
        AnnotationAwareOrderComparator.sort(contributors);

        assertEquals(List.of("compression", "translation", "rewrite"),
                contributors.stream().map(QueryTransformerContributor::id).toList());

        RetrievalAugmentationAdvisor advisor = config.retrievalAugmentationAdvisor(
                mock(ChatModel.class), mock(VectorStore.class), new RagProperties(), contributors);

        assertNotNull(advisor);
    }
}
