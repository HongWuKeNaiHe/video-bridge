package dev.videobridge.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.videobridge.common.BridgeException;
import dev.videobridge.common.Times;
import dev.videobridge.config.BridgeProperties;
import dev.videobridge.onvif.HikvisionIsapi;
import dev.videobridge.onvif.OnvifClient;
import dev.videobridge.onvif.OnvifModels;
import dev.videobridge.storage.Channel;
import dev.videobridge.storage.ChannelRepository;
import dev.videobridge.storage.StateStore;
import dev.videobridge.wvp.WvpApi;
import dev.videobridge.wvp.WvpClient;
import dev.videobridge.zlm.ZlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final ChannelRouter router;
    private final ChannelRepository repository;
    private final ZlmClient zlm;
    private final WvpClient wvp;
    private final OnvifClient onvif;
    private final HikvisionIsapi isapi;
    private final StateStore state;
    private final ObjectMapper mapper;
    private final BridgeProperties props;
    private final ScheduledExecutorService scheduler;

    public RecordService(ChannelRouter router, ChannelRepository repository, ZlmClient zlm,
                         WvpClient wvp, OnvifClient onvif, HikvisionIsapi isapi,
                         StateStore state, ObjectMapper mapper, BridgeProperties props,
                         ScheduledExecutorService scheduler) {
        this.router = router;
        this.repository = repository;
        this.zlm = zlm;
        this.wvp = wvp;
        this.onvif = onvif;
        this.isapi = isapi;
        this.state = state;
        this.mapper = mapper;
        this.props = props;
        this.scheduler = scheduler;
    }

    public Map<String, Object> start(String channelKey) {
        Channel channel = router.require(channelKey);
        if (state.exists("record:" + channelKey)) {
            throw new BridgeException("通道正在录像中: " + channelKey);
        }

        String streamKey;
        if (channel.protocol() == VideoProtocol.ONVIF) {
            String rtsp = onvif.streamUri(channel.onvifDevice(), null);
            if (rtsp == null) {
                throw new BridgeException("ONVIF 没有返回 RTSP 地址: " + channelKey);
            }
            streamKey = "record_" + channelKey + "_" + System.currentTimeMillis();
            zlm.addProxy(rtsp, streamKey, ZlmClient.Mode.RECORD);
        } else {
            wvp.data(WvpApi.RECORD_START, null, channel.gbDeviceId(), channel.gbChannelId());
            streamKey = channel.streamKey();
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("streamKey", streamKey);
        info.put("startTimeMs", System.currentTimeMillis());
        info.put("protocol", channel.protocol().name());
        state.set("record:" + channelKey, write(info),
                Duration.ofSeconds(props.getRecord().getMaxDurationSeconds()));
        state.addToSet("record:keys:" + channelKey, streamKey,
                Duration.ofDays(props.getRecord().getKeysRetentionDays()));
        scheduleAutoStop(channelKey, streamKey);
        return info;
    }

    public Map<String, Object> stop(String channelKey) {
        Channel channel = router.require(channelKey);
        Map<String, Object> info = read("record:" + channelKey);
        if (info == null) {
            return Map.of("stopped", false, "reason", "通道未在录像");
        }
        String streamKey = String.valueOf(info.get("streamKey"));
        if (VideoProtocol.ONVIF.name().equals(info.get("protocol"))) {
            zlm.closeStream(streamKey);
        } else {
            wvp.data(WvpApi.RECORD_STOP, null, channel.gbDeviceId(), channel.gbChannelId());
        }
        state.delete("record:" + channelKey);
        return Map.of("stopped", true, "streamKey", streamKey);
    }

    public Map<String, Object> status(String channelKey) {
        Map<String, Object> info = read("record:" + channelKey);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channelKey", channelKey);
        result.put("isRecording", info != null);
        if (info != null) {
            result.putAll(info);
            long start = ((Number) info.getOrDefault("startTimeMs", 0L)).longValue();
            long remaining = Math.max(0,
                    props.getRecord().getMaxDurationSeconds() - (System.currentTimeMillis() - start) / 1000);
            result.put("remainingSeconds", remaining);
        }
        return result;
    }

    public Map<String, Object> list(String channelKey, String startTime, String endTime) {
        Channel channel = router.require(channelKey);
        if (channel.protocol() == VideoProtocol.GB28181) {
            JsonNode data = wvp.data(WvpApi.CLOUD_RECORD_LIST,
                    WvpClient.params("query", channel.gbDeviceId() + "_" + channel.gbChannelId(),
                            "startTime", startTime, "endTime", endTime, "page", 1, "count", 100));
            Map<String, Object> payload = data == null
                    ? Map.of() : mapper.convertValue(data, new TypeReference<>() {
            });
            return Map.of("channelKey", channelKey, "source", "wvp",
                    "data", payload == null ? Map.of() : payload);
        }

        Set<String> streamKeys = new java.util.HashSet<>(state.members("record:keys:" + channelKey));
        // Redis 里可能漏记，ZLM 当前活着的录像流也一起算上
        JsonNode media = zlm.mediaList(props.getZlm().getApp(), null);
        JsonNode streams = media == null ? null : media.path("data");
        if (streams != null && streams.isArray()) {
            for (JsonNode item : streams) {
                String stream = item.path("stream").asText("");
                if (stream.startsWith("record_" + channelKey + "_")) {
                    streamKeys.add(stream);
                }
            }
        }

        long startEpoch = Times.epochSeconds(startTime);
        long endEpoch = Times.epochSeconds(endTime);
        List<Map<String, Object>> files = new ArrayList<>();
        String app = props.getZlm().getApp();
        for (String streamKey : streamKeys) {
            for (String date : Times.dates(startTime, endTime)) {
                JsonNode result;
                try {
                    result = zlm.mp4RecordFile(app, streamKey, date);
                } catch (BridgeException e) {
                    // 当天没有录像文件时 ZLM 会直接报错，跳过即可
                    continue;
                }
                JsonNode data = result == null ? null : result.path("data");
                JsonNode paths = data == null ? null : data.path("paths");
                if (paths == null || !paths.isArray()) {
                    continue;
                }
                for (JsonNode path : paths) {
                    String fileName = path.asText();
                    if (!Times.inRange(fileName, startEpoch, endEpoch)) {
                        continue;
                    }
                    Map<String, Object> file = new LinkedHashMap<>();
                    file.put("fileName", fileName);
                    file.put("app", app);
                    file.put("stream", streamKey);
                    file.put("date", date);
                    file.put("downloadUrl", zlm.recordFileUrl(app, streamKey, date, fileName));
                    files.add(file);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channelKey", channelKey);
        result.put("source", "zlm");
        result.put("total", files.size());
        result.put("files", files);
        return result;
    }

    public List<OnvifModels.RecordSegment> nvrRecords(String channelKey, String startTime, String endTime) {
        Channel channel = router.require(channelKey);
        if (channel.protocol() != VideoProtocol.ONVIF) {
            throw new BridgeException("NVR 录像检索只支持 ONVIF 通道");
        }
        return isapi.records(channel.onvifDevice(), channel.nvrHostOrSelf(),
                props.getOnvif().getDefaultPort(),
                channel.channelNo() == null ? 1 : channel.channelNo(), startTime, endTime);
    }

    /** 服务重启会丢掉内存里的定时停止任务，靠状态 TTL 兜底把残留的录像流关掉 */
    public void closeOrphanStreams() {
        for (Channel channel : repository.findAll()) {
            if (channel.protocol() != VideoProtocol.ONVIF || state.exists("record:" + channel.channelKey())) {
                continue;
            }
            for (String streamKey : state.members("record:keys:" + channel.channelKey())) {
                if (zlm.isStreamOnline(streamKey)) {
                    log.info("关闭残留录像流: {}", streamKey);
                    zlm.closeStream(streamKey);
                }
            }
        }
    }

    private void scheduleAutoStop(String channelKey, String streamKey) {
        scheduler.schedule(() -> {
            Map<String, Object> current = read("record:" + channelKey);
            if (current == null || !streamKey.equals(current.get("streamKey"))) {
                return;
            }
            try {
                stop(channelKey);
            } catch (Exception e) {
                log.warn("录像自动停止失败: {}", channelKey, e);
            }
        }, props.getRecord().getMaxDurationSeconds(), TimeUnit.SECONDS);
    }

    private String write(Map<String, Object> value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BridgeException("录像状态序列化失败", e);
        }
    }

    private Map<String, Object> read(String key) {
        String json = state.get(key);
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}
