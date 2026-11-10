package dev.videobridge;

import dev.videobridge.core.ChannelDriver;
import dev.videobridge.core.ChannelRouter;
import dev.videobridge.core.VideoProtocol;
import dev.videobridge.storage.Channel;
import dev.videobridge.storage.ChannelRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChannelRouterTest {

    private static Channel channel(VideoProtocol protocol, boolean enabled) {
        return new Channel(1L, "cam1", "大门", protocol, "34020000001320000001",
                "34020000001310000001", "10.0.0.8", 80, "admin", "123456",
                null, 1, "AUTO", true, false, enabled, null, null);
    }

    @Test
    void routesByProtocol() {
        ChannelRepository repository = mock(ChannelRepository.class);
        ChannelDriver onvif = mock(ChannelDriver.class);
        ChannelDriver gb = mock(ChannelDriver.class);
        when(onvif.protocol()).thenReturn(VideoProtocol.ONVIF);
        when(gb.protocol()).thenReturn(VideoProtocol.GB28181);

        ChannelRouter router = new ChannelRouter(repository, List.of(onvif, gb));
        when(repository.findByKey("cam1")).thenReturn(Optional.of(channel(VideoProtocol.ONVIF, true)));

        assertSame(onvif, router.driver(router.require("cam1")));
    }

    @Test
    void disabledChannelIsRejected() {
        ChannelRepository repository = mock(ChannelRepository.class);
        when(repository.findByKey("cam1")).thenReturn(Optional.of(channel(VideoProtocol.GB28181, false)));
        ChannelRouter router = new ChannelRouter(repository, List.of());
        assertThrows(dev.videobridge.common.BridgeException.class, () -> router.require("cam1"));
    }

    @Test
    void streamKeyDiffersByProtocol() {
        assertEquals("cam1", channel(VideoProtocol.ONVIF, true).streamKey());
        assertEquals("34020000001320000001_34020000001310000001",
                channel(VideoProtocol.GB28181, true).streamKey());
    }
}
