package com.learn.assistant.tool;

import org.jsoup.Jsoup;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@AssistantTool
@Order(4)
public class WebScrapeTool {

    private static final int MAX_TEXT_LENGTH = 8_000;

    private final PublicHttp publicHttp;

    public WebScrapeTool(PublicHttp publicHttp) {
        this.publicHttp = publicHttp;
    }

    @Tool(description = "抓取网页正文，去掉脚本和样式后返回文本。")
    public String scrape(@ToolParam(description = "以 http 或 https 开头的网页地址") String url) {
        try {
            publicHttp.checkPublicHttp(url);
            String text = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15_000)
                    .maxBodySize(1_000_000)
                    .get()
                    .body()
                    .text();
            if (text.length() > MAX_TEXT_LENGTH) {
                return text.substring(0, MAX_TEXT_LENGTH) + "\n...内容已截断";
            }
            return text.isBlank() ? "页面没有可提取的文本" : text;
        }
        catch (Exception exception) {
            return "抓取失败: " + exception.getMessage();
        }
    }
}
