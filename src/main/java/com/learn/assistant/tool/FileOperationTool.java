package com.learn.assistant.tool;

import com.learn.assistant.prompts.ToolPrompts;
import com.learn.assistant.properties.ToolProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Component
@AssistantTool
@Order(1)
public class FileOperationTool {

    private final ProjectPaths projectPaths;

    private final int maxTextLength;

    public FileOperationTool(ProjectPaths projectPaths) {
        this(projectPaths, new ToolProperties());
    }

    @Autowired
    public FileOperationTool(ProjectPaths projectPaths, ToolProperties toolProperties) {
        this.projectPaths = projectPaths;
        this.maxTextLength = ToolProperties.positive(toolProperties.getReadMaxChars(), 100_000);
    }

    @Tool(description = ToolPrompts.LIST_FILES)
    public String listFiles(@ToolParam(description = ToolPrompts.LIST_FILES_PATH) String relativePath) {
        try {
            Path directory = projectPaths.resolveWithin(projectPaths.root(), relativePath);
            if (!Files.isDirectory(directory)) {
                return "不是目录: " + directory;
            }
            StringBuilder result = new StringBuilder();
            try (Stream<Path> children = Files.list(directory)) {
                children.limit(200).forEach(child -> result.append(directory.relativize(child)).append('\n'));
            }
            return result.isEmpty() ? "目录为空" : result.toString().trim();
        }
        catch (Exception exception) {
            return "列出文件失败: " + exception.getMessage();
        }
    }

    @Tool(description = ToolPrompts.READ_TEXT)
    public String readText(@ToolParam(description = ToolPrompts.READ_TEXT_PATH) String relativePath) {
        try {
            Path file = projectPaths.resolveWithin(projectPaths.root(), relativePath);
            if (!Files.isRegularFile(file)) {
                return "文件不存在: " + file;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.length() > maxTextLength) {
                return text.substring(0, maxTextLength) + "\n...内容已截断";
            }
            return text;
        }
        catch (Exception exception) {
            return "读取文件失败: " + exception.getMessage();
        }
    }

    @Tool(description = ToolPrompts.WRITE_TEXT)
    public String writeText(
            @ToolParam(description = ToolPrompts.WRITE_TEXT_PATH) String relativePath,
            @ToolParam(description = ToolPrompts.WRITE_TEXT_CONTENT) String content) {
        try {
            Path file = projectPaths.resolveWithin(projectPaths.root(), relativePath);
            Files.createDirectories(file.getParent());
            Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
            return "已写入 " + file;
        }
        catch (Exception exception) {
            return "写入文件失败: " + exception.getMessage();
        }
    }

    @Tool(description = ToolPrompts.DELETE_FILE)
    public String deleteFile(@ToolParam(description = ToolPrompts.DELETE_FILE_PATH) String relativePath) {
        try {
            Path file = projectPaths.resolveWithin(projectPaths.root(), relativePath);
            if (Files.isDirectory(file)) {
                return "不能删除目录";
            }
            if (!Files.deleteIfExists(file)) {
                return "文件不存在: " + file;
            }
            return "已删除 " + file;
        }
        catch (IOException exception) {
            return "删除文件失败: " + exception.getMessage();
        }
        catch (RuntimeException exception) {
            return "删除文件失败: " + exception.getMessage();
        }
    }
}
