package com.learn.assistant.rag;

import org.springframework.ai.document.Document;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 切片正文的 SHA-256。向量表主键是 uuid，所以用这段哈希生成稳定 id：
 * 同一段文字再次写入会落到同一行。
 */
final class ChunkFingerprint {

    static final String METADATA_KEY = "chunkHash";

    private ChunkFingerprint() {
    }

    static String hash(String text) {
        return hex(digest(text));
    }

    static Document stamp(Document chunk) {
        String text = chunk.getText();
        byte[] digest = digest(text);
        Map<String, Object> metadata = new HashMap<>();
        chunk.getMetadata().forEach((key, value) -> {
            if (key != null && value != null) {
                metadata.put(key, value);
            }
        });
        metadata.put(METADATA_KEY, hex(digest));
        return Document.builder()
                .id(UUID.nameUUIDFromBytes(digest).toString())
                .text(text)
                .metadata(metadata)
                .build();
    }

    private static byte[] digest(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String hex(byte[] digest) {
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            builder.append(Character.forDigit((value >> 4) & 0xf, 16));
            builder.append(Character.forDigit(value & 0xf, 16));
        }
        return builder.toString();
    }
}
