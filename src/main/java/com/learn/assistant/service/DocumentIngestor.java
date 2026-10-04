package com.learn.assistant.service;

import com.learn.assistant.library.LibraryRemoval;
import com.learn.assistant.rag.etl.IngestResult;
import com.learn.assistant.rag.vectorstore.LibraryFile;

import java.util.List;

public interface DocumentIngestor {

    IngestResult ingest();

    IngestResult store(String path, byte[] content);

    List<LibraryFile> list();

    LibraryRemoval delete(String file);

    int clear();
}

