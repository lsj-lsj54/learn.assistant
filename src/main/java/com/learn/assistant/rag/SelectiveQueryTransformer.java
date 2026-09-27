package com.learn.assistant.rag;

import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;

import java.util.function.Predicate;

class SelectiveQueryTransformer implements QueryTransformer {

    private final QueryTransformer delegate;

    private final Predicate<Query> when;

    private SelectiveQueryTransformer(QueryTransformer delegate, Predicate<Query> when) {
        this.delegate = delegate;
        this.when = when;
    }

    static QueryTransformer when(QueryTransformer delegate, Predicate<Query> when) {
        return new SelectiveQueryTransformer(delegate, when);
    }

    @Override
    public Query transform(Query query) {
        if (!when.test(query)) {
            return query;
        }
        return delegate.transform(query);
    }
}
