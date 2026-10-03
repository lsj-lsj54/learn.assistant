package com.learn.assistant.rag.etl.t;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

/** 按 TokenTextSplitter 的规则把文档切成文本段。 */
@RequiredArgsConstructor
public class TokenChunkTransform implements Transform {

  private final TokenTextSplitter splitter;

  @Override
  public List<Document> apply(List<Document> documents) {
    return splitter.apply(documents);
  }
}
