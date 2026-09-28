package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationAwareQueryAugmenterTest {

    @Test
    void keepsUserWordsWhenContextIsEmptyAndNotAllowed() {
        Query result = new ConversationAwareQueryAugmenter(false)
                .augment(new Query("我有个朋友叫威龙"), List.of());

        assertTrue(result.text().contains("我有个朋友叫威龙"));
        assertTrue(result.text().contains("记住"));
    }

    @Test
    void returnsOriginalQueryWhenEmptyContextIsAllowed() {
        Query result = new ConversationAwareQueryAugmenter(true)
                .augment(new Query("我有个朋友叫威龙"), List.of());

        assertEquals("我有个朋友叫威龙", result.text());
    }

    @Test
    void includesDocumentsWithoutForbiddingConversation() {
        Query result = new ConversationAwareQueryAugmenter(false)
                .augment(new Query("我叫什么"), List.of(new Document("林嘉豪在霍格沃茨。")));

        assertTrue(result.text().contains("林嘉豪在霍格沃茨。"));
        assertTrue(result.text().contains("我叫什么"));
        assertTrue(result.text().contains("对话里用户说过的信息要记住"));
    }
}
