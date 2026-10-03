package com.learn.assistant.library;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
public class FileStoreRouter {

  private final List<FileStore> stores;

  public FileStoreRouter(List<FileStore> stores) {
    this.stores = List.copyOf(stores);
  }

  public FileStore select(String path) {
    for (FileStore store : stores) {
      if (store.supports(path)) {
        return store;
      }
    }
    if (stores.size() == 1) {
      stores.get(0).normalize(path);
    }
    throw new IllegalArgumentException("不支持的文件类型");
  }

  public String normalize(String path) {
    return select(path).normalize(path);
  }

  public void check(String path, byte[] content) {
    select(path).check(content);
  }

  public void place(Path source, String relative) throws IOException {
    select(relative).place(source, relative);
  }

  public boolean delete(String relative) throws IOException {
    return select(relative).delete(relative);
  }
}
