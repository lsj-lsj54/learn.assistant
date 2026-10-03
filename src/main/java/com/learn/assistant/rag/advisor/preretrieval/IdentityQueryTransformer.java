package com.learn.assistant.rag.advisor.preretrieval;

import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

/** 装饰链的终点，原样返回查询。 */
public final class IdentityQueryTransformer implements QueryTransformer {

  @Override
  public Query transform(Query query) {
    return query;
  }
}
