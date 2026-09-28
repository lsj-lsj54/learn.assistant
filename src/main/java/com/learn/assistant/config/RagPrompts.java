package com.learn.assistant.config;

public final class RagPrompts {

    public static final String CONTEXT = """
            上下文如下。

            ---------------------
            {context}
            ---------------------

            只根据上下文回答，不要使用上下文以外的知识。
            如果上下文里没有答案，就说不知道。

            问题：{query}

            回答：
            """;

    public static final String EMPTY_CONTEXT = "请只回复：上下文为空";

    private RagPrompts() {
    }
}
