package com.learn.assistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

@Component
@AssistantTool
@Order(3)
public class ResourceDownloadTool {

    private static final int MAX_BYTES = 10 * 1024 * 1024;

    private final ProjectPaths projectPaths;

    private final PublicHttp publicHttp;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ResourceDownloadTool(ProjectPaths projectPaths, PublicHttp publicHttp) {
        this.projectPaths = projectPaths;
        this.publicHttp = publicHttp;
    }

    @Tool(description = "下载 http 或 https 文件直链。PDF 保存到 src/main/resources/pdf，其他资源保存到 res。必须使用「分类/文件名」，例如 游戏/威龙.png、风景/泰山.jpg。先看返回的已有分类，选最合适的；没有就新建一个简短中文分类。不要使用普通网页地址。")
    public String download(
            @ToolParam(description = "以 http 或 https 开头的文件直链") String url,
            @ToolParam(description = "分类/文件名，例如 游戏/威龙.png 或 风景/泰山.jpg", required = false) String fileName) {
        try {
            publicHttp.checkPublicHttp(url);
            String name = resolveFileName(url, fileName);
            if (!name.contains("/")) {
                return categoryPrompt(name.toLowerCase().endsWith(".pdf")
                        ? projectPaths.pdfDirectory()
                        : projectPaths.downloadDirectory());
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream input = response.body()) {
                if (response.statusCode() >= 400) {
                    return "下载失败，HTTP " + response.statusCode();
                }
                String type = response.headers().firstValue("content-type").orElse("");
                Path directory = directoryFor(type, name);
                Path target = projectPaths.resolveWithin(directory, name);
                Files.createDirectories(target.getParent());
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) {
                    return "文件超过 10MB，已取消保存";
                }
                Files.write(target, bytes);
                return "已下载到 " + target;
            }
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "下载失败: " + exception.getMessage();
        }
        catch (Exception exception) {
            return "下载失败: " + exception.getMessage();
        }
    }

    private String resolveFileName(String url, String fileName) {
        String name = fileName;
        if (name == null || name.isBlank()) {
            String path = URI.create(url).getPath();
            if (path != null) {
                int slash = path.lastIndexOf('/');
                name = slash >= 0 ? path.substring(slash + 1) : path;
            }
        }
        try {
            return projectPaths.safeRelative(name);
        }
        catch (IllegalArgumentException exception) {
            if ("文件名不能为空".equals(exception.getMessage())) {
                return "download.bin";
            }
            throw exception;
        }
    }

    private String categoryPrompt(Path directory) throws IOException {
        String existing = "还没有分类";
        if (Files.isDirectory(directory)) {
            try (Stream<Path> children = Files.list(directory)) {
                String names = children.filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .sorted()
                        .reduce((left, right) -> left + "、" + right)
                        .orElse("");
                if (!names.isBlank()) {
                    existing = names;
                }
            }
        }
        return "需要按分类保存，文件还没写入。已有分类：" + existing
                + "。请选择最合适的已有分类，用「分类/文件名」再次下载。没有合适分类就新建一个简短中文分类，例如 游戏/威龙.png、风景/泰山.jpg。";
    }

    Path directoryFor(String contentType, String fileName) {
        if (isPdf(contentType, fileName)) {
            return projectPaths.pdfDirectory();
        }
        return projectPaths.downloadDirectory();
    }

    private static boolean isPdf(String contentType, String fileName) {
        String type = contentType == null ? "" : contentType.toLowerCase();
        int semicolon = type.indexOf(';');
        if (semicolon >= 0) {
            type = type.substring(0, semicolon).trim();
        }
        String name = fileName == null ? "" : fileName.toLowerCase();
        return "application/pdf".equals(type) || name.endsWith(".pdf");
    }
}
