package dev.videobridge.core;

import com.fasterxml.jackson.databind.JsonNode;
import dev.videobridge.common.BridgeException;
import dev.videobridge.config.BridgeProperties;
import dev.videobridge.storage.Channel;
import dev.videobridge.wvp.WvpApi;
import dev.videobridge.wvp.WvpClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ControlService {

    private final ChannelRouter router;
    private final WvpClient wvp;
    private final BridgeProperties props;

    public ControlService(ChannelRouter router, WvpClient wvp, BridgeProperties props) {
        this.router = router;
        this.wvp = wvp;
        this.props = props;
    }

    public void ptz(String channelKey, String command, float speed) {
        Channel channel = router.require(channelKey);
        router.driver(channel).ptz(channel, command, speed);
    }

    public List<Preset> presets(String channelKey) {
        Channel channel = router.require(channelKey);
        return router.driver(channel).presets(channel);
    }

    public String preset(String channelKey, String command, String presetToken, String name) {
        Channel channel = router.require(channelKey);
        return router.driver(channel).preset(channel, command, presetToken, name);
    }

    public JsonNode homePosition(String channelKey, Boolean enabled, Integer resetTime, Integer presetIndex) {
        Channel channel = router.require(channelKey);
        if (channel.protocol() != VideoProtocol.GB28181) {
            throw new BridgeException("看守位是国标能力，ONVIF 通道不支持");
        }
        if (!props.getWvp().isForkExtensions()) {
            throw new BridgeException("看守位依赖扩展版 wvp，请打开 bridge.wvp.fork-extensions");
        }
        return wvp.data(WvpApi.HOME_POSITION, WvpClient.params("deviceId", channel.gbDeviceId(),
                "channelId", channel.gbChannelId(), "enabled", enabled,
                "resetTime", resetTime, "presetIndex", presetIndex));
    }
}
