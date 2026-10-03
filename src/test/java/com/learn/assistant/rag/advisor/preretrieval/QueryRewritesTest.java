package com.learn.assistant.rag.advisor.preretrieval;

import com.learn.assistant.properties.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class QueryRewritesTest {

    @Test
    void decoratesCompressionThenTranslationThenRewrite() {
        QueryTransformer chain = chain(new RagProperties());

        assertInstanceOf(CompressionDecorator.class, chain);
        QueryTransformer translation = ((QueryTransformerDecorator) chain).next();
        assertInstanceOf(TranslationDecorator.class, translation);
        QueryTransformer rewrite = ((QueryTransformerDecorator) translation).next();
        assertInstanceOf(RewriteDecorator.class, rewrite);
        assertInstanceOf(IdentityQueryTransformer.class, ((QueryTransformerDecorator) rewrite).next());
    }

    @Test
    void skipsEveryRewriteWhenTheQuestionIsShort() {
        RagProperties properties = new RagProperties();
        properties.getRetrieval().setRewriteMinLength(24);
        ChatModel model = mock(ChatModel.class);
        Query query = Query.builder().text("hi").history(List.of(new UserMessage("上一句"))).build();

        Query result = QueryRewrites.chain(ChatClient.builder(model), properties).transform(query);

        assertEquals("hi", result.text());
        verifyNoInteractions(model);
    }

    private static QueryTransformer chain(RagProperties properties) {
        return QueryRewrites.chain(ChatClient.builder(mock(ChatModel.class)), properties);
    }
}
