package com.learn.assistant.rag.etl.e;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

@Slf4j
@RequiredArgsConstructor
public class ExtractRouter implements Extract {
  protected final Extract extract;

  @Override
  public SourceRead load() {
    log.info("读取的文件格式为：{}", extract.fileType());
    return extract.load();
  }

  @Override
  public boolean supports(String name) {
    return extract.supports(name);
  }

  @Override
  public String fileType() {
    return extract.fileType();
  }

  @Override
  public SourceRead read(String name, Resource resource) {
    log.info("读取的文件格式为：{}", extract.fileType());
    return extract.read(name, resource);
  }
}
