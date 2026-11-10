package dev.videobridge.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.videobridge.common.BridgeException;
import dev.videobridge.config.BridgeProperties;
import dev.videobridge.storage.Channel;
import dev.videobridge.wvp.WvpApi;
import dev.videobridge.wvp.WvpClient;
import dev.videobridge.zlm.PlayUrls;
import dev.videobridge.zlm.ZlmClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WvpChannelDriver implements ChannelDriver {

    private final WvpClient wvp;
    private final ZlmClient zlm;
    private final BridgeProperties props;
    private final ObjectMapper mapper;

    public WvpChannelDriver(WvpClient wvp, ZlmClient zlm, BridgeProperties props, ObjectMapper mapper) {
        this.wvp = wvp;
        this.zlm = zlm;
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public VideoProtocol protocol() {
        return VideoProtocol.GB28181;
    }

    @Override
    public StreamTicket start(Channel channel) {
        String endpoint = WvpApi.PLAY_START;
        if (props.getWvp().isForkExtensions() && channel.permanent()) {
            endpoint = WvpApi.PLAY_PERSISTENT;
        }
        JsonNode data = wvp.data(endpoint, null, channel.gbDeviceId(), channel.gbChannelId());
        if (data == null || data.isMissingNode()) {
            throw new BridgeException("wvp 点流没有返回数据: " + channel.channelKey());
        }
        String stream = text(data, "stream");
        PlayUrls urls;
        if (props.getWvp().isShareZlm() && stream != null && !stream.isBlank()) {
            // wvp 返回的地址里是 ZLM 内网 IP，共用 ZLM 时直接按对外地址重算，比字符串替换可靠
            String app = text(data, "app");
            urls = zlm.urls(stream, app == null ? "rtp" : app);
        } else {
            urls = new PlayUrls(stream == null ? channel.streamKey() : stream,
                    text(data, "rtc"), text(data, "rtsp"), text(data, "ws_flv"), text(data, "flv"));
        }
        Map<String, Object> raw = mapper.convertValue(data, new TypeReference<>() {
        });
        return StreamTicket.of(urls, raw);
    }

    @Override
    public void stop(Channel channel) {
        if (channel.permanent()) {
            return;
        }
        wvp.data(WvpApi.PLAY_STOP, null, channel.gbDeviceId(), channel.gbChannelId());
    }

    @Override
    public void ptz(Channel channel, String command, float speed) {
        if (!channel.ptzEnabled()) {
            return;
        }
        // 国标云台速度是 1~255，对外接口统一用 0~1
        int gbSpeed = Math.max(1, Math.min(255, Math.round(speed * 255)));
        wvp.data(WvpApi.PTZ, WvpClient.params("serial", channel.gbDeviceId(),
                "code", channel.gbChannelId(), "command", command, "speed", gbSpeed));
    }

    @Override
    public List<Preset> presets(Channel channel) {
        if (!channel.ptzEnabled()) {
            return List.of();
        }
        JsonNode data = wvp.data(WvpApi.FETCH_PRESET,
                WvpClient.params("serial", channel.gbDeviceId(), "code", channel.gbChannelId(), "fill", true));
        List<Preset> result = new ArrayList<>();
        JsonNode list = pickList(data);
        if (list == null) {
            return result;
        }
        for (JsonNode item : list) {
            String token = text(item, "presetId", "preset", "id", "presetIndex");
            if (token != null) {
                String name = text(item, "name", "presetName", "description");
                result.add(new Preset(token, name == null ? "" : name));
            }
        }
        return result;
    }

    @Override
    public String preset(Channel channel, String command, String presetToken, String name) {
        if (!channel.ptzEnabled()) {
            return "ok";
        }
        wvp.data(WvpApi.PRESET, WvpClient.params("serial", channel.gbDeviceId(),
                "code", channel.gbChannelId(), "command", command,
                "preset", presetToken, "name", name));
        // wvp 的控制接口响应体是空的，拿不到设备分配的预置位号
        return "ok";
    }

    @Override
    public void keepAlive(Channel channel) {
        if (!channel.permanent()) {
            return;
        }
        // 2.7.x 的 /api/v1/stream/touch 是空实现，重复点播能拿回已有流并续期，比 touch 管用
        wvp.data(WvpApi.PLAY_START, null, channel.gbDeviceId(), channel.gbChannelId());
    }

    private static JsonNode pickList(JsonNode data) {
        if (data == null || data.isMissingNode() || data.isNull()) {
            return null;
        }
        if (data.isArray()) {
            return data;
        }
        for (String key : List.of("list", "data", "rows")) {
            JsonNode node = data.path(key);
            if (node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isValueNode() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }
}
