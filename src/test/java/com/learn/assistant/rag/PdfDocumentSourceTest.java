package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfDocumentSourceTest {

    @Test
    void readsPdfFromClasspath() {
        String text = new PdfDocumentSource().read().stream()
                .map(document -> document.getText())
                .reduce("", (left, right) -> left + right);

        assertFalse(text.isBlank());
    }

    @Test
    void readsPdfInsideSubfolder() {
        boolean nested = new PdfDocumentSource().collect().keySet().stream()
                .anyMatch(name -> name.contains("/"));

        assertTrue(nested);
    }
}
