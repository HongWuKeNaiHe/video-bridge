package dev.videobridge.core;

import dev.videobridge.common.BridgeException;
import dev.videobridge.onvif.OnvifClient;
import dev.videobridge.storage.Channel;
import dev.videobridge.zlm.PlayUrls;
import dev.videobridge.zlm.ZlmClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OnvifChannelDriver implements ChannelDriver {

    private final OnvifClient onvif;
    private final ZlmClient zlm;

    public OnvifChannelDriver(OnvifClient onvif, ZlmClient zlm) {
        this.onvif = onvif;
        this.zlm = zlm;
    }

    @Override
    public VideoProtocol protocol() {
        return VideoProtocol.ONVIF;
    }

    @Override
    public StreamTicket start(Channel channel) {
        String key = channel.streamKey();
        // 同一个通道重复点播直接复用已有流，不然 ZLM 会建出第二条代理
        if (zlm.isStreamOnline(key)) {
            return StreamTicket.of(zlm.urls(key));
        }
        String rtsp = onvif.streamUri(channel.onvifDevice(), null);
        if (rtsp == null) {
            throw new BridgeException("ONVIF 没有返回 RTSP 地址: " + key);
        }
        PlayUrls urls = zlm.addProxy(rtsp, key,
                channel.permanent() ? ZlmClient.Mode.PERMANENT : ZlmClient.Mode.LIVE);
        return StreamTicket.of(urls);
    }

    @Override
    public void stop(Channel channel) {
        if (!channel.permanent()) {
            zlm.closeStream(channel.streamKey());
        }
    }

    @Override
    public void ptz(Channel channel, String command, float speed) {
        if (channel.ptzEnabled()) {
            onvif.ptz(channel.onvifDevice(), null, command, speed);
        }
    }

    @Override
    public List<Preset> presets(Channel channel) {
        if (!channel.ptzEnabled()) {
            return List.of();
        }
        return onvif.presets(channel.onvifDevice(), null).stream()
                .map(p -> new Preset(p.token(), p.name()))
                .toList();
    }

    @Override
    public String preset(Channel channel, String command, String presetToken, String name) {
        if (!channel.ptzEnabled()) {
            return "ok";
        }
        return onvif.preset(channel.onvifDevice(), null, command, presetToken, name);
    }

    @Override
    public void keepAlive(Channel channel) {
        // auto_close=0 只能保证没人看时不断，设备重启或网络抖动后还得重新拉一次
        if (channel.permanent() && !zlm.isStreamOnline(channel.streamKey())) {
            start(channel);
        }
    }
}
