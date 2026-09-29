package com.learn.assistant.rag;

import com.learn.assistant.prompts.RagPrompts;
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

    private final boolean allowEmptyContext;

    private final QueryAugmenter whenDocumentsPresent;

    public ConversationAwareQueryAugmenter(boolean allowEmptyContext) {
        this.allowEmptyContext = allowEmptyContext;
        this.whenDocumentsPresent = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .promptTemplate(new PromptTemplate(RagPrompts.WITH_CONTEXT))
                .build();
    }

    @Override
    public Query augment(Query query, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            if (allowEmptyContext) {
                return query;
            }
            return new Query(RagPrompts.WITHOUT_CONTEXT + query.text());
        }
        return whenDocumentsPresent.augment(query, documents);
    }
}
