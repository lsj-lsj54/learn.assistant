package com.learn.assistant.tool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@AssistantTool
@Order(2)
public class WebSearchTool {

    @Tool(description = "联网搜索，返回若干条标题和链接。")
    public String search(@ToolParam(description = "搜索关键词") String query) {
        try {
            Document document = Jsoup.connect("https://html.duckduckgo.com/html/")
                    .data("q", query)
                    .userAgent("Mozilla/5.0")
                    .timeout(15_000)
                    .post();
            StringBuilder result = new StringBuilder();
            int count = 0;
            for (Element link : document.select("a.result__a")) {
                if (count == 5) {
                    break;
                }
                String title = link.text();
                String href = link.absUrl("href");
                if (title.isBlank() || href.isBlank()) {
                    continue;
                }
                result.append(title).append('\n').append(href).append("\n\n");
                count++;
            }
            return result.isEmpty() ? "没有搜索到结果" : result.toString().trim();
        }
        catch (Exception exception) {
            return "搜索失败: " + exception.getMessage();
        }
    }
}
