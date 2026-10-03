package com.learn.assistant.rag.etl.t;

import org.springframework.ai.document.Document;

import java.util.List;

public interface Transform {
  List<Document> apply(List<Document> documents);
}
