package com.learn.assistant.rag.advisor;

import com.learn.assistant.properties.RagProperties;
import com.learn.assistant.rag.advisor.postretrieval.ConversationAwareQueryAugmenter;
import com.learn.assistant.rag.advisor.preretrieval.QueryRewrites;
import com.learn.assistant.rag.advisor.retrieval.VectorSearch;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetrievalAdvisorConfig {

  @Bean
  public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
      ChatModel chatModel, VectorStore vectorStore, RagProperties properties) {
    ChatClient.Builder transformerBuilder =
        ChatClient.builder(chatModel).defaultOptions(ChatOptions.builder().temperature(0.0));
    return RetrievalAugmentationAdvisor.builder()
        .queryTransformers(QueryRewrites.chain(transformerBuilder, properties))
        .documentRetriever(VectorSearch.retriever(vectorStore, properties))
        .queryAugmenter(
            new ConversationAwareQueryAugmenter(properties.getRetrieval().isAllowEmptyContext()))
        .build();
  }
}
