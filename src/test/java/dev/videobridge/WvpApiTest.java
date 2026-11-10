package dev.videobridge;

import dev.videobridge.wvp.WvpApi;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WvpApiTest {

    @Test
    void defaultsAreOfficialPaths() {
        WvpApi api = new WvpApi(Map.of());
        assertEquals("/api/v1/device/list", api.path(WvpApi.DEVICE_LIST));
        assertEquals("/api/play/start/34020000001320000001/34020000001310000001",
                api.path(WvpApi.PLAY_START, "34020000001320000001", "34020000001310000001"));
    }

    @Test
    void endpointsCanBeOverriddenPerVersion() {
        WvpApi api = new WvpApi(Map.of(WvpApi.PLAY_START, "/custom/play/{0}/{1}"));
        assertEquals("/custom/play/D1/C1", api.path(WvpApi.PLAY_START, "D1", "C1"));
    }

    @Test
    void pathArgsAreUrlEncoded() {
        WvpApi api = new WvpApi(Map.of());
        assertEquals("/api/playback/speed/a+b/2.0",
                api.path(WvpApi.PLAYBACK_SPEED, "a b", 2.0));
    }

    @Test
    void unknownKeyFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> new WvpApi(Map.of()).path("notAKey"));
    }
}
