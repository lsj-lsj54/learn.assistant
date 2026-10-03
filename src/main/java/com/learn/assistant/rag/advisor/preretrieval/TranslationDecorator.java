package com.learn.assistant.rag.advisor.preretrieval;

import com.learn.assistant.properties.RagProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;

/** 问题足够长且含拉丁字母时，翻译成目标语言。 */
public final class TranslationDecorator extends QueryTransformerDecorator {

  private final QueryTransformer transformer;

  private final RagProperties properties;

  public TranslationDecorator(
      QueryTransformer next, ChatClient.Builder builder, RagProperties properties) {
    super(next);
    this.transformer =
        TranslationQueryTransformer.builder()
            .chatClientBuilder(builder)
            .targetLanguage(properties.getRetrieval().getTargetLanguage())
            .build();
    this.properties = properties;
  }

  @Override
  protected Query rewrite(Query query) {
    if (QueryRewriteRules.longEnough(query.text(), properties)
        && QueryRewriteRules.containsLatinLetter(query.text())) {
      return transformer.transform(query);
    }
    return query;
  }
}
