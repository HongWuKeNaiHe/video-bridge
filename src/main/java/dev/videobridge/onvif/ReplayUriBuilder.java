package dev.videobridge.onvif;

import dev.videobridge.common.Http;

public final class ReplayUriBuilder {

    private ReplayUriBuilder() {
    }

    /**
     * 标准 ReplayControl 在很多 NVR 上要么不支持、要么返回的 URI 拉不出来，
     * 现场主要靠按厂商格式拼 RTSP 回放地址，标准接口只作为兜底。
     */
    public static String build(Vendor vendor, String host, int rtspPort, String username, String password,
                               int channelNo, String startTime, String endTime) {
        int ch = Math.max(1, channelNo);
        String start = compact(startTime);
        String end = compact(endTime);
        String base = "rtsp://" + Http.encode(username) + ":" + Http.encode(password) + "@"
                + host + ":" + rtspPort;

        if (vendor == Vendor.DAHUA) {
            return base + "/cam/playback?channel=" + (ch - 1)
                    + "&subtype=0&starttime=" + start + "&endtime=" + end;
        }
        // 海康和未识别的厂商都按 tracks 格式试，绝大多数 NVR 认这个
        return base + "/Streaming/tracks/" + ch + "01?starttime=" + start + "&endtime=" + end;
    }

    /** yyyy-MM-dd HH:mm:ss / yyyyMMddHHmmss -> yyyy-MM-ddTHH:mm:ssZ */
    public static String standard(String time) {
        String digits = digits(time);
        if (digits.length() < 14) {
            return null;
        }
        return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8)
                + "T" + digits.substring(8, 10) + ":" + digits.substring(10, 12)
                + ":" + digits.substring(12, 14) + "Z";
    }

    /** yyyy-MM-dd HH:mm:ss / yyyyMMddHHmmss -> yyyyMMddTHHmmssZ */
    public static String compact(String time) {
        String digits = digits(time);
        if (digits.length() < 14) {
            return "";
        }
        return digits.substring(0, 8) + "T" + digits.substring(8, 14) + "Z";
    }

    private static String digits(String time) {
        return time == null ? "" : time.replaceAll("[^0-9]", "");
    }
}
