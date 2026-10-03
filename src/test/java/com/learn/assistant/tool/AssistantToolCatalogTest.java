package com.learn.assistant.tool;

import com.learn.assistant.properties.ToolProperties;
import com.learn.assistant.tool.concretetool.PdfWriteTool;
import com.learn.assistant.tool.concretetool.WebSearchTool;
import com.learn.assistant.tool.config.AssistantTool;
import com.learn.assistant.tool.config.AssistantToolCatalog;
import com.learn.assistant.tool.path.ProjectPaths;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantToolCatalogTest {

    @Test
    void sortsToolsByOrder() {
        PdfWriteTool pdf = new PdfWriteTool(new ProjectPaths("src/main/resources/pdf", "res"), new ToolProperties());
        WebSearchTool search = new WebSearchTool(new ToolProperties());
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBeansWithAnnotation(AssistantTool.class)).thenReturn(Map.of(
                "search", search,
                "pdf", pdf));

        Object[] tools = new AssistantToolCatalog(context).toArray();

        assertSame(pdf, tools[0]);
        assertSame(search, tools[1]);
    }
}
