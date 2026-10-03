package com.learn.assistant.rag.etl.e;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExtractReader {

  private final List<Extract> extracts;

  public ExtractReader(List<Extract> extracts) {
    this.extracts = List.copyOf(extracts);
  }

  public SourceRead read(String name, Resource resource) {
    for (Extract extract : extracts) {
      if (extract.supports(name)) {
        return new ExtractRouter(extract).read(name, resource);
      }
    }
    throw new IllegalArgumentException("不支持的文件类型");
  }
}
