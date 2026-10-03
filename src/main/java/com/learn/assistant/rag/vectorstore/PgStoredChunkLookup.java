package com.learn.assistant.rag.vectorstore;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PgStoredChunkLookup implements StoredChunkLookup {

  private static final int BATCH = 500;

  private final JdbcTemplate jdbcTemplate;

  private final PgVectorStoreProperties vectorStoreProperties;

  public PgStoredChunkLookup(JdbcTemplate jdbcTemplate, PgVectorStoreProperties vectorStoreProperties) {
    this.jdbcTemplate = jdbcTemplate;
    this.vectorStoreProperties = vectorStoreProperties;
  }

  @Override
  public Set<String> findPresent(Collection<String> contents) {
    if (contents == null || contents.isEmpty()) {
      return Set.of();
    }
    List<String> distinct = contents.stream().distinct().toList();
    Set<String> present = new HashSet<>();
    for (int start = 0; start < distinct.size(); start += BATCH) {
      List<String> batch = distinct.subList(start, Math.min(start + BATCH, distinct.size()));
      present.addAll(query(batch));
    }
    return present;
  }

  private List<String> query(List<String> batch) {
    String placeholders = String.join(", ", Collections.nCopies(batch.size(), "?"));
    String sql =
        "SELECT content FROM "
            + VectorTables.qualified(vectorStoreProperties)
            + " WHERE content IN ("
            + placeholders
            + ")";
    return jdbcTemplate.query(sql, (result, row) -> result.getString(1), batch.toArray());
  }
}
