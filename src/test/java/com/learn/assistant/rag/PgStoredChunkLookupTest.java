package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PgStoredChunkLookupTest {

    @Test
    void returnsContentsThatAreAlreadyStored() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(eq("SELECT content FROM public.vector_store WHERE content IN (?, ?)"),
                any(RowMapper.class), any(Object[].class))).thenReturn(List.of("已经在库里。"));
        PgStoredChunkLookup lookup = new PgStoredChunkLookup(jdbcTemplate, new PgVectorStoreProperties());

        assertEquals(Set.of("已经在库里。"), lookup.findPresent(List.of("已经在库里。", "这次是新的。")));
    }

    @Test
    void skipsTheQueryWhenThereIsNothingToMatch() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PgStoredChunkLookup lookup = new PgStoredChunkLookup(jdbcTemplate, new PgVectorStoreProperties());

        assertTrue(lookup.findPresent(List.of()).isEmpty());
    }

    @Test
    void rejectsUnsafeVectorTableName() {
        PgVectorStoreProperties properties = new PgVectorStoreProperties();
        properties.setTableName("vector_store;drop");
        PgStoredChunkLookup lookup = new PgStoredChunkLookup(mock(JdbcTemplate.class), properties);

        assertThrows(IllegalArgumentException.class, () -> lookup.findPresent(List.of("一段")));
    }
}
