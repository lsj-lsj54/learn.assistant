package com.learn.assistant.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;

import java.util.List;

/**
 * 检索到资料时把资料附在问题上，同时允许使用本段对话。
 * 没有检索到资料时，仍保留用户原话，避免对话记忆被固定回复盖掉。
 */
public final class ConversationAwareQueryAugmenter implements QueryAugmenter {

    static final String WITH_CONTEXT = """
            下面是资料库检索到的内容，可能和当前问题无关。

            ---------------------
            {context}
            ---------------------

            回答时同时遵守这些规则：
            1. 同一段对话里用户说过的信息要记住，并在后续问题里使用。
            2. 用户正在告诉你的新信息，先确认已经记下。
            3. 资料库内容用来回答资料里的问题，不要把无关段落当成答案。
            4. 资料和对话里都没有答案时，就说不知道。不要编造。
            5. 用户要求搜索、下载或读写文件时，调用工具完成，不要改口说不知道。

            问题：{query}

            回答：
            """;

    private static final String WITHOUT_CONTEXT = """
            资料库没有检索到相关内容。不要编造资料里的事实。
            同一段对话里用户说过的信息要记住，并在后续问题里使用。
            如果用户正在告诉你新的信息，先确认已经记下。
            资料和对话里都没有答案时，就说不知道。
            用户要求搜索、下载或读写文件时，调用工具完成，不要改口说不知道。

            用户说：""";

    private final boolean allowEmptyContext;

    private final QueryAugmenter whenDocumentsPresent;

    public ConversationAwareQueryAugmenter(boolean allowEmptyContext) {
        this.allowEmptyContext = allowEmptyContext;
        this.whenDocumentsPresent = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .promptTemplate(new PromptTemplate(WITH_CONTEXT))
                .build();
    }

    @Override
    public Query augment(Query query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            if (allowEmptyContext) {
                return query;
            }
            return new Query(WITHOUT_CONTEXT + query.text());
        }
        return whenDocumentsPresent.augment(query, documents);
    }
}
