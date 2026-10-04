package com.learn.assistant.rag.vectorstore;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** 按向量库配置访问 pgvector 表。 */
public class PgVectorCatalog {

  private final JdbcTemplate jdbcTemplate;

  private final PgVectorStoreProperties vectorStoreProperties;

  public PgVectorCatalog(JdbcTemplate jdbcTemplate, PgVectorStoreProperties vectorStoreProperties) {
    this.jdbcTemplate = jdbcTemplate;
    this.vectorStoreProperties = vectorStoreProperties;
  }

  public int clear() {
    String sql = "DELETE FROM " + VectorTables.qualified(vectorStoreProperties);
    return jdbcTemplate.update(sql);
  }

  public List<LibraryFile> listFiles() {
    String sql =
        "SELECT metadata->>'source_file' AS file, COUNT(*) AS chunks, "
            + "COUNT(DISTINCT metadata->>'page_number') AS pages FROM "
            + VectorTables.qualified(vectorStoreProperties)
            + " WHERE metadata->>'source_file' IS NOT NULL AND metadata->>'source_file' <> '' "
            + "GROUP BY metadata->>'source_file' ORDER BY metadata->>'source_file'";
    List<LibraryFile> files =
        jdbcTemplate.query(
            sql,
            (result, row) ->
                new LibraryFile(
                    result.getString("file"), result.getInt("pages"), result.getInt("chunks")));
    return files == null ? List.of() : files;
  }

  public List<String> hashesOf(String file) {
    String sql =
        "SELECT metadata->>'chunkHash' FROM "
            + VectorTables.qualified(vectorStoreProperties)
            + " WHERE metadata->>'source_file' = ?";
    List<String> stored = jdbcTemplate.query(sql, (result, row) -> result.getString(1), file);
    return stored == null ? List.of() : stored;
  }

  public int deleteBySourceFile(String file) {
    if (file == null || file.isBlank()) {
      return 0;
    }
    String sql =
        "DELETE FROM "
            + VectorTables.qualified(vectorStoreProperties)
            + " WHERE metadata->>'source_file' = ?";
    return jdbcTemplate.update(sql, file);
  }

  public int collapseDuplicates() {
    String table = VectorTables.qualified(vectorStoreProperties);
    String sql =
        "DELETE FROM "
            + table
            + " AS extra USING "
            + table
            + " AS kept "
            + "WHERE extra.content = kept.content AND extra.ctid > kept.ctid";
    return jdbcTemplate.update(sql);
  }
}
