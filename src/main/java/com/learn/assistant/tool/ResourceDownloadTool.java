package com.learn.assistant.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

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

    @Tool(description = "下载网络图片到项目的 res 目录。")
    public String downloadImage(
            @ToolParam(description = "以 http 或 https 开头的图片地址") String url,
            @ToolParam(description = "保存的文件名，例如 photo.png；留空则从地址中取文件名", required = false) String fileName) {
        try {
            publicHttp.checkPublicHttp(url);
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
                String name = resolveFileName(url, fileName);
                if (!type.startsWith("image/") && !isImageName(name)) {
                    return "该地址不是图片";
                }
                Path directory = projectPaths.downloadDirectory();
                Files.createDirectories(directory);
                Path target = projectPaths.resolveWithin(directory, name);
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) {
                    return "图片超过 10MB，已取消保存";
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

    private static String resolveFileName(String url, String fileName) {
        String name = fileName;
        if (name == null || name.isBlank()) {
            String path = URI.create(url).getPath();
            int slash = path == null ? -1 : path.lastIndexOf('/');
            name = slash >= 0 ? path.substring(slash + 1) : "";
        }
        if (name.isBlank()) {
            name = "image.bin";
        }
        name = Path.of(name).getFileName().toString().replaceAll("[\\\\/:*?\"<>|]", "_");
        return name.isBlank() ? "image.bin" : name;
    }

    private static boolean isImageName(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp");
    }
}
