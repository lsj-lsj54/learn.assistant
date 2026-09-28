package com.learn.assistant.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learn.tools")
public class ToolProperties {

    private String pdfFont = "C:/Windows/Fonts/simhei.ttf";

    private String bochaApiKey = "";

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
}
