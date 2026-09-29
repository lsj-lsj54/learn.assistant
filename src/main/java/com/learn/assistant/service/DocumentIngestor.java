package com.learn.assistant.service;

import java.util.List;

public interface DocumentIngestor {

    IngestResult ingest();

    IngestResult store(String path, byte[] content);

    List<LibraryFile> list();

    LibraryRemoval delete(String file);

    int clear();
}

