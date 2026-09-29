package com.learn.assistant.rag;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 一次读取的结果。读失败的文件留在 failures 里，其他文件继续。
 */
public record SourceRead(List<Document> documents, List<Failure> failures) {

    public SourceRead {
        documents = documents == null ? List.of() : List.copyOf(documents);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }

    public static SourceRead documents(List<Document> documents) {
        return new SourceRead(documents, List.of());
    }

    public record Failure(String file, String message) {
    }
}
