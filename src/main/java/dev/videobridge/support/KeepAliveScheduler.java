package dev.videobridge.support;

import dev.videobridge.core.ChannelRouter;
import dev.videobridge.core.RecordService;
import dev.videobridge.storage.Channel;
import dev.videobridge.storage.ChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final ChannelRepository repository;
    private final ChannelRouter router;
    private final RecordService recordService;

    public KeepAliveScheduler(ChannelRepository repository, ChannelRouter router, RecordService recordService) {
        this.repository = repository;
        this.router = router;
        this.recordService = recordService;
    }

    // 配置里写的是秒，这里补三个零换成毫秒，单个通道出问题不能影响其他通道
    @Scheduled(fixedDelayString = "${bridge.keep-alive.interval-seconds:60}000")
    public void keepAlive() {
        for (Channel channel : repository.findAll()) {
            if (!channel.enabled() || !channel.permanent()) {
                continue;
            }
            try {
                router.driver(channel).keepAlive(channel);
            } catch (Exception e) {
                log.warn("永久流保活失败: {} -> {}", channel.channelKey(), e.getMessage());
            }
        }
        try {
            recordService.closeOrphanStreams();
        } catch (Exception e) {
            log.warn("残留录像流清理失败: {}", e.getMessage());
        }
    }
}
