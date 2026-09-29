package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChunkFingerprintTest {

    @Test
    void sameTextKeepsTheSameHashAndId() {
        Document first = ChunkFingerprint.stamp(new Document("霍格沃茨", Map.of("page_number", 2)));
        Document second = ChunkFingerprint.stamp(new Document("霍格沃茨"));

        assertEquals(ChunkFingerprint.hash("霍格沃茨"), first.getMetadata().get(ChunkFingerprint.METADATA_KEY));
        assertEquals(first.getId(), second.getId());
        assertEquals(2, first.getMetadata().get("page_number"));
        UUID.fromString(first.getId());
    }

    @Test
    void differentTextGetsDifferentHash() {
        assertNotEquals(ChunkFingerprint.hash("威龙"), ChunkFingerprint.hash("泰山"));
    }
}
