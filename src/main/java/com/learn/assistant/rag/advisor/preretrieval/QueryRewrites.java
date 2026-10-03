package com.learn.assistant.rag.advisor.preretrieval;

import com.learn.assistant.properties.RagProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

/** 压缩、翻译、重写依次装饰，先执行的在外层。 */
public final class QueryRewrites {

  private QueryRewrites() {}

  public static QueryTransformer chain(ChatClient.Builder builder, RagProperties properties) {
    QueryTransformer rewrite =
        new RewriteDecorator(new IdentityQueryTransformer(), builder, properties);
    QueryTransformer translation = new TranslationDecorator(rewrite, builder, properties);
    return new CompressionDecorator(translation, builder, properties);
  }
}
