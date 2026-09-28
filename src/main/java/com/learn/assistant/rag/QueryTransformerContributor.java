package com.learn.assistant.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.core.Ordered;

/**
 * 检索前的查询变换。实现本接口并声明为 Bean 即可插入流水线，{@link #getOrder()} 越小越先执行。
 */
public interface QueryTransformerContributor extends Ordered {

    String id();

    QueryTransformer contribute(ChatClient.Builder chatClientBuilder, RagProperties properties);
}
