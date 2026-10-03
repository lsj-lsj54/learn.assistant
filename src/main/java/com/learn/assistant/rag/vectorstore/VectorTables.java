package com.learn.assistant.rag.vectorstore;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;

final class VectorTables {

  private VectorTables() {}

  static String qualified(PgVectorStoreProperties properties) {
    return identifier(properties.getSchemaName()) + "." + identifier(properties.getTableName());
  }

  static String identifier(String name) {
    if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
      throw new IllegalArgumentException("向量表名不合法");
    }
    return name;
  }
}
