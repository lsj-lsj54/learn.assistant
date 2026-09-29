package com.learn.assistant.domain.vo;

import java.util.List;

public record DocumentIngestResponse(int addedCount, int skippedCount, List<FileIngestResponse> files) {
}

