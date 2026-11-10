package dev.videobridge.onvif;

import java.util.Locale;

public enum Vendor {
    AUTO,
    HIKVISION,
    DAHUA,
    GENERIC;

    public static Vendor parse(String value) {
        if (value == null || value.isBlank() || "AUTO".equalsIgnoreCase(value)) {
            return AUTO;
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "HIK", "HIKVISION" -> HIKVISION;
            case "DAHUA" -> DAHUA;
            default -> GENERIC;
        };
    }

    /** 厂商字段各家写法不统一，流地址反而更可靠，两个都看一眼 */
    public static Vendor detect(String manufacturer, String streamUri) {
        String m = manufacturer == null ? "" : manufacturer.toLowerCase(Locale.ROOT);
        if (m.contains("hikvision") || m.contains("海康")) {
            return HIKVISION;
        }
        if (m.contains("dahua") || m.contains("大华")) {
            return DAHUA;
        }
        String uri = streamUri == null ? "" : streamUri.toLowerCase(Locale.ROOT);
        if (uri.contains("/streaming/") || uri.contains("hikvision")) {
            return HIKVISION;
        }
        if (uri.contains("/cam/")) {
            return DAHUA;
        }
        return GENERIC;
    }
}
