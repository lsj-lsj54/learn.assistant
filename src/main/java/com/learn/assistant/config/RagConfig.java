package com.learn.assistant.config;

import com.knuddels.jtokkit.api.EncodingType;
import com.learn.assistant.properties.RagProperties;
import com.learn.assistant.rag.BuiltinQueryTransformers;
import com.learn.assistant.rag.QueryTransformerContributor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    @Bean
    public TokenTextSplitter tokenTextSplitter(RagProperties properties) {
        RagProperties.Chunk chunk = properties.getChunk();
        return TokenTextSplitter.builder()
                .withChunkSize(chunk.getChunkSize())
                .withMinChunkSizeChars(chunk.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(chunk.getMinChunkLengthToEmbed())
                .withMaxNumChunks(chunk.getMaxNumChunks())
                .withKeepSeparator(chunk.isKeepSeparator())
                .withPunctuationMarks(punctuationMarks(chunk.getPunctuation()))
                .build();
    }

    @Bean
    public BatchingStrategy batchingStrategy(RagProperties properties) {
        RagProperties.Batch batch = properties.getBatch();
        return new TokenCountBatchingStrategy(
                EncodingType.valueOf(batch.getEncoding()),
                batch.getMaxTokenCount(),
                batch.getReservePercentage());
    }

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
                        .promptTemplate(new PromptTemplate(RagPrompts.CONTEXT))
                        .emptyContextPromptTemplate(new PromptTemplate(RagPrompts.EMPTY_CONTEXT))
                        .build())
                .build();
    }

    private static List<Character> punctuationMarks(String punctuation) {
        List<Character> marks = new ArrayList<>();
        punctuation.codePoints().forEach(codePoint -> marks.add((char) codePoint));
        return marks;
    }
}
