package com.learn.assistant.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learn.paths")
public class PathProperties {

    private String pdfDir = "src/main/resources/pdf";

    private String downloadDir = "res";

    public String getPdfDir() {
        return pdfDir;
    }

    public void setPdfDir(String pdfDir) {
        this.pdfDir = pdfDir;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }
}
