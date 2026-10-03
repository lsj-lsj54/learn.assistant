package com.learn.assistant.rag.etl.l;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

/** 把文本段写入向量库。 */
@RequiredArgsConstructor
public class VectorStoreLoad implements Load {

  private final VectorStore vectorStore;

  @Override
  public void accept(List<Document> documents) {
    vectorStore.add(documents);
  }
}
