package com.learn.assistant.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectiveQueryTransformerTest {

    @Test
    void transformsWhenConditionMatches() {
        QueryTransformer transformer = SelectiveQueryTransformer.when(
                query -> query.mutate().text("改写后").build(),
                query -> query.text().length() > 2);

        assertEquals("改写后", transformer.transform(new Query("较长问题")).text());
    }

    @Test
    void keepsOriginalWhenConditionDoesNotMatch() {
        QueryTransformer transformer = SelectiveQueryTransformer.when(
                query -> query.mutate().text("改写后").build(),
                query -> query.text().length() > 2);

        assertEquals("短", transformer.transform(new Query("短")).text());
    }
}
