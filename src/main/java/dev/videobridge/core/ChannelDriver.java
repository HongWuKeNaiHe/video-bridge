package dev.videobridge.core;

import dev.videobridge.storage.Channel;

import java.util.List;

/** 上层只认这一个接口，ONVIF 和 GB28181 的差异都关在各自实现里 */
public interface ChannelDriver {

    VideoProtocol protocol();

    StreamTicket start(Channel channel);

    void stop(Channel channel);

    void ptz(Channel channel, String command, float speed);

    List<Preset> presets(Channel channel);

    /** set 返回设备分配的预置位号，其余返回 "ok" */
    String preset(Channel channel, String command, String presetToken, String name);

    void keepAlive(Channel channel);
}
