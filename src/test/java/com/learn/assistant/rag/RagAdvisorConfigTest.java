package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RagAdvisorConfigTest {

    @Test
    void buildsAdvisorWithEmptyContextRejected() {
        RagAdvisorConfig config = new RagAdvisorConfig();
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
