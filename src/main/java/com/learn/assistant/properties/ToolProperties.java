package com.learn.assistant.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learn.tools")
public class ToolProperties {

    private String pdfFont = "C:/Windows/Fonts/simhei.ttf";

    private String bochaApiKey = "";

    private int scrapeMaxChars = 8_000;

    private int readMaxChars = 100_000;

    public String getPdfFont() {
        return pdfFont;
    }

    public void setPdfFont(String pdfFont) {
        this.pdfFont = pdfFont;
    }

    public String getBochaApiKey() {
        return bochaApiKey;
    }

    public void setBochaApiKey(String bochaApiKey) {
        this.bochaApiKey = bochaApiKey;
    }

    public int getScrapeMaxChars() {
        return scrapeMaxChars;
    }

    public void setScrapeMaxChars(int scrapeMaxChars) {
        this.scrapeMaxChars = scrapeMaxChars;
    }

    public int getReadMaxChars() {
        return readMaxChars;
    }

    public void setReadMaxChars(int readMaxChars) {
        this.readMaxChars = readMaxChars;
    }

    public static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
