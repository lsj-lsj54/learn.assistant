package com.learn.assistant.prompts;

public final class ToolPrompts {

    public static final String LIST_FILES = "列出项目目录中的文件和子目录。路径相对于项目根目录，空字符串表示项目根目录。";

    public static final String LIST_FILES_PATH = "相对项目根目录的路径，可为空";

    public static final String READ_TEXT = "读取项目内的文本文件。路径相对于项目根目录。";

    public static final String READ_TEXT_PATH = "相对项目根目录的文件路径";

    public static final String WRITE_TEXT = "把文本写入项目内的文件。路径相对于项目根目录，父目录不存在时会创建。";

    public static final String WRITE_TEXT_PATH = "相对项目根目录的文件路径";

    public static final String WRITE_TEXT_CONTENT = "要写入的文本";

    public static final String DELETE_FILE = "删除项目内的文件。路径相对于项目根目录。不能删除目录。";

    public static final String DELETE_FILE_PATH = "相对项目根目录的文件路径";

    public static final String WRITE_PDF = "把文本写成 PDF，保存到项目的 src/main/resources/pdf 目录。文件名可以带子文件夹，例如 课程/note.pdf，没有该文件夹时会新建。";

    public static final String WRITE_PDF_NAME = "保存路径，例如 note.pdf 或 课程/note.pdf";

    public static final String WRITE_PDF_CONTENT = "要写入 PDF 的正文";

    public static final String DOWNLOAD = "下载 http 或 https 文件直链。PDF 保存到 src/main/resources/pdf，其他资源保存到 res。必须使用「分类/文件名」，例如 游戏/威龙.png、风景/泰山.jpg。先看返回的已有分类，选最合适的；没有就新建一个简短中文分类。不要使用普通网页地址。";

    public static final String DOWNLOAD_URL = "以 http 或 https 开头的文件直链";

    public static final String DOWNLOAD_FILE_NAME = "分类/文件名，例如 游戏/威龙.png 或 风景/泰山.jpg";

    public static final String SEARCH = "用博查 Web Search API 联网搜索。返回网页链接和文件直链。用户要保存文件时，把直链交给下载工具：PDF 会进 pdf 目录，其他资源进 res。不要改去抓取网页。";

    public static final String SEARCH_QUERY = "搜索关键词";

    public static final String SCRAPE = "抓取网页正文，去掉脚本和样式后返回文本。不能从页面得到文件地址。下载文件请把直链交给下载工具。";

    public static final String SCRAPE_URL = "以 http 或 https 开头的网页地址";

    private ToolPrompts() {
    }

    public static String downloadNeedsCategory(String existingCategories) {
        return "需要按分类保存，文件还没写入。已有分类：" + existingCategories
                + "。请选择最合适的已有分类，用「分类/文件名」再次下载。没有合适分类就新建一个简短中文分类，例如 游戏/威龙.png、风景/泰山.jpg。";
    }
}
