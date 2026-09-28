package com.learn.assistant.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.assistant.properties.ToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@AssistantTool
@Order(2)
public class WebSearchTool {

    static final String ENDPOINT = "https://api.bochaai.com/v1/web-search";

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

    private static final int RESULT_LIMIT = 5;

    private final String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public WebSearchTool(ToolProperties toolProperties) {
        this(toolProperties, "");
    }

    @Autowired
    public WebSearchTool(ToolProperties toolProperties, @Value("${BOCHA_API_KEY:}") String bochaApiKey) {
        this.apiKey = firstNonBlank(toolProperties.getBochaApiKey(), bochaApiKey);
        if (this.apiKey.isBlank()) {
            log.warn("博查 Web Search API 未读到 BOCHA_API_KEY");
        }
        else {
            log.info("博查 Web Search API 已配置");
        }
    }

    @Tool(description = "用博查 Web Search API 联网搜索。返回网页链接和文件直链。用户要保存文件时，把直链交给下载工具：PDF 会进 pdf 目录，其他资源进 res。不要改去抓取网页。")
    public String search(@ToolParam(description = "搜索关键词") String query) {
        if (apiKey.isBlank()) {
            return "搜索失败: 未配置博查 API Key";
        }
        try {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "query", query == null ? "" : query,
                    "freshness", "noLimit",
                    "summary", false,
                    "count", RESULT_LIMIT));
            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return "搜索失败: HTTP " + response.statusCode();
            }
            return formatResults(response.body(), objectMapper);
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "搜索失败: " + exception.getMessage();
        }
        catch (Exception exception) {
            return "搜索失败: " + exception.getMessage();
        }
    }

    static String formatResults(String body, ObjectMapper objectMapper) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode pages = section(root, "webPages");
        JsonNode images = section(root, "images");
        if (!hasItems(pages) && !hasItems(images)) {
            String message = text(root, "msg");
            if (message.isBlank()) {
                message = text(root, "message");
            }
            JsonNode code = root.get("code");
            if (code != null && code.canConvertToInt() && code.asInt() != 200) {
                return "搜索失败: " + (message.isBlank() ? "业务码 " + code.asInt() : message);
            }
            return "没有搜索到结果";
        }
        StringBuilder result = new StringBuilder();
        appendLinks(result, "图片直链", images, "contentUrl", "url", "thumbnailUrl");
        appendLinks(result, "网页", pages, "url");
        return result.isEmpty() ? "没有搜索到结果" : result.toString().trim();
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static void appendLinks(StringBuilder result, String heading, JsonNode items, String... urlFields) {
        if (!hasItems(items)) {
            return;
        }
        if (!result.isEmpty()) {
            result.append("\n\n");
        }
        result.append(heading).append("：\n");
        int count = 0;
        for (JsonNode item : items) {
            if (count == RESULT_LIMIT) {
                break;
            }
            String title = text(item, "name");
            String url = firstText(item, urlFields);
            if (url.isBlank()) {
                continue;
            }
            if (title.isBlank()) {
                if (!"图片直链".equals(heading)) {
                    continue;
                }
                title = "图片";
            }
            result.append(title).append('\n').append(url).append('\n');
            count++;
        }
    }

    private static JsonNode section(JsonNode root, String name) {
        JsonNode pages = root.path(name).path("value");
        if (hasItems(pages)) {
            return pages;
        }
        return root.path("data").path(name).path("value");
    }

    private static boolean hasItems(JsonNode node) {
        return node != null && node.isArray() && !node.isEmpty();
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
