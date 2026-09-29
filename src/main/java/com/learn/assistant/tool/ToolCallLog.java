package com.learn.assistant.tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

final class ToolCallLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int MAX_CHARS = 800;

    private static final Object LOCK = new Object();

    private static Path directory = Path.of("log");

    private ToolCallLog() {
    }

    static String record(String tool, String arguments, Supplier<String> action) {
        long started = System.nanoTime();
        try {
            String result = action.get();
            write(tool, arguments, result, started);
            return result;
        }
        catch (RuntimeException exception) {
            write(tool, arguments, "异常: " + exception.getMessage(), started);
            throw exception;
        }
    }

    static void useDirectory(Path path) {
        directory = path;
    }

    private static void write(String tool, String arguments, String result, long startedNanos) {
        long millis = (System.nanoTime() - startedNanos) / 1_000_000;
        String entry = TIME.format(LocalDateTime.now()) + " " + tool + "\n"
                + "参数: " + shorten(arguments) + "\n"
                + "结果: " + shorten(result) + "\n"
                + "耗时: " + millis + "ms\n"
                + "---\n";
        synchronized (LOCK) {
            try {
                Path folder = directory.toAbsolutePath().normalize();
                Files.createDirectories(folder);
                Files.writeString(folder.resolve("tool.log"), entry, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            catch (IOException exception) {
                // 日志写失败时不打断工具本身
            }
        }
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        String singleLine = text.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ');
        if (singleLine.length() <= MAX_CHARS) {
            return singleLine;
        }
        return singleLine.substring(0, MAX_CHARS) + "...(已截断)";
    }
}
