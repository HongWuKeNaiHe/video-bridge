package dev.videobridge.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.videobridge.common.BridgeException;
import dev.videobridge.common.Times;
import dev.videobridge.config.BridgeProperties;
import dev.videobridge.onvif.OnvifClient;
import dev.videobridge.onvif.ReplayUriBuilder;
import dev.videobridge.onvif.Vendor;
import dev.videobridge.storage.Channel;
import dev.videobridge.storage.StateStore;
import dev.videobridge.wvp.WvpApi;
import dev.videobridge.wvp.WvpClient;
import dev.videobridge.zlm.PlayUrls;
import dev.videobridge.zlm.ZlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class PlaybackService {

    private static final Logger log = LoggerFactory.getLogger(PlaybackService.class);
    private static final Duration TASK_TTL = Duration.ofDays(2);

    private final ChannelRouter router;
    private final ZlmClient zlm;
    private final WvpClient wvp;
    private final OnvifClient onvif;
    private final StateStore state;
    private final ObjectMapper mapper;
    private final BridgeProperties props;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> closeTasks = new ConcurrentHashMap<>();

    public PlaybackService(ChannelRouter router, ZlmClient zlm, WvpClient wvp, OnvifClient onvif,
                           StateStore state, ObjectMapper mapper, BridgeProperties props,
                           ScheduledExecutorService scheduler) {
        this.router = router;
        this.zlm = zlm;
        this.wvp = wvp;
        this.onvif = onvif;
        this.state = state;
        this.mapper = mapper;
        this.props = props;
        this.scheduler = scheduler;
    }

    public StreamTicket start(String channelKey, String startTime, String endTime, boolean download) {
        Channel channel = router.require(channelKey);
        Times.parse(startTime);
        Times.parse(endTime);
        return channel.protocol() == VideoProtocol.ONVIF
                ? startOnvif(channel, startTime, endTime, download)
                : startGb(channel, startTime, endTime, download);
    }

    private StreamTicket startOnvif(Channel channel, String startTime, String endTime, boolean download) {
        Vendor vendor = Vendor.parse(channel.vendor());
        if (vendor == Vendor.AUTO) {
            String manufacturer = onvif.deviceInfo(channel.onvifDevice()).manufacturer();
            vendor = Vendor.detect(manufacturer, onvif.streamUri(channel.onvifDevice(), null));
        }
        String rtsp = ReplayUriBuilder.build(vendor, channel.nvrHostOrSelf(),
                props.getOnvif().getRtspPort(), channel.onvifUsername(), channel.onvifPassword(),
                channel.channelNo() == null ? 1 : channel.channelNo(), startTime, endTime);

        // 下载流用 record_ 前缀，录像列表才能按前缀捞到生成的 MP4
        String streamKey = (download ? "record_" : "pb_") + channel.channelKey() + "_"
                + System.currentTimeMillis();
        PlayUrls urls;
        try {
            urls = zlm.addProxy(rtsp, streamKey,
                    download ? ZlmClient.Mode.DOWNLOAD : ZlmClient.Mode.REPLAY);
        } catch (BridgeException e) {
            String standard = onvif.standardReplayUri(channel.onvifDevice(), null, startTime, endTime);
            urls = zlm.addProxy(standard, streamKey,
                    download ? ZlmClient.Mode.DOWNLOAD : ZlmClient.Mode.REPLAY);
        }

        state.set("playback:" + streamKey,
                writeTask(Map.of("protocol", "ONVIF", "download", download)), TASK_TTL);
        if (download) {
            scheduleClose(channel, streamKey, startTime, endTime);
        }
        return StreamTicket.of(urls);
    }

    private StreamTicket startGb(Channel channel, String startTime, String endTime, boolean download) {
        // 起始时间往前挪几秒，否则 NVR 的 GOP 对齐会把开头几秒吃掉
        String adjusted = Times.format(Times.parse(startTime)
                .minusSeconds(props.getRecord().getPlaybackPreRollSeconds()));
        String endpoint = download ? WvpApi.DOWNLOAD_START : WvpApi.PLAYBACK_START;
        JsonNode data = wvp.data(endpoint,
                WvpClient.params("startTime", adjusted, "endTime", endTime),
                channel.gbDeviceId(), channel.gbChannelId());
        if (data == null || data.isMissingNode()) {
            throw new BridgeException("wvp 没有返回回放数据: " + channel.channelKey());
        }
        String stream = data.path("stream").asText(channel.streamKey());
        String app = data.path("app").asText("rtp");
        PlayUrls urls = props.getWvp().isShareZlm()
                ? zlm.urls(stream, app)
                : new PlayUrls(stream, data.path("rtc").asText(null), data.path("rtsp").asText(null),
                data.path("ws_flv").asText(null), data.path("flv").asText(null));
        state.set("playback:" + stream,
                writeTask(Map.of("protocol", "GB28181", "channelKey", channel.channelKey(),
                        "download", download)), TASK_TTL);
        Map<String, Object> raw = mapper.convertValue(data, new TypeReference<>() {
        });
        return StreamTicket.of(urls, raw == null ? Map.of() : raw);
    }

    public void stop(String streamKey) {
        ScheduledFuture<?> task = closeTasks.remove(streamKey);
        if (task != null) {
            task.cancel(false);
        }
        String protocol = protocolOf(streamKey);
        if (VideoProtocol.ONVIF.name().equals(protocol)) {
            zlm.closeStream(streamKey);
            return;
        }
        String channelKey = channelKeyOf(streamKey);
        Channel channel = router.require(channelKey);
        String endpoint = downloadOf(streamKey) ? WvpApi.DOWNLOAD_STOP : WvpApi.PLAYBACK_STOP;
        wvp.data(endpoint, null, channel.gbDeviceId(), channel.gbChannelId(), streamKey);
        state.set("download:" + streamKey, "STOPPED", TASK_TTL);
    }

    public Map<String, Object> progress(String streamKey) {
        String value = state.get("download:" + streamKey);
        if (value == null) {
            String protocol = protocolOf(streamKey);
            if (VideoProtocol.GB28181.name().equals(protocol)) {
                Channel channel = router.require(channelKeyOf(streamKey));
                JsonNode data = wvp.data(WvpApi.DOWNLOAD_PROGRESS, null,
                        channel.gbDeviceId(), channel.gbChannelId(), streamKey);
                Map<String, Object> payload = data == null
                        ? Map.of() : mapper.convertValue(data, new TypeReference<>() {
                });
                return Map.of("source", "wvp", "data", payload == null ? Map.of() : payload);
            }
            throw new BridgeException("未找到下载任务: " + streamKey);
        }

        String[] parts = value.split("\\|");
        String status = parts[0];
        long startEpoch = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
        long expected = parts.length > 2 ? Long.parseLong(parts[2]) : 0;
        long elapsed = System.currentTimeMillis() / 1000 - startEpoch;

        if ("CLOSING".equals(status)) {
            String done = tryMarkDownloaded(streamKey, startEpoch, expected);
            if (done != null) {
                status = done;
                state.set("download:" + streamKey,
                        status + "|" + startEpoch + "|" + expected, TASK_TTL);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("streamKey", streamKey);
        result.put("status", status);
        if ("DOWNLOADING".equals(status) || "CLOSING".equals(status)) {
            int percent = expected <= 0 ? 0 : (int) Math.min(99, Math.max(0, elapsed * 100 / expected));
            result.put("progress", percent);
            result.put("elapsedSeconds", elapsed);
            result.put("expectedSeconds", expected);
            result.put("remainingSeconds", Math.max(0, expected - elapsed));
        } else {
            result.put("progress", "STOPPED".equals(status) ? 0 : 100);
        }
        return result;
    }

    public JsonNode control(String streamKey, String type, Long seek, Double speed) {
        String protocol = protocolOf(streamKey);
        if (!VideoProtocol.GB28181.name().equals(protocol)) {
            // ONVIF 回放只是 ZLM 转了一条 RTSP，没有国标那套回放控制信令
            throw new BridgeException("ONVIF 回放不支持暂停/恢复/倍速");
        }
        Channel channel = router.require(channelKeyOf(streamKey));
        return switch (type) {
            case "pause" -> wvp.data(WvpApi.PLAYBACK_PAUSE, null, streamKey);
            case "resume" -> wvp.data(WvpApi.PLAYBACK_RESUME, null, streamKey);
            case "speed" -> wvp.data(WvpApi.PLAYBACK_SPEED,
                    null, streamKey, speed == null ? 1 : speed);
            case "seek" -> wvp.data(WvpApi.PLAYBACK_SEEK, null, streamKey, seek == null ? 0 : seek);
            case "stop" -> {
                stop(streamKey);
                yield null;
            }
            default -> throw new IllegalArgumentException("不支持的回放控制: " + type);
        };
    }

    public JsonNode gbRecords(String channelKey, String startTime, String endTime) {
        Channel channel = router.require(channelKey);
        return wvp.data(WvpApi.GB_RECORD_QUERY,
                WvpClient.params("startTime", startTime, "endTime", endTime),
                channel.gbDeviceId(), channel.gbChannelId());
    }

    private void scheduleClose(Channel channel, String streamKey, String startTime, String endTime) {
        long expected = Math.max(1, Duration.between(Times.parse(startTime), Times.parse(endTime)).getSeconds())
                + props.getRecord().getDownloadBufferSeconds();
        long startEpoch = System.currentTimeMillis() / 1000;
        state.set("download:" + streamKey,
                "DOWNLOADING|" + startEpoch + "|" + expected, TASK_TTL);
        state.addToSet("record:keys:" + channel.channelKey(), streamKey,
                Duration.ofDays(props.getRecord().getKeysRetentionDays()));

        // NVR 的回放流播完会从头循环，ZLM 等不到 EOF，只能按录像时长主动关流
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            zlm.closeStream(streamKey);
            closeTasks.remove(streamKey);
            state.set("download:" + streamKey,
                    "CLOSING|" + startEpoch + "|" + expected, TASK_TTL);
            log.info("下载回放定时关流: {}", streamKey);
        }, expected, TimeUnit.SECONDS);
        closeTasks.put(streamKey, task);
    }

    private String tryMarkDownloaded(String streamKey, long startEpoch, long expected) {
        if (System.currentTimeMillis() / 1000 - startEpoch < expected + 5) {
            return null;
        }
        if (zlm.isStreamOnline(streamKey)) {
            return null;
        }
        try {
            zlm.mp4RecordFile(props.getZlm().getApp(), streamKey, LocalDate.now().toString());
        } catch (BridgeException e) {
            return null;
        }
        return "DOWNLOADED";
    }

    private boolean downloadOf(String streamKey) {
        String json = state.get("playback:" + streamKey);
        if (json == null) {
            return false;
        }
        try {
            return mapper.readTree(json).path("download").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private String writeTask(Map<String, Object> task) {
        try {
            return mapper.writeValueAsString(task);
        } catch (Exception e) {
            throw new BridgeException("回放任务状态序列化失败", e);
        }
    }

    private String protocolOf(String streamKey) {
        String json = state.get("playback:" + streamKey);
        if (json == null) {
            return VideoProtocol.ONVIF.name();
        }
        try {
            return mapper.readTree(json).path("protocol").asText(VideoProtocol.ONVIF.name());
        } catch (Exception e) {
            return VideoProtocol.ONVIF.name();
        }
    }

    private String channelKeyOf(String streamKey) {
        String json = state.get("playback:" + streamKey);
        if (json == null) {
            throw new BridgeException("任务已过期: " + streamKey);
        }
        try {
            return mapper.readTree(json).path("channelKey").asText(null);
        } catch (Exception e) {
            throw new BridgeException("任务信息损坏: " + streamKey);
        }
    }
}
