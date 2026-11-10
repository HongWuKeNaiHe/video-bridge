package dev.videobridge.wvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.videobridge.common.BridgeException;
import dev.videobridge.common.Http;
import dev.videobridge.config.BridgeProperties;
import dev.videobridge.onvif.DigestAuth;
import dev.videobridge.storage.StateStore;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WvpClient {

    private static final String TOKEN_KEY = "wvp:token";

    private final HttpClient http;
    private final BridgeProperties props;
    private final ObjectMapper mapper;
    private final StateStore state;
    private final WvpApi api;

    public WvpClient(HttpClient http, BridgeProperties props, ObjectMapper mapper, StateStore state) {
        this.http = http;
        this.props = props;
        this.mapper = mapper;
        this.state = state;
        this.api = new WvpApi(props.getWvp().getEndpoints());
    }

    /** 完整响应；控制类接口可能只回空体，此时返回 null */
    public JsonNode get(String endpoint, Map<String, Object> query, Object... pathArgs) {
        requireEnabled();
        String token = token();
        URI uri = uri(endpoint, query, pathArgs);
        Http.Response response = Http.send(http, "GET", uri, Map.of("access-token", token), null, timeout());
        if (response.status() == 401) {
            // token 被服务端提前踢掉了，重新登录再发一次
            state.delete(TOKEN_KEY);
            response = Http.send(http, "GET", uri, Map.of("access-token", token()), null, timeout());
        }
        return parse(response, uri);
    }

    public JsonNode data(String endpoint, Map<String, Object> query, Object... pathArgs) {
        JsonNode json = get(endpoint, query, pathArgs);
        return json == null ? null : json.path("data");
    }

    public static Map<String, Object> params(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null) {
                map.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return map;
    }

    public void clearToken() {
        state.delete(TOKEN_KEY);
    }

    private String token() {
        String cached = state.get(TOKEN_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        // wvp 登录接口收的是密码 MD5，不是明文
        URI uri = UriComponentsBuilder.fromUriString(props.getWvp().getBaseUrl())
                .path(api.path(WvpApi.LOGIN))
                .queryParam("username", props.getWvp().getUsername())
                .queryParam("password", DigestAuth.md5(props.getWvp().getPassword()))
                .build().encode().toUri();
        Http.Response response = Http.send(http, "GET", uri, null, null, timeout());
        JsonNode json = parse(response, uri);
        String accessToken = json == null ? null : json.path("data").path("accessToken").asText(null);
        if (accessToken == null || accessToken.isBlank()) {
            throw new BridgeException("WVP 登录没有返回 accessToken");
        }
        long ttl = Math.max(60, props.getWvp().getTokenTtlSeconds() - 30L);
        state.set(TOKEN_KEY, accessToken, Duration.ofSeconds(ttl));
        return accessToken;
    }

    private JsonNode parse(Http.Response response, URI uri) {
        if (!response.ok()) {
            throw new BridgeException("WVP 请求失败 status=" + response.status() + " url=" + uri
                    + " body=" + response.text());
        }
        String body = response.text();
        if (body.isBlank()) {
            return null;
        }
        try {
            JsonNode json = mapper.readTree(body);
            if (json.hasNonNull("code") && json.path("code").asInt() != 0) {
                throw new BridgeException("WVP 返回错误 code=" + json.path("code").asInt()
                        + " msg=" + json.path("msg").asText(""));
            }
            return json;
        } catch (BridgeException e) {
            throw e;
        } catch (Exception e) {
            throw new BridgeException("WVP 响应解析失败: " + body, e);
        }
    }

    private URI uri(String endpoint, Map<String, Object> query, Object... pathArgs) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(props.getWvp().getBaseUrl())
                .path(api.path(endpoint, pathArgs));
        if (query != null) {
            query.forEach((k, v) -> {
                if (v != null) {
                    builder.queryParam(k, v);
                }
            });
        }
        return builder.build().encode().toUri();
    }

    private Duration timeout() {
        return Duration.ofMillis(props.getWvp().getRequestTimeoutMs());
    }

    private void requireEnabled() {
        if (!props.getWvp().isEnabled()) {
            throw new BridgeException("WVP 未启用，请在 bridge.wvp.enabled 打开");
        }
    }
}
