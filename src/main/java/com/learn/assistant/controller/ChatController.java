package com.learn.assistant.controller;

import com.learn.assistant.domain.dto.ChatRequest;
import com.learn.assistant.domain.vo.ChatResponse;
import com.learn.assistant.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String EVENT_STREAM = "text/event-stream;charset=UTF-8";

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(chatService.reply(request.message(), request.conversationId()));
    }

    @PostMapping(path = "/stream", produces = EVENT_STREAM)
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest request) {
        return chatService.stream(request.message(), request.conversationId())
                .filter(token -> token != null && !token.isEmpty())
                .map(token -> ServerSentEvent.builder(token).build())
                .onErrorResume(error -> {
                    log.warn("流式回答失败", error);
                    String message = error.getMessage();
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(message == null || message.isBlank() ? "请求失败" : message)
                            .build());
                });
    }

    @DeleteMapping("/{conversationId}")
    public void delete(@PathVariable String conversationId) {
        chatService.clear(conversationId);
    }
}
