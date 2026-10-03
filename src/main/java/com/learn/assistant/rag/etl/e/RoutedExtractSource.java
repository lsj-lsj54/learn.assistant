package com.learn.assistant.rag.etl.e;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class RoutedExtractSource implements DocumentSource {

  private final ExtractRouter router;

  public RoutedExtractSource(PdfExtract extract) {
    this.router = new ExtractRouter(extract);
  }

  @Override
  public SourceRead load() {
    return router.load();
  }
}
