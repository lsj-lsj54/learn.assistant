package com.learn.assistant.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RagAdvisorConfig {

    @Bean
    public QueryTransformerContributor compressionQueryTransformerContributor() {
        return BuiltinQueryTransformers.compression();
    }

    @Bean
    public QueryTransformerContributor translationQueryTransformerContributor() {
        return BuiltinQueryTransformers.translation();
    }

    @Bean
    public QueryTransformerContributor rewriteQueryTransformerContributor() {
        return BuiltinQueryTransformers.rewrite();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(ChatModel chatModel, VectorStore vectorStore,
            RagProperties properties, List<QueryTransformerContributor> contributors) {
        RagProperties.Retrieval retrieval = properties.getRetrieval();
        if (retrieval.getTopK() < 1) {
            throw new IllegalArgumentException("learn.rag.retrieval.top-k 必须大于 0");
        }
        ChatClient.Builder transformerBuilder = ChatClient.builder(chatModel)
                .defaultOptions(ChatOptions.builder().temperature(0.0));
        List<QueryTransformerContributor> ordered = new ArrayList<>(contributors);
        AnnotationAwareOrderComparator.sort(ordered);
        QueryTransformer[] transformers = ordered.stream()
                .map(contributor -> contributor.contribute(transformerBuilder, properties))
                .toArray(QueryTransformer[]::new);
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(transformers)
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(retrieval.getSimilarityThreshold())
                        .topK(retrieval.getTopK())
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(retrieval.isAllowEmptyContext())
                        .promptTemplate(new PromptTemplate("""
                                上下文如下。

                                ---------------------
                                {context}
                                ---------------------

                                只根据上下文回答，不要使用上下文以外的知识。
                                如果上下文里没有答案，就说不知道。

                                问题：{query}

                                回答：
                                """))
                        .emptyContextPromptTemplate(new PromptTemplate("请只回复：上下文为空"))
                        .build())
                .build();
    }
}
