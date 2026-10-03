package com.learn.assistant.tool.config;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AssistantToolCatalog {

    private final List<Object> tools;

    public AssistantToolCatalog(ApplicationContext context) {
        Map<String, Object> found = context.getBeansWithAnnotation(AssistantTool.class);
        List<Object> ordered = new ArrayList<>(found.values());
        AnnotationAwareOrderComparator.sort(ordered);
        this.tools = List.copyOf(ordered);
    }

    public Object[] toArray() {
        return tools.toArray();
    }
}
