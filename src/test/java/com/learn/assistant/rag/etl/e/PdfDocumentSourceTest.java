package com.learn.assistant.rag.etl.e;

import com.learn.assistant.tool.ProjectPaths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfDocumentSourceTest {

    @Test
    void readsPdfFromClasspath() {
        String text = new PdfExtract(new ProjectPaths("src/main/resources/pdf", "res")).read().stream()
                .map(document -> document.getText())
                .reduce("", (left, right) -> left + right);

        assertFalse(text.isBlank());
    }

    @Test
    void readsPdfInsideSubfolder() {
        boolean nested = new PdfExtract(new ProjectPaths("src/main/resources/pdf", "res")).collect().keySet().stream()
                .anyMatch(name -> name.contains("/"));

        assertTrue(nested);
    }
}
