package com.learn.assistant.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learn.rag")
public class RagProperties {

    private final Chunk chunk = new Chunk();

    private final Batch batch = new Batch();

    private final Retrieval retrieval = new Retrieval();

    public Chunk getChunk() {
        return chunk;
    }

    public Batch getBatch() {
        return batch;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public static class Chunk {

        private int chunkSize = 500;

        private int minChunkSizeChars = 100;

        private int minChunkLengthToEmbed = 5;

        private int maxNumChunks = 10000;

        private boolean keepSeparator = true;

        private String punctuation = "。！？；\n.!?;";

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getMinChunkSizeChars() {
            return minChunkSizeChars;
        }

        public void setMinChunkSizeChars(int minChunkSizeChars) {
            this.minChunkSizeChars = minChunkSizeChars;
        }

        public int getMinChunkLengthToEmbed() {
            return minChunkLengthToEmbed;
        }

        public void setMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
            this.minChunkLengthToEmbed = minChunkLengthToEmbed;
        }

        public int getMaxNumChunks() {
            return maxNumChunks;
        }

        public void setMaxNumChunks(int maxNumChunks) {
            this.maxNumChunks = maxNumChunks;
        }

        public boolean isKeepSeparator() {
            return keepSeparator;
        }

        public void setKeepSeparator(boolean keepSeparator) {
            this.keepSeparator = keepSeparator;
        }

        public String getPunctuation() {
            return punctuation;
        }

        public void setPunctuation(String punctuation) {
            this.punctuation = punctuation;
        }
    }

    public static class Batch {

        private int maxTokenCount = 8192;

        private double reservePercentage = 0.1;

        private String encoding = "CL100K_BASE";

        public int getMaxTokenCount() {
            return maxTokenCount;
        }

        public void setMaxTokenCount(int maxTokenCount) {
            this.maxTokenCount = maxTokenCount;
        }

        public double getReservePercentage() {
            return reservePercentage;
        }

        public void setReservePercentage(double reservePercentage) {
            this.reservePercentage = reservePercentage;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }
    }

    public static class Retrieval {

        private double similarityThreshold = 0.5;

        private int topK = 4;

        private int rewriteMinLength = 24;

        private String targetLanguage = "chinese";

        private boolean allowEmptyContext = false;

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public int getRewriteMinLength() {
            return rewriteMinLength;
        }

        public void setRewriteMinLength(int rewriteMinLength) {
            this.rewriteMinLength = rewriteMinLength;
        }

        public String getTargetLanguage() {
            return targetLanguage;
        }

        public void setTargetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
        }

        public boolean isAllowEmptyContext() {
            return allowEmptyContext;
        }

        public void setAllowEmptyContext(boolean allowEmptyContext) {
            this.allowEmptyContext = allowEmptyContext;
        }
    }
}
