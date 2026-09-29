package com.learn.assistant.tool;

import org.springframework.ai.model.tool.internal.ToolCallReactiveContextHolder;
import reactor.util.context.ContextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次流式回答里的工具调用。Spring AI 在执行工具前把 Reactor 上下文放进当前线程，
 * 所以工具方法里能把结果交回这条回答。
 */
public final class ToolActivity {

    public static final String CONTEXT_KEY = "learn.toolActivity";

    private static final int MAX_CHARS = 500;

    private final List<ToolStep> finished = new ArrayList<>();

    public void finish(String tool, String arguments, String result) {
        ToolStep step = new ToolStep(ToolNames.display(tool), shorten(arguments), shorten(result));
        synchronized (finished) {
            finished.add(step);
        }
    }

    public List<ToolStep> drain() {
        synchronized (finished) {
            if (finished.isEmpty()) {
                return List.of();
            }
            List<ToolStep> copy = List.copyOf(finished);
            finished.clear();
            return copy;
        }
    }

    public static ToolActivity current() {
        ContextView view = ToolCallReactiveContextHolder.getContext();
        if (view == null || !view.hasKey(CONTEXT_KEY)) {
            return null;
        }
        Object value = view.get(CONTEXT_KEY);
        return value instanceof ToolActivity activity ? activity : null;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        String singleLine = text.replace("\r\n", "\n").replace('\r', '\n').replace('\n', ' ').trim();
        if (singleLine.length() <= MAX_CHARS) {
            return singleLine;
        }
        return singleLine.substring(0, MAX_CHARS) + "...(已截断)";
    }
}
