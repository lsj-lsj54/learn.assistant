package com.learn.assistant.tool;

public final class ToolNames {

    private ToolNames() {
    }

    public static String display(String raw) {
        if (raw == null || raw.isBlank()) {
            return "工具";
        }
        return switch (raw) {
            case "search", "WebSearchTool.search" -> "搜索";
            case "scrape", "WebScrapeTool.scrape" -> "抓取网页";
            case "download", "ResourceDownloadTool.download" -> "下载";
            case "listFiles", "FileOperationTool.listFiles" -> "列出文件";
            case "readText", "FileOperationTool.readText" -> "读取文件";
            case "writeText", "FileOperationTool.writeText" -> "写入文件";
            case "deleteFile", "FileOperationTool.deleteFile" -> "删除文件";
            case "writePdf", "PdfWriteTool.writePdf" -> "写 PDF";
            default -> raw;
        };
    }
}
