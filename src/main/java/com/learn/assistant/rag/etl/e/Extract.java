package com.learn.assistant.rag.etl.e;

import org.springframework.core.io.Resource;

public interface Extract {
  SourceRead load();

  boolean supports(String name);

  SourceRead read(String name, Resource resource);
}
