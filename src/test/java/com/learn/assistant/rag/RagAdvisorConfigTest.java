package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class RagAdvisorConfigTest {

    @Test
    void buildsAdvisorWithEmptyContextRejected() {
        RetrievalAugmentationAdvisor advisor = new RagAdvisorConfig()
                .retrievalAugmentationAdvisor(mock(ChatModel.class), mock(VectorStore.class));

        assertNotNull(advisor);
    }
}
