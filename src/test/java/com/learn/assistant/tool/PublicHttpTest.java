package com.learn.assistant.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicHttpTest {

    private final PublicHttp publicHttp = new PublicHttp();

    @Test
    void rejectsNonHttpScheme() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publicHttp.checkPublicHttp("file:///C:/secret.txt"));

        assertTrueMessage(exception, "只允许 http 或 https 地址");
    }

    @Test
    void rejectsLoopback() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publicHttp.checkPublicHttp("http://127.0.0.1/a.png"));

        assertTrueMessage(exception, "不允许访问内网地址");
    }

    @Test
    void rejectsPrivateNetwork() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> publicHttp.checkPublicHttp("http://192.168.1.8/a.png"));

        assertTrueMessage(exception, "不允许访问内网地址");
    }

    @Test
    void allowsPublicHost() {
        assertDoesNotThrow(() -> publicHttp.checkPublicHttp("https://example.com/index.html"));
    }

    private static void assertTrueMessage(IllegalArgumentException exception, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(message, exception.getMessage());
    }
}
