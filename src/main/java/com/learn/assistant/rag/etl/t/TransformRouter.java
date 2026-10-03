package com.learn.assistant.rag.etl.t;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class TransformRouter implements Transform {
  protected final Transform transform;

  @Override
  public List<Document> apply(List<Document> documents) {
    log.info("转换方式为：{}", getTransformType(transform));
    return transform.apply(documents);
  }

  protected String getTransformType(Transform transform) {
    if (transform instanceof TokenChunkTransform) {
      return "Token";
    }
    return "未知转换";
  }
}
