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

    private final String configuredFont;

    public PdfWriteTool(ProjectPaths projectPaths, ToolProperties toolProperties) {
        this.projectPaths = projectPaths;
        this.configuredFont = toolProperties.getPdfFont() == null ? "" : toolProperties.getPdfFont().trim();
    }

    @Tool(description = ToolPrompts.WRITE_PDF)
    public String writePdf(
            @ToolParam(description = ToolPrompts.WRITE_PDF_NAME) String fileName,
            @ToolParam(description = ToolPrompts.WRITE_PDF_CONTENT) String content) {
        return ToolCallLog.record("PdfWriteTool.writePdf",
                "fileName=" + fileName + ", contentLength=" + (content == null ? 0 : content.length()),
                () -> writePdfFile(fileName, content));
    }

    private String writePdfFile(String fileName, String content) {
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
            PDFont font = loadFont(document);
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

    private PDFont loadFont(PDDocument document) throws IOException {
        Path fontFile = resolveFont();
        if (fontFile.getFileName().toString().toLowerCase().endsWith(".ttc")) {
            try (var input = Files.newInputStream(fontFile)) {
                return PDType0Font.load(document, input, true);
            }
        }
        return PDType0Font.load(document, fontFile.toFile());
    }

    private Path resolveFont() throws IOException {
        if (!configuredFont.isBlank()) {
            Path configured = Path.of(configuredFont);
            if (!Files.isRegularFile(configured)) {
                throw new IOException("找不到配置的字体: " + configured);
            }
            return configured;
        }
        for (Path candidate : fontCandidates()) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new IOException("没有找到可用的中文字体。请设置 learn.tools.pdf-font，指向一个 .ttf、.otf 或 .ttc 文件");
    }

    private static List<Path> fontCandidates() {
        List<Path> candidates = new ArrayList<>();
        addNamed(candidates, Path.of("C:/Windows/Fonts"), "simhei.ttf", "msyh.ttf", "msyh.ttc", "simsun.ttc");
        addNamed(candidates, Path.of("/usr/share/fonts/truetype/wqy"), "wqy-microhei.ttc", "wqy-zenhei.ttc");
        addNamed(candidates, Path.of("/usr/share/fonts/opentype/noto"), "NotoSansCJK-Regular.ttc", "NotoSansCJKsc-Regular.otf");
        addNamed(candidates, Path.of("/usr/share/fonts/truetype/noto"), "NotoSansCJK-Regular.ttc");
        addNamed(candidates, Path.of("/System/Library/Fonts"), "PingFang.ttc", "STHeiti Light.ttc");
        addNamed(candidates, Path.of("/Library/Fonts"), "Arial Unicode.ttf");
        return candidates;
    }

    private static void addNamed(List<Path> candidates, Path directory, String... names) {
        for (String name : names) {
            candidates.add(directory.resolve(name));
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
