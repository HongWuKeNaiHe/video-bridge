package dev.videobridge.wvp;

import dev.videobridge.common.Http;

import java.util.LinkedHashMap;
import java.util.Map;

/** 默认路径按 wvp-pro 2.7.x 官方接口；不同版本可在 bridge.wvp.endpoints 里按键覆盖 */
public final class WvpApi {

    public static final String LOGIN = "login";
    public static final String DEVICE_LIST = "deviceList";
    public static final String CHANNEL_LIST = "channelList";
    public static final String FETCH_PRESET = "fetchPreset";
    public static final String PLAY_START = "playStart";
    public static final String PLAY_STOP = "playStop";
    public static final String STREAM_START = "streamStart";
    public static final String STREAM_STOP = "streamStop";
    public static final String STREAM_TOUCH = "streamTouch";
    public static final String PTZ = "ptz";
    public static final String PRESET = "preset";
    public static final String HOME_POSITION = "homePosition";
    public static final String RECORD_START = "recordStart";
    public static final String RECORD_STOP = "recordStop";
    public static final String CLOUD_RECORD_LIST = "cloudRecordList";
    public static final String GB_RECORD_QUERY = "gbRecordQuery";
    public static final String PLAYBACK_START = "playbackStart";
    public static final String PLAYBACK_STOP = "playbackStop";
    public static final String PLAYBACK_PAUSE = "playbackPause";
    public static final String PLAYBACK_RESUME = "playbackResume";
    public static final String PLAYBACK_SPEED = "playbackSpeed";
    public static final String PLAYBACK_SEEK = "playbackSeek";
    public static final String DOWNLOAD_START = "downloadStart";
    public static final String DOWNLOAD_STOP = "downloadStop";
    public static final String DOWNLOAD_PROGRESS = "downloadProgress";
    public static final String SNAP = "snap";
    public static final String SNAP_BATCH = "snapBatch";
    public static final String PLAY_PERSISTENT = "playPersistent";

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(LOGIN, "/api/user/login");
        DEFAULTS.put(DEVICE_LIST, "/api/v1/device/list");
        DEFAULTS.put(CHANNEL_LIST, "/api/v1/device/channellist");
        DEFAULTS.put(FETCH_PRESET, "/api/v1/device/fetchpreset");
        DEFAULTS.put(PLAY_START, "/api/play/start/{0}/{1}");
        DEFAULTS.put(PLAY_STOP, "/api/play/stop/{0}/{1}");
        DEFAULTS.put(STREAM_START, "/api/v1/stream/start");
        DEFAULTS.put(STREAM_STOP, "/api/v1/stream/stop");
        DEFAULTS.put(STREAM_TOUCH, "/api/v1/stream/touch");
        DEFAULTS.put(PTZ, "/api/v1/control/ptz");
        DEFAULTS.put(PRESET, "/api/v1/control/preset");
        DEFAULTS.put(HOME_POSITION, "/api/device/control/home_position");
        DEFAULTS.put(RECORD_START, "/api/record/start/{0}/{1}");
        DEFAULTS.put(RECORD_STOP, "/api/record/stop/{0}/{1}");
        DEFAULTS.put(CLOUD_RECORD_LIST, "/api/cloud/record/list");
        DEFAULTS.put(GB_RECORD_QUERY, "/api/gb_record/query/{0}/{1}");
        DEFAULTS.put(PLAYBACK_START, "/api/playback/start/{0}/{1}");
        DEFAULTS.put(PLAYBACK_STOP, "/api/playback/stop/{0}/{1}/{2}");
        DEFAULTS.put(PLAYBACK_PAUSE, "/api/playback/pause/{0}");
        DEFAULTS.put(PLAYBACK_RESUME, "/api/playback/resume/{0}");
        DEFAULTS.put(PLAYBACK_SPEED, "/api/playback/speed/{0}/{1}");
        DEFAULTS.put(PLAYBACK_SEEK, "/api/playback/seek/{0}/{1}");
        DEFAULTS.put(DOWNLOAD_START, "/api/gb_record/download/start/{0}/{1}");
        DEFAULTS.put(DOWNLOAD_STOP, "/api/gb_record/download/stop/{0}/{1}/{2}");
        DEFAULTS.put(DOWNLOAD_PROGRESS, "/api/gb_record/download/progress/{0}/{1}/{2}");
        DEFAULTS.put(SNAP, "/api/play/snap");
        DEFAULTS.put(SNAP_BATCH, "/api/play/snap/batch");
        DEFAULTS.put(PLAY_PERSISTENT, "/api/play/startPersistent/{0}/{1}");
    }

    private final Map<String, String> overrides;

    public WvpApi(Map<String, String> overrides) {
        this.overrides = overrides == null ? Map.of() : overrides;
    }

    public static Map<String, String> defaults() {
        return Map.copyOf(DEFAULTS);
    }

    public String path(String key, Object... args) {
        String template = overrides.getOrDefault(key, DEFAULTS.get(key));
        if (template == null) {
            throw new IllegalArgumentException("未知的 WVP 端点: " + key);
        }
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", Http.encode(String.valueOf(args[i])));
        }
        return template;
    }
}
