package dev.videobridge.onvif;

import dev.videobridge.common.BridgeException;
import dev.videobridge.common.Http;
import dev.videobridge.config.BridgeProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OnvifTransport {

    private final HttpClient http;
    private final BridgeProperties props;

    public OnvifTransport(HttpClient http, BridgeProperties props) {
        this.http = http;
        this.props = props;
    }

    public String soap(OnvifDevice device, String path, String xml) {
        return request(device, path, "POST", xml, "application/soap+xml; charset=utf-8");
    }

    public String get(OnvifDevice device, String urlOrPath) {
        return request(device, urlOrPath, "GET", null, null);
    }

    private String request(OnvifDevice device, String urlOrPath, String method,
                           String body, String contentType) {
        String url = urlOrPath.startsWith("http") ? urlOrPath : device.baseUrl() + urlOrPath;
        URI uri = URI.create(url);
        Duration timeout = Duration.ofMillis(props.getOnvif().getRequestTimeoutMs());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "*/*");
        if (body != null) {
            headers.put("Content-Type", contentType);
        }

        Http.Response first = Http.send(http, method, uri, headers, body, timeout);
        if (first.ok()) {
            return first.text();
        }

        String challenge = first.header("WWW-Authenticate");
        // 海康 NVR 未带认证时可能直接回 500，头里的 WWW-Authenticate 却是齐的，只看 401 会漏掉
        if (challenge == null) {
            throw new BridgeException("ONVIF 请求失败 status=" + first.status() + " url=" + safeUrl(url)
                    + " body=" + abbreviate(first.text()));
        }

        String auth;
        if (DigestAuth.isDigest(challenge)) {
            String digestUri = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            auth = DigestAuth.header(challenge, device.username(), device.password(),
                    method, digestUri, DigestAuth.cnonce());
        } else {
            auth = DigestAuth.basic(device.username(), device.password());
        }
        headers.put("Authorization", auth);

        Http.Response second = Http.send(http, method, uri, headers, body, timeout);
        if (!second.ok()) {
            throw new BridgeException("ONVIF 认证后请求失败 status=" + second.status()
                    + " url=" + safeUrl(url) + " body=" + abbreviate(second.text()));
        }
        return second.text();
    }

    private static String safeUrl(String url) {
        int at = url.indexOf('@');
        return at < 0 ? url : url.substring(0, url.indexOf("://") + 3) + "***@" + url.substring(at + 1);
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
