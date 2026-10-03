package com.learn.assistant.rag.etl.l;

import org.springframework.ai.document.Document;

import java.util.List;

public interface Load {
  void accept(List<Document> documents);
}
