package com.learn.assistant.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;

import java.util.function.BiFunction;

final class BuiltinQueryTransformers {

    private BuiltinQueryTransformers() {
    }

    static QueryTransformerContributor compression() {
        return contributor("compression", 0, (builder, properties) -> SelectiveQueryTransformer.when(
                CompressionQueryTransformer.builder().chatClientBuilder(builder).build(),
                query -> !query.history().isEmpty()));
    }

    static QueryTransformerContributor translation() {
        return contributor("translation", 1, (builder, properties) -> SelectiveQueryTransformer.when(
                TranslationQueryTransformer.builder()
                        .chatClientBuilder(builder)
                        .targetLanguage(properties.getRetrieval().getTargetLanguage())
                        .build(),
                query -> containsLatinLetter(query.text())));
    }

    static QueryTransformerContributor rewrite() {
        return contributor("rewrite", 2, (builder, properties) -> SelectiveQueryTransformer.when(
                RewriteQueryTransformer.builder()
                        .chatClientBuilder(builder)
                        .targetSearchSystem("vector store")
                        .build(),
                query -> query.text().length() > properties.getRetrieval().getRewriteMinLength()));
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

    private static QueryTransformerContributor contributor(String id, int order,
            BiFunction<ChatClient.Builder, RagProperties, QueryTransformer> factory) {
        return new QueryTransformerContributor() {

            @Override
            public String id() {
                return id;
            }

            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public QueryTransformer contribute(ChatClient.Builder chatClientBuilder, RagProperties properties) {
                return factory.apply(chatClientBuilder, properties);
            }
        };
    }
}
