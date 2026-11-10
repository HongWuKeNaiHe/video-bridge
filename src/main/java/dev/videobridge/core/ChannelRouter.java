package dev.videobridge.core;

import dev.videobridge.common.BridgeException;
import dev.videobridge.storage.Channel;
import dev.videobridge.storage.ChannelRepository;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ChannelRouter {

    private final ChannelRepository repository;
    private final Map<VideoProtocol, ChannelDriver> drivers = new EnumMap<>(VideoProtocol.class);

    public ChannelRouter(ChannelRepository repository, List<ChannelDriver> driverList) {
        this.repository = repository;
        driverList.forEach(driver -> drivers.put(driver.protocol(), driver));
    }

    public Channel require(String channelKey) {
        return repository.findByKey(channelKey)
                .filter(Channel::enabled)
                .orElseThrow(() -> new BridgeException("通道不存在或已禁用: " + channelKey));
    }

    public ChannelDriver driver(Channel channel) {
        ChannelDriver driver = drivers.get(channel.protocol());
        if (driver == null) {
            throw new BridgeException("没有对应的协议驱动: " + channel.protocol());
        }
        return driver;
    }
}
