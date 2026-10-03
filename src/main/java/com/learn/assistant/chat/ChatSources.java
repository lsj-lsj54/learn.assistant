package com.learn.assistant.chat;

import com.learn.assistant.domain.vo.ChatSource;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChatSources {

    private ChatSources() {
    }

    public static List<ChatSource> from(ChatClientResponse response) {
        if (response == null) {
            return List.of();
        }
        List<ChatSource> sources = fromDocuments(response.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT));
        if (!sources.isEmpty()) {
            return sources;
        }
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null || chatResponse.getMetadata() == null) {
            return List.of();
        }
        return fromDocuments(chatResponse.getMetadata().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT));
    }

    public static List<ChatSource> fromDocuments(Object raw) {
        if (!(raw instanceof List<?> documents)) {
            return List.of();
        }
        Map<String, ChatSource> unique = new LinkedHashMap<>();
        for (Object item : documents) {
            if (!(item instanceof Document document)) {
                continue;
            }
            String file = fileName(document);
            if (file == null || file.isBlank()) {
                continue;
            }
            Integer page = pageNumber(document.getMetadata().get("page_number"));
            String key = file + "#" + (page == null ? "" : page);
            unique.putIfAbsent(key, new ChatSource(file, page));
        }
        return List.copyOf(unique.values());
    }

    public static String wire(ChatSource source) {
        String file = source.file() == null ? "" : source.file().replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"file\":\"" + file + "\",\"page\":" + (source.page() == null ? "null" : source.page()) + "}";
    }

    private static String fileName(Document document) {
        Object source = document.getMetadata().get(com.learn.assistant.rag.etl.e.PdfExtract.SOURCE_FILE);
        if (source instanceof String file && !file.isBlank()) {
            return file;
        }
        Object name = document.getMetadata().get("file_name");
        return name instanceof String file ? file : null;
    }

    private static Integer pageNumber(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        if (raw instanceof String text && !text.isBlank()) {
            try {
                return Integer.valueOf(text);
            }
            catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }
}
