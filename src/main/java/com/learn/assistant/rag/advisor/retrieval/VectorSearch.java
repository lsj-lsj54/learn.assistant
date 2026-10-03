package com.learn.assistant.rag.advisor.retrieval;

import com.learn.assistant.properties.RagProperties;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;

/** 按相似度阈值和 topK 从向量库检索文档。 */
public final class VectorSearch {

    private VectorSearch() {
    }

    public static DocumentRetriever retriever(VectorStore vectorStore, RagProperties properties) {
        RagProperties.Retrieval retrieval = properties.getRetrieval();
        if (retrieval.getTopK() < 1) {
            throw new IllegalArgumentException("learn.rag.retrieval.top-k 必须大于 0");
        }
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(retrieval.getSimilarityThreshold())
                .topK(retrieval.getTopK())
                .build();
    }
}
