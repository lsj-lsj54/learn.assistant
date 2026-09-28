package com.learn.assistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

    private static final int MAX_TEXT_LENGTH = 100_000;

    private final ProjectPaths projectPaths;

    public FileOperationTool(ProjectPaths projectPaths) {
        this.projectPaths = projectPaths;
    }

    @Tool(description = "列出项目目录中的文件和子目录。路径相对于项目根目录，空字符串表示项目根目录。")
    public String listFiles(@ToolParam(description = "相对项目根目录的路径，可为空") String relativePath) {
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

    @Tool(description = "读取项目内的文本文件。路径相对于项目根目录。")
    public String readText(@ToolParam(description = "相对项目根目录的文件路径") String relativePath) {
        try {
            Path file = projectPaths.resolveWithin(projectPaths.root(), relativePath);
            if (!Files.isRegularFile(file)) {
                return "文件不存在: " + file;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.length() > MAX_TEXT_LENGTH) {
                return text.substring(0, MAX_TEXT_LENGTH) + "\n...内容已截断";
            }
            return text;
        }
        catch (Exception exception) {
            return "读取文件失败: " + exception.getMessage();
        }
    }

    @Tool(description = "把文本写入项目内的文件。路径相对于项目根目录，父目录不存在时会创建。")
    public String writeText(
            @ToolParam(description = "相对项目根目录的文件路径") String relativePath,
            @ToolParam(description = "要写入的文本") String content) {
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

    @Tool(description = "删除项目内的文件。路径相对于项目根目录。不能删除目录。")
    public String deleteFile(@ToolParam(description = "相对项目根目录的文件路径") String relativePath) {
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
