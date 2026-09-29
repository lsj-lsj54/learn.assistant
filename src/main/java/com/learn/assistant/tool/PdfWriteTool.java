package com.learn.assistant.tool;

import com.learn.assistant.prompts.ToolPrompts;
import com.learn.assistant.properties.ToolProperties;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@AssistantTool
@Order(0)
public class PdfWriteTool {

    private static final float FONT_SIZE = 12;

    private static final float MARGIN = 50;

    private static final float LEADING = 18;

    private final ProjectPaths projectPaths;

    private final Path chineseFont;

    public PdfWriteTool(ProjectPaths projectPaths, ToolProperties toolProperties) {
        this.projectPaths = projectPaths;
        this.chineseFont = Path.of(toolProperties.getPdfFont());
    }

    @Tool(description = ToolPrompts.WRITE_PDF)
    public String writePdf(
            @ToolParam(description = ToolPrompts.WRITE_PDF_NAME) String fileName,
            @ToolParam(description = ToolPrompts.WRITE_PDF_CONTENT) String content) {
        try {
            String safeName = withPdfExtension(projectPaths.safeRelative(fileName));
            Path directory = projectPaths.pdfDirectory();
            Path target = projectPaths.resolveWithin(directory, safeName);
            Files.createDirectories(target.getParent());
            write(target, content == null ? "" : content);
            return "已写入 " + target;
        }
        catch (Exception exception) {
            return "写入 PDF 失败: " + exception.getMessage();
        }
    }

    private void write(Path target, String content) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDFont font = PDType0Font.load(document, chineseFont.toFile());
            float width = PDRectangle.A4.getWidth() - MARGIN * 2;
            List<String> lines = wrap(content, font, width);
            PDPage page = newPage(document);
            PDPageContentStream stream = start(document, page, font);
            try {
                float y = page.getMediaBox().getHeight() - MARGIN;
                for (String line : lines) {
                    if (y < MARGIN) {
                        stream.endText();
                        stream.close();
                        stream = null;
                        page = newPage(document);
                        stream = start(document, page, font);
                        y = page.getMediaBox().getHeight() - MARGIN;
                    }
                    stream.showText(line);
                    stream.newLineAtOffset(0, -LEADING);
                    y -= LEADING;
                }
                stream.endText();
                stream.close();
                stream = null;
            }
            finally {
                if (stream != null) {
                    try {
                        stream.close();
                    }
                    catch (IOException ignored) {
                        // 保留触发失败的原始异常
                    }
                }
            }
            document.save(target.toFile());
        }
    }

    private static PDPage newPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    private static PDPageContentStream start(PDDocument document, PDPage page, PDFont font) throws IOException {
        PDPageContentStream stream = new PDPageContentStream(document, page);
        stream.beginText();
        stream.setFont(font, FONT_SIZE);
        stream.newLineAtOffset(MARGIN, page.getMediaBox().getHeight() - MARGIN);
        return stream;
    }

    private static List<String> wrap(String content, PDFont font, float width) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : content.split("\\R", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char ch = paragraph.charAt(i);
                String next = current.toString() + ch;
                if (font.getStringWidth(next) / 1000 * FONT_SIZE > width && !current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(ch);
            }
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    private static String withPdfExtension(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        String parent = slash >= 0 ? relativePath.substring(0, slash + 1) : "";
        String file = slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
        if (!file.toLowerCase().endsWith(".pdf")) {
            file = file + ".pdf";
        }
        return parent + file;
    }
}
