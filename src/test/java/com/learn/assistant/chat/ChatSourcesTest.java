package com.learn.assistant.chat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSourcesTest {

    @Test
    void keepsFileAndPageOnce() {
        List<com.learn.assistant.domain.vo.ChatSource> sources = ChatSources.fromDocuments(List.of(
                new Document("第一段", Map.of("source_file", "课程/笔记.pdf", "page_number", 2)),
                new Document("第二段", Map.of("source_file", "课程/笔记.pdf", "page_number", 2))));

        assertEquals(1, sources.size());
        assertEquals("课程/笔记.pdf", sources.get(0).file());
        assertEquals(2, sources.get(0).page());
        assertTrue(ChatSources.wire(sources.get(0)).contains("课程/笔记.pdf"));
    }
}
