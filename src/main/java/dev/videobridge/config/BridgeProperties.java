package dev.videobridge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "bridge")
public class BridgeProperties {

    private Wvp wvp = new Wvp();
    private Zlm zlm = new Zlm();
    private Onvif onvif = new Onvif();
    private Record record = new Record();
    private State state = new State();
    private KeepAlive keepAlive = new KeepAlive();

    @Data
    public static class Wvp {
        private boolean enabled = true;
        private String baseUrl = "http://127.0.0.1:18978";
        private String username = "admin";
        private String password = "";
        private int tokenTtlSeconds = 1200;
        private int requestTimeoutMs = 10000;
        private boolean forkExtensions = false;
        /** wvp 和网关共用同一个 ZLM 时，用网关配置的对外地址重写播放 URL */
        private boolean shareZlm = true;
        /** 键名见 WvpApi，用来适配不同 wvp 版本的路径差异 */
        private Map<String, String> endpoints = new HashMap<>();
    }

    @Data
    public static class Zlm {
        /** 网关自己访问 ZLM 的地址 */
        private String apiHost = "http://127.0.0.1";
        private int apiPort = 9098;
        private String secret = "";
        private String vhost = "__defaultVhost__";
        private String app = "live";
        /** 发给播放器的地址，公网部署时通常和 apiHost 不是同一个 */
        private String playHost = "127.0.0.1";
        private int playHttpPort = 8080;
        private int playRtspPort = 554;
    }

    @Data
    public static class Onvif {
        private int requestTimeoutMs = 10000;
        private int defaultPort = 80;
        private int rtspPort = 554;
    }

    @Data
    public static class Record {
        private int maxDurationSeconds = 900;
        private int downloadBufferSeconds = 30;
        private int keysRetentionDays = 7;
        /** 国标回放把起始时间往前挪几秒，吃掉 GOP/I 帧对齐造成的开头缺失 */
        private int playbackPreRollSeconds = 4;
    }

    @Data
    public static class State {
        /** memory 或 redis */
        private String type = "memory";
        private String keyPrefix = "video-bridge:";
    }

    @Data
    public static class KeepAlive {
        private int intervalSeconds = 60;
    }
}
