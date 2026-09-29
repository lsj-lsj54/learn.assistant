package com.learn.assistant.rag;

import com.learn.assistant.properties.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class BuiltinQueryTransformersTest {

    @Test
    void skipsEveryTransformerWhenTheQuestionIsShort() {
        RagProperties properties = new RagProperties();
        properties.getRetrieval().setRewriteMinLength(24);
        ChatModel model = mock(ChatModel.class);
        Query query = Query.builder().text("hi").history(List.of(new UserMessage("上一句"))).build();

        for (QueryTransformerContributor contributor : List.of(
                BuiltinQueryTransformers.compression(),
                BuiltinQueryTransformers.translation(),
                BuiltinQueryTransformers.rewrite())) {
            Query result = contributor.contribute(
                    org.springframework.ai.chat.client.ChatClient.builder(model), properties).transform(query);
            assertEquals("hi", result.text());
        }
        verifyNoInteractions(model);
    }
}
