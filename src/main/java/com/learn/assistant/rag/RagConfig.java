package com.learn.assistant.rag;

import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    private static List<Character> punctuationMarks(String punctuation) {
        List<Character> marks = new ArrayList<>();
        punctuation.codePoints().forEach(codePoint -> marks.add((char) codePoint));
        return marks;
    }
}
