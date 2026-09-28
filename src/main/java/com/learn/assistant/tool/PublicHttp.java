package com.learn.assistant.tool;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

@Component
public class PublicHttp {

    public void checkPublicHttp(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("只允许 http 或 https 地址");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("地址缺少主机名");
        }
        try {
            InetAddress address = InetAddress.getByName(uri.getHost());
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
                throw new IllegalArgumentException("不允许访问内网地址");
            }
        }
        catch (IllegalArgumentException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("无法解析地址: " + exception.getMessage());
        }
    }
}
