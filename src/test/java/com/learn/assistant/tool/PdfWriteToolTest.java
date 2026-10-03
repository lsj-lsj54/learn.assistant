package com.learn.assistant.tool;

import com.learn.assistant.properties.ToolProperties;
import com.learn.assistant.tool.concretetool.PdfWriteTool;
import com.learn.assistant.tool.path.ProjectPaths;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfWriteToolTest {

    @TempDir
    Path tempDir;

    @Test
    void writePdfSavesChineseText() throws Exception {
        PdfWriteTool tool = new PdfWriteTool(new TempProjectPaths(tempDir), new ToolProperties());

        String result = tool.writePdf("note", "我的名字是小明。");

        assertTrue(result.startsWith("已写入"));
        Path pdf = tempDir.resolve("src/main/resources/pdf/note.pdf");
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("小明"));
        }
    }

    @Test
    void writePdfCreatesSubfolder() {
        PdfWriteTool tool = new PdfWriteTool(new TempProjectPaths(tempDir), new ToolProperties());

        String result = tool.writePdf("课程/note", "正文");

        assertTrue(result.startsWith("已写入"));
        assertTrue(Files.exists(tempDir.resolve("src/main/resources/pdf/课程/note.pdf")));
    }

    @Test
    void writePdfRejectsBlankName() {
        PdfWriteTool tool = new PdfWriteTool(new TempProjectPaths(tempDir), new ToolProperties());

        assertTrue(tool.writePdf("  ", "正文").startsWith("写入 PDF 失败"));
    }

    @Test
    void writePdfReportsMissingConfiguredFont() {
        ToolProperties properties = new ToolProperties();
        properties.setPdfFont(tempDir.resolve("missing.ttf").toString());
        PdfWriteTool tool = new PdfWriteTool(new TempProjectPaths(tempDir), properties);

        String result = tool.writePdf("note", "正文");

        assertTrue(result.contains("找不到配置的字体"));
    }

    private static final class TempProjectPaths extends ProjectPaths {

        private final Path root;

        private TempProjectPaths(Path root) {
            super("src/main/resources/pdf", "res");
            this.root = root;
        }

        @Override
        public Path root() {
            return root;
        }
    }
}
