package dev.videobridge.zlm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.videobridge.common.BridgeException;
import dev.videobridge.common.Http;
import dev.videobridge.config.BridgeProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class ZlmClient {

    private final HttpClient http;
    private final BridgeProperties props;
    private final ObjectMapper mapper;

    public ZlmClient(HttpClient http, BridgeProperties props, ObjectMapper mapper) {
        this.http = http;
        this.props = props;
        this.mapper = mapper;
    }

    public enum Mode {
        LIVE("1", "0"),
        PERMANENT("0", "0"),
        RECORD("1", "1"),
        REPLAY("1", "0"),
        DOWNLOAD("0", "1");

        private final String autoClose;
        private final String mp4;

        Mode(String autoClose, String mp4) {
            this.autoClose = autoClose;
            this.mp4 = mp4;
        }
    }

    public PlayUrls addProxy(String rtspUrl, String streamKey, Mode mode) {
        BridgeProperties.Zlm zlm = props.getZlm();
        Map<String, String> form = Http.formOf(
                "secret", zlm.getSecret(), "vhost", zlm.getVhost(), "app", zlm.getApp(),
                "stream", streamKey, "url", rtspUrl,
                "rtp_type", "0", "auto_close", mode.autoClose, "enable_mp4", mode.mp4,
                "enable_rtsp", "1", "enable_rtmp", "1", "enable_hls", "0",
                "enable_ts", "0", "enable_fmp4", "0", "enable_rtc", "1");
        if (zlm.getSecret().isBlank()) {
            form.remove("secret");
        }

        Http.Response response = Http.send(http, "POST", URI.create(apiUrl("addStreamProxy")),
                Map.of("Content-Type", "application/x-www-form-urlencoded"), Http.form(form), timeout());
        checkCode(response, "addStreamProxy");
        return urls(streamKey);
    }

    public boolean closeStream(String streamKey) {
        Map<String, String> form = Http.formOf("secret", props.getZlm().getSecret(),
                "vhost", props.getZlm().getVhost(), "app", props.getZlm().getApp(),
                "stream", streamKey, "force", "1");
        if (props.getZlm().getSecret().isBlank()) {
            form.remove("secret");
        }
        Http.Response response = Http.send(http, "POST", URI.create(apiUrl("close_streams")),
                Map.of("Content-Type", "application/x-www-form-urlencoded"), Http.form(form), timeout());
        return response.ok() && codeOf(response) == 0;
    }

    public PlayUrls urls(String streamKey) {
        return urls(streamKey, props.getZlm().getApp());
    }

    /** 国标流在 ZLM 里的 app 通常是 rtp，和 ONVIF 的 app 不是同一个 */
    public PlayUrls urls(String streamKey, String app) {
        BridgeProperties.Zlm zlm = props.getZlm();
        String host = hostOf(zlm.getPlayHost());
        int port = zlm.getPlayHttpPort();
        return new PlayUrls(streamKey,
                "http://" + host + ":" + port + "/index/api/webrtc?app=" + app + "&stream=" + streamKey + "&type=play",
                "rtsp://" + host + ":" + zlm.getPlayRtspPort() + "/" + app + "/" + streamKey,
                "ws://" + host + ":" + port + "/" + app + "/" + streamKey + ".flv",
                "http://" + host + ":" + port + "/" + app + "/" + streamKey + ".live.flv");
    }

    public JsonNode mediaList(String app, String stream) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl("getMediaList"))
                .queryParam("app", app == null ? props.getZlm().getApp() : app)
                .queryParam("vhost", props.getZlm().getVhost());
        if (stream != null) {
            builder.queryParam("stream", stream);
        }
        if (!props.getZlm().getSecret().isBlank()) {
            builder.queryParam("secret", props.getZlm().getSecret());
        }
        Http.Response response = Http.send(http, "GET", builder.build().encode().toUri(), null, null, timeout());
        return checkCode(response, "getMediaList");
    }

    public boolean isStreamOnline(String streamKey) {
        return isStreamOnline(streamKey, props.getZlm().getApp());
    }

    public boolean isStreamOnline(String streamKey, String app) {
        JsonNode json = mediaList(app, streamKey);
        JsonNode data = json == null ? null : json.path("data");
        return data != null && data.isArray() && !data.isEmpty();
    }

    public int readerCount(String streamKey, String app) {
        JsonNode json = mediaList(app, streamKey);
        JsonNode data = json == null ? null : json.path("data");
        if (data == null || !data.isArray()) {
            return 0;
        }
        // 一条流会按 schema 出现多条记录，totalReaderCount 相同，取最大值兜底
        int max = 0;
        for (JsonNode item : data) {
            if (!streamKey.equals(item.path("stream").asText())) {
                continue;
            }
            max = Math.max(max, item.path("totalReaderCount").asInt(0));
        }
        return max;
    }

    public byte[] snap(String streamKey) {
        return snap(streamKey, props.getZlm().getApp());
    }

    public byte[] snap(String streamKey, String app) {
        BridgeProperties.Zlm zlm = props.getZlm();
        // getSnap 是 ZLM 自己按这个 RTSP 去拉流截帧，必须用 ZLM 自身可达的地址，
        // 换成发给播放器的公网地址会拉不到流，最后只回一张默认图
        String rtsp = "rtsp://" + hostOf(zlm.getApiHost()) + ":" + zlm.getPlayRtspPort()
                + "/" + app + "/" + streamKey;
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl("getSnap"))
                .queryParam("url", rtsp)
                .queryParam("timeout_sec", 10)
                .queryParam("expire_sec", 5);
        if (!zlm.getSecret().isBlank()) {
            builder.queryParam("secret", zlm.getSecret());
        }
        Http.Response response = Http.send(http, "GET", builder.build().encode().toUri(), null, null, timeout());
        if (!response.ok() || response.bytes().length == 0) {
            throw new BridgeException("ZLM 截图失败 stream=" + streamKey);
        }
        return response.bytes();
    }

    public JsonNode mp4RecordFile(String app, String stream, String period) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(apiUrl("getMp4RecordFile"))
                .queryParam("app", app == null ? props.getZlm().getApp() : app)
                .queryParam("stream", stream)
                .queryParam("period", period)
                .queryParam("vhost", props.getZlm().getVhost());
        if (!props.getZlm().getSecret().isBlank()) {
            builder.queryParam("secret", props.getZlm().getSecret());
        }
        Http.Response response = Http.send(http, "GET", builder.build().encode().toUri(), null, null, timeout());
        return checkCode(response, "getMp4RecordFile");
    }

    public String recordFileUrl(String app, String stream, String date, String fileName) {
        BridgeProperties.Zlm zlm = props.getZlm();
        return "http://" + hostOf(zlm.getPlayHost()) + ":" + zlm.getPlayHttpPort()
                + "/record/" + app + "/" + stream + "/" + date + "/" + fileName;
    }

    private JsonNode checkCode(Http.Response response, String api) {
        if (!response.ok()) {
            throw new BridgeException("ZLM " + api + " 请求失败 status=" + response.status());
        }
        try {
            JsonNode json = mapper.readTree(response.text());
            int code = json.path("code").asInt(-1);
            if (code != 0) {
                throw new BridgeException("ZLM " + api + " 返回 code=" + code + " msg=" + json.path("msg").asText(""));
            }
            return json;
        } catch (BridgeException e) {
            throw e;
        } catch (Exception e) {
            throw new BridgeException("ZLM " + api + " 响应解析失败: " + response.text(), e);
        }
    }

    private int codeOf(Http.Response response) {
        if (!response.ok() || response.text().isBlank()) {
            return -1;
        }
        try {
            return mapper.readTree(response.text()).path("code").asInt(-1);
        } catch (Exception e) {
            return -1;
        }
    }

    private String apiUrl(String path) {
        return trimSlash(props.getZlm().getApiHost()) + ":" + props.getZlm().getApiPort() + "/index/api/" + path;
    }

    private Duration timeout() {
        return Duration.ofMillis(Math.max(10000, props.getOnvif().getRequestTimeoutMs()));
    }

    private static String hostOf(String value) {
        return value.replaceFirst("^https?://", "").replaceAll("/+$", "");
    }

    private static String trimSlash(String value) {
        return value.replaceAll("/+$", "");
    }
}
