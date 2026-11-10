package dev.videobridge.core;

import dev.videobridge.zlm.PlayUrls;

import java.util.Map;

public record StreamTicket(String streamKey, String webrtc, String rtsp, String wsFlv,
                           String httpFlv, Map<String, Object> raw) {

    public static StreamTicket of(PlayUrls urls) {
        return of(urls, null);
    }

    public static StreamTicket of(PlayUrls urls, Map<String, Object> raw) {
        return new StreamTicket(urls.streamKey(), urls.webrtc(), urls.rtsp(),
                urls.wsFlv(), urls.httpFlv(), raw);
    }
}
