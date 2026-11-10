package dev.videobridge;

import dev.videobridge.core.VideoProtocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoProtocolTest {

    @Test
    void acceptsCommonAliases() {
        assertEquals(VideoProtocol.ONVIF, VideoProtocol.parse("onvif"));
        assertEquals(VideoProtocol.GB28181, VideoProtocol.parse("wvp"));
        assertEquals(VideoProtocol.GB28181, VideoProtocol.parse("GB28181"));
        assertEquals(VideoProtocol.GB28181, VideoProtocol.parse(null));
    }

    @Test
    void rejectsUnknownProtocol() {
        assertThrows(IllegalArgumentException.class, () -> VideoProtocol.parse("rtsp"));
    }
}
