package com.learn.assistant.controller;

import com.learn.assistant.chat.ChatMode;
import com.learn.assistant.chat.ChatPiece;
import com.learn.assistant.service.ChatAnswer;
import com.learn.assistant.service.ChatService;
import com.learn.assistant.tool.activity.ToolStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService))
                .setMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new JacksonJsonHttpMessageConverter())
                .build();
    }

    @Test
    void keepsTheBlockingReply() throws Exception {
        when(chatService.reply(eq("你好"), eq("c1"), nullable(ChatMode.class)))
                .thenReturn(new ChatAnswer("完整回答", List.of()));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"你好\",\"conversationId\":\"c1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("完整回答"));
    }

    @Test
    void streamsEachToken() throws Exception {
        when(chatService.stream(eq("你好"), eq("c1"), nullable(ChatMode.class)))
                .thenReturn(Flux.just(ChatPiece.delta("你"), ChatPiece.delta(""), ChatPiece.delta("好")));

        var started = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"你好\",\"conversationId\":\"c1\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult streamed = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();
        String body = streamed.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("data:你"));
        assertTrue(body.contains("data:好"));
        assertEquals(2, body.lines().filter(line -> line.startsWith("data:")).count());
    }

    @Test
    void streamsToolNameArgumentsAndResult() throws Exception {
        when(chatService.stream(eq("下载泰山"), eq("c1"), nullable(ChatMode.class)))
                .thenReturn(Flux.just(ChatPiece.status("搜索"),
                        ChatPiece.tool(new ToolStep("搜索", "query=泰山", "没有搜索到结果")),
                        ChatPiece.delta("没有找到")));

        var started = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"下载泰山\",\"conversationId\":\"c1\",\"mode\":\"task\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult streamed = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn();
        String body = streamed.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("event:status"));
        assertTrue(body.contains("data:搜索"));
        assertTrue(body.contains("event:tool"));
        assertTrue(body.contains("\"name\":\"搜索\""));
        assertTrue(body.contains("\"arguments\":\"query=泰山\""));
        assertTrue(body.contains("\"result\":\"没有搜索到结果\""));
    }

    @Test
    void sendsAnErrorEventWhenStreamingFails() throws Exception {
        when(chatService.stream(eq("你好"), eq("c1"), nullable(ChatMode.class)))
                .thenReturn(Flux.error(new IllegalStateException("模型不可用")));

        var started = mockMvc.perform(post("/api/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"message\":\"你好\",\"conversationId\":\"c1\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult streamed = mockMvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andReturn();
        String body = streamed.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("event:error"));
        assertTrue(body.contains("模型不可用"));
    }
}
