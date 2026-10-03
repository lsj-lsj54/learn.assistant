package com.learn.assistant.rag.etl.l;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class LoadRouter implements Load {
  protected final Load load;

  @Override
  public void accept(List<Document> documents) {
    log.info("写入方式为：{}", getLoadType(load));
    load.accept(documents);
  }

  protected String getLoadType(Load load) {
    if (load instanceof VectorStoreLoad) {
      return "Vector";
    }
    return "未知写入";
  }
}
