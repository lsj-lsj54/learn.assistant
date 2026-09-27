package com.learn.assistant.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisorConfig {

    private static final int REWRITE_MIN_LENGTH = 24;

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(ChatModel chatModel, VectorStore vectorStore) {
        ChatClient.Builder transformerBuilder = ChatClient.builder(chatModel)
                .defaultOptions(ChatOptions.builder().temperature(0.0));

        QueryTransformer compression = SelectiveQueryTransformer.when(
                CompressionQueryTransformer.builder().chatClientBuilder(transformerBuilder).build(),
                query -> !query.history().isEmpty());

        QueryTransformer translation = SelectiveQueryTransformer.when(
                TranslationQueryTransformer.builder()
                        .chatClientBuilder(transformerBuilder)
                        .targetLanguage("chinese")
                        .build(),
                query -> containsLatinLetter(query.text()));

        QueryTransformer rewrite = SelectiveQueryTransformer.when(
                RewriteQueryTransformer.builder()
                        .chatClientBuilder(transformerBuilder)
                        .targetSearchSystem("vector store")
                        .build(),
                query -> query.text().length() > REWRITE_MIN_LENGTH);

        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(compression, translation, rewrite)
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.5)
                        .topK(4)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false)
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

    private static boolean containsLatinLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                return true;
            }
        }
        return false;
    }
}
