package dev.videobridge.common;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Http {

    private Http() {
    }

    public static Response send(HttpClient client, String method, URI uri,
                                Map<String, String> headers, String body, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(timeout);
        if (headers != null) {
            headers.forEach(builder::header);
        }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        }
        try {
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Response(response.statusCode(), response.headers(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BridgeException("HTTP 请求被中断: " + uri, e);
        } catch (IOException e) {
            throw new BridgeException("HTTP 请求失败: " + uri + " -> " + e.getMessage(), e);
        }
    }

    public static String form(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (v == null) {
                return;
            }
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(encode(k)).append('=').append(encode(v));
        });
        return sb.toString();
    }

    public static Map<String, String> formOf(Object... kv) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object value = kv[i + 1];
            if (value != null) {
                map.put(String.valueOf(kv[i]), String.valueOf(value));
            }
        }
        return map;
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static final class Response {
        private final int status;
        private final HttpHeaders headers;
        private final byte[] body;

        Response(int status, HttpHeaders headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        public int status() {
            return status;
        }

        public boolean ok() {
            return status >= 200 && status < 300;
        }

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        public String text() {
            return body == null ? "" : new String(body, StandardCharsets.UTF_8);
        }

        public byte[] bytes() {
            return body == null ? new byte[0] : body;
        }
    }
}
