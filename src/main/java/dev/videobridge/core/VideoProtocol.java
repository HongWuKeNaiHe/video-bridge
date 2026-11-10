package dev.videobridge.core;

import java.util.Locale;

public enum VideoProtocol {
    ONVIF,
    GB28181;

    public static VideoProtocol parse(String value) {
        if (value == null || value.isBlank()) {
            return GB28181;
        }
        String normalized = value.toUpperCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "ONVIF" -> ONVIF;
            case "GB28181", "WVP", "GB" -> GB28181;
            default -> throw new IllegalArgumentException("未知视频协议: " + value);
        };
    }
}
