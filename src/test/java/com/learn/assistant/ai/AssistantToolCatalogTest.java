package com.learn.assistant.ai;

import com.learn.assistant.tool.AssistantTool;
import com.learn.assistant.tool.PdfWriteTool;
import com.learn.assistant.tool.ProjectPaths;
import com.learn.assistant.tool.WebSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantToolCatalogTest {

    @Test
    void sortsToolsByOrder() {
        PdfWriteTool pdf = new PdfWriteTool(new ProjectPaths());
        WebSearchTool search = new WebSearchTool();
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansWithAnnotation(AssistantTool.class)).thenReturn(Map.of(
                "search", search,
                "pdf", pdf));

        Object[] tools = new AssistantToolCatalog(context).toArray();

        assertSame(pdf, tools[0]);
        assertSame(search, tools[1]);
    }
}
