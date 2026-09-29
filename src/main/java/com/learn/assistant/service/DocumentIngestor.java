package com.learn.assistant.service;

public interface DocumentIngestor {

    IngestResult ingest();

    int clear();
}
