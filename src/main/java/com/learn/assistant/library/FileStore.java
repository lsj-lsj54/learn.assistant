package com.learn.assistant.library;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStore {

  boolean supports(String path);

  String normalize(String path);

  void check(byte[] content);

  void place(Path source, String relative) throws IOException;

  boolean delete(String relative) throws IOException;
}
