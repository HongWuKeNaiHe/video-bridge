package dev.videobridge.core;

import com.fasterxml.jackson.databind.JsonNode;
import dev.videobridge.config.BridgeProperties;
import dev.videobridge.onvif.HikvisionIsapi;
import dev.videobridge.onvif.OnvifClient;
import dev.videobridge.onvif.OnvifDevice;
import dev.videobridge.onvif.OnvifModels;
import dev.videobridge.storage.Channel;
import dev.videobridge.wvp.WvpApi;
import dev.videobridge.wvp.WvpClient;
import dev.videobridge.zlm.ZlmClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StreamService {

    private final ChannelRouter router;
    private final ZlmClient zlm;
    private final WvpClient wvp;
    private final OnvifClient onvif;
    private final HikvisionIsapi isapi;
    private final BridgeProperties props;

    public StreamService(ChannelRouter router, ZlmClient zlm, WvpClient wvp,
                         OnvifClient onvif, HikvisionIsapi isapi, BridgeProperties props) {
        this.router = router;
        this.zlm = zlm;
        this.wvp = wvp;
        this.onvif = onvif;
        this.isapi = isapi;
        this.props = props;
    }

    public StreamTicket start(String channelKey) {
        Channel channel = router.require(channelKey);
        return router.driver(channel).start(channel);
    }

    public void stop(String channelKey) {
        Channel channel = router.require(channelKey);
        router.driver(channel).stop(channel);
    }

    public void touch(String channelKey) {
        Channel channel = router.require(channelKey);
        router.driver(channel).keepAlive(channel);
    }

    public Map<String, Object> status(String channelKey) {
        Channel channel = router.require(channelKey);
        String app = channel.protocol() == VideoProtocol.ONVIF ? props.getZlm().getApp() : "rtp";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channelKey", channelKey);
        result.put("protocol", channel.protocol().name());
        result.put("streamKey", channel.streamKey());
        result.put("online", zlm.isStreamOnline(channel.streamKey(), app));
        result.put("readers", zlm.readerCount(channel.streamKey(), app));
        result.put("permanent", channel.permanent());
        return result;
    }

    public byte[] snap(String channelKey) {
        Channel channel = router.require(channelKey);
        if (channel.protocol() == VideoProtocol.ONVIF) {
            return zlm.snap(channel.streamKey());
        }
        // 国标流在 ZLM 里的 stream 名由 wvp 分配，先点一次拿到真实名字再截帧
        JsonNode data = wvp.data(WvpApi.PLAY_START, null,
                channel.gbDeviceId(), channel.gbChannelId());
        String stream = data == null ? channel.gbChannelId() : data.path("stream").asText(channel.gbChannelId());
        String app = data == null ? "rtp" : data.path("app").asText("rtp");
        return zlm.snap(stream, app);
    }

    public JsonNode devices(Integer start, Integer limit, String q, Boolean online) {
        return wvp.data(WvpApi.DEVICE_LIST,
                WvpClient.params("start", start, "limit", limit, "q", q, "online", online));
    }

    public JsonNode channels(String serial, String channelType, String code, Integer start,
                             Integer limit, String q, Boolean online) {
        return wvp.data(WvpApi.CHANNEL_LIST, WvpClient.params("serial", serial,
                "channel_type", channelType, "code", code, "start", start,
                "limit", limit, "q", q, "online", online));
    }

    public OnvifModels.DiscoveredDevice probe(OnvifDevice device) {
        return onvif.discover(device);
    }

    public List<OnvifModels.HikChannel> isapiChannels(OnvifDevice device) {
        return isapi.channels(device);
    }

    public void clearToken() {
        wvp.clearToken();
    }
}
