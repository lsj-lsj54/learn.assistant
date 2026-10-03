package com.learn.assistant.rag.advisor.preretrieval;

import com.learn.assistant.properties.RagProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;

/** 问题足够长时，改写成适合向量检索的查询。 */
public final class RewriteDecorator extends QueryTransformerDecorator {

  private final QueryTransformer transformer;

  private final RagProperties properties;

  public RewriteDecorator(
      QueryTransformer next, ChatClient.Builder builder, RagProperties properties) {
    super(next);
    this.transformer =
        RewriteQueryTransformer.builder()
            .chatClientBuilder(builder)
            .targetSearchSystem("vector store")
            .build();
    this.properties = properties;
  }

  @Override
  protected Query rewrite(Query query) {
    if (QueryRewriteRules.longEnough(query.text(), properties)) {
      return transformer.transform(query);
    }
    return query;
  }
}
