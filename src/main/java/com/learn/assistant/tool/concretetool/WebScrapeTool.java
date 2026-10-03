package com.learn.assistant.tool.concretetool;

import com.learn.assistant.prompts.ToolPrompts;
import com.learn.assistant.tool.config.AssistantTool;
import com.learn.assistant.tool.http.PublicHttp;
import com.learn.assistant.tool.log.ToolCallLog;
import com.learn.assistant.properties.ToolProperties;
import org.jsoup.Jsoup;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@AssistantTool
@Order(4)
public class WebScrapeTool {

    private final PublicHttp publicHttp;

    private final int maxTextLength;

    public WebScrapeTool(PublicHttp publicHttp) {
        this(publicHttp, new ToolProperties());
    }

    @Autowired
    public WebScrapeTool(PublicHttp publicHttp, ToolProperties toolProperties) {
        this.publicHttp = publicHttp;
        this.maxTextLength = ToolProperties.positive(toolProperties.getScrapeMaxChars(), 8_000);
    }

    @Tool(description = ToolPrompts.SCRAPE)
    public String scrape(@ToolParam(description = ToolPrompts.SCRAPE_URL) String url) {
        return ToolCallLog.record("WebScrapeTool.scrape", "url=" + url, () -> scrapePage(url));
    }

    private String scrapePage(String url) {
        try {
            publicHttp.checkPublicHttp(url);
            String text = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(15_000)
                    .maxBodySize(1_000_000)
                    .get()
                    .body()
                    .text();
            if (text.length() > maxTextLength) {
                return text.substring(0, maxTextLength) + "\n...内容已截断";
            }
            return text.isBlank() ? "页面没有可提取的文本" : text;
        }
        catch (Exception exception) {
            return "抓取失败: " + exception.getMessage();
        }
    }
}
