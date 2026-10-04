package com.learn.assistant.controller;

import com.learn.assistant.chat.ChatMode;
import com.learn.assistant.chat.ChatPiece;
import com.learn.assistant.chat.ChatSources;
import com.learn.assistant.domain.dto.ChatRequest;
import com.learn.assistant.domain.vo.ChatResponse;
import com.learn.assistant.chat.ChatAnswer;
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
        ChatMode mode = ChatMode.from(request.mode());
        ChatAnswer answer = chatService.reply(request.message(), request.conversationId(), mode);
        return new ChatResponse(answer.reply(), answer.sources());
    }

    @PostMapping(path = "/stream", produces = EVENT_STREAM)
    public Flux<ServerSentEvent<String>> stream(@Valid @RequestBody ChatRequest request) {
        ChatMode mode = ChatMode.from(request.mode());
        return chatService.stream(request.message(), request.conversationId(), mode)
                .filter(piece -> piece.kind() != ChatPiece.Kind.DELTA || hasText(piece.text()))
                .map(ChatController::event)
                .onErrorResume(error -> {
                    log.warn("流式回答失败", error);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(readable(error))
                            .build());
                });
    }

    private static String readable(Throwable error) {
        String message = "";
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null) {
                message = message + " " + current.getMessage();
            }
            current = current.getCause();
        }
        if (message.contains("Failed to resolve") || message.contains("api.deepseek.com")) {
            return "连不上 DeepSeek：无法解析 api.deepseek.com。请检查网络或 DNS 后重试";
        }
        if (error.getMessage() == null || error.getMessage().isBlank()) {
            return "请求失败";
        }
        return error.getMessage();
    }

    private static ServerSentEvent<String> event(ChatPiece piece) {
        if (piece.kind() == ChatPiece.Kind.STATUS) {
            return ServerSentEvent.<String>builder().event("status").data(piece.text()).build();
        }
        if (piece.kind() == ChatPiece.Kind.SOURCE && piece.source() != null) {
            return ServerSentEvent.<String>builder().event("source").data(ChatSources.wire(piece.source())).build();
        }
        if (piece.kind() == ChatPiece.Kind.TOOL && piece.tool() != null) {
            return ServerSentEvent.<String>builder().event("tool").data(piece.tool().wire()).build();
        }
        return ServerSentEvent.builder(piece.text()).build();
    }

    private static boolean hasText(String text) {
        return text != null && !text.isEmpty();
    }

    @DeleteMapping("/{conversationId}")
    public void delete(@PathVariable String conversationId) {
        chatService.clear(conversationId);
    }
}
