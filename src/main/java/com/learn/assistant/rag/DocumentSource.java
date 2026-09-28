package com.learn.assistant.rag;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 可导入的文档来源。没有内容时返回空列表。
 */
public interface DocumentSource {

    List<Document> read();
}
