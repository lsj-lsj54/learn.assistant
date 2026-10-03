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
    log.info("读取的文件格式为：{}", getFileType(extract));
    return extract.load();
  }

  @Override
  public boolean supports(String name) {
    return extract.supports(name);
  }

  @Override
  public SourceRead read(String name, Resource resource) {
    log.info("读取的文件格式为：{}", getFileType(extract));
    return extract.read(name, resource);
  }

  protected String getFileType(Extract extract) {
    if (extract instanceof HtmlExtract) {
      return "HTML";
    } else if (extract instanceof JsonExtract) {
      return "Json";
    } else if (extract instanceof MarkdownExtract) {
      return "Markdown";
    } else if (extract instanceof PdfExtract) {
      return "Pdf";
    } else if (extract instanceof TextExtract) {
      return "Text";
    }

    return "未知格式";
  }
}
