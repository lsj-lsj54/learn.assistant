package com.learn.assistant.rag.etl.e;

/**
 * 可导入的文档来源。没有内容时 documents 为空。
 * 单个文件读失败时写入 failures，不中断其他文件。
 */
public interface DocumentSource {

    SourceRead load();
}
