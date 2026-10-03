package com.learn.assistant.rag.advisor.preretrieval;

import com.learn.assistant.properties.RagProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

/** 有对话历史且问题足够长时，把历史压缩成独立问题。 */
public final class CompressionDecorator extends QueryTransformerDecorator {

  private final QueryTransformer transformer;

  private final RagProperties properties;

  public CompressionDecorator(
      QueryTransformer next, ChatClient.Builder builder, RagProperties properties) {
    super(next);
    this.transformer = CompressionQueryTransformer.builder().chatClientBuilder(builder).build();
    this.properties = properties;
  }

  @Override
  protected Query rewrite(Query query) {
    if (QueryRewriteRules.longEnough(query.text(), properties) && !query.history().isEmpty()) {
      return transformer.transform(query);
    }
    return query;
  }
}
