package com.learn.assistant.tool;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfWriteToolTest {

    @TempDir
    Path tempDir;

    @Test
    void writePdfSavesChineseText() throws Exception {
        PdfWriteTool tool = new PdfWriteTool(new TempProjectPaths(tempDir));

        String result = tool.writePdf("note", "我的名字是小明。");

        assertTrue(result.startsWith("已写入"));
        Path pdf = tempDir.resolve("src/main/resources/pdf/note.pdf");
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("小明"));
        }
    }

    @Test
    void writePdfRejectsBlankName() {
        PdfWriteTool tool = new PdfWriteTool(new TempProjectPaths(tempDir));

        assertTrue(tool.writePdf("  ", "正文").startsWith("写入 PDF 失败"));
    }

    private static final class TempProjectPaths extends ProjectPaths {

        private final Path root;

        private TempProjectPaths(Path root) {
            this.root = root;
        }

        @Override
        public Path root() {
            return root;
        }
    }
}
