package com.learn.assistant.rag.advisor.preretrieval;

import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

/** 检索前的查询改写。先执行本层，再交给下一层。 */
public abstract class QueryTransformerDecorator implements QueryTransformer {

  private final QueryTransformer next;

  protected QueryTransformerDecorator(QueryTransformer next) {
    this.next = next;
  }

  protected QueryTransformer next() {
    return next;
  }

  @Override
  public final Query transform(Query query) {
    return next.transform(rewrite(query));
  }

  protected abstract Query rewrite(Query query);
}
