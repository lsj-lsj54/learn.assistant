package com.learn.assistant.service;

import java.util.List;

public record IngestResult(int added, int skipped, List<FileIngest> files) {

    public IngestResult {
        files = files == null ? List.of() : List.copyOf(files);
    }
}

