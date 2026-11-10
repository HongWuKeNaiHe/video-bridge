package dev.videobridge;

import dev.videobridge.onvif.ReplayUriBuilder;
import dev.videobridge.onvif.Vendor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReplayUriBuilderTest {

    private static final String START = "2026-06-16 00:00:00";
    private static final String END = "2026-06-16 01:00:00";

    @Test
    void timeFormats() {
        assertEquals("2026-06-16T00:00:00Z", ReplayUriBuilder.standard(START));
        assertEquals("20260616T000000Z", ReplayUriBuilder.compact(START));
        assertEquals("2026-06-16T01:00:00Z", ReplayUriBuilder.standard("20260616010000"));
        assertNull(ReplayUriBuilder.standard("not a time"));
    }

    @Test
    void hikvisionTracksUrl() {
        String url = ReplayUriBuilder.build(Vendor.HIKVISION, "10.0.0.5", 554,
                "admin", "p@ss", 3, START, END);
        assertTrue(url.startsWith("rtsp://admin:p%40ss@10.0.0.5:554/Streaming/tracks/301?"));
        assertTrue(url.contains("starttime=20260616T000000Z"));
        assertTrue(url.contains("endtime=20260616T010000Z"));
    }

    @Test
    void dahuaUsesZeroBasedChannel() {
        String url = ReplayUriBuilder.build(Vendor.DAHUA, "10.0.0.6", 554,
                "admin", "123456", 3, START, END);
        assertTrue(url.contains("/cam/playback?channel=2&subtype=0"));
    }

    @Test
    void genericFallsBackToTracks() {
        assertTrue(ReplayUriBuilder.build(Vendor.GENERIC, "10.0.0.7", 554,
                "admin", "123456", 1, START, END).contains("/Streaming/tracks/101?"));
    }

    @Test
    void detectsVendorFromManufacturerOrStreamUri() {
        assertEquals(Vendor.HIKVISION, Vendor.detect("HIKVISION", null));
        assertEquals(Vendor.DAHUA, Vendor.detect(null, "rtsp://x/cam/realmonitor"));
        assertEquals(Vendor.HIKVISION, Vendor.detect("unknown", "rtsp://x/Streaming/Channels/101"));
        assertEquals(Vendor.GENERIC, Vendor.detect("unknown", "rtsp://x/live/0"));
    }
}
