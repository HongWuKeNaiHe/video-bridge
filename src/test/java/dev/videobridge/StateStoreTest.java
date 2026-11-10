package dev.videobridge;

import dev.videobridge.config.BridgeProperties;
import dev.videobridge.storage.MemoryStateStore;
import dev.videobridge.storage.StateStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class StateStoreTest {

    private static StateStore store() {
        BridgeProperties props = new BridgeProperties();
        props.getState().setKeyPrefix("test:");
        return new MemoryStateStore(props);
    }

    @Test
    void valueExpires() {
        StateStore state = store();
        state.set("k", "v", Duration.ofSeconds(60));
        assertTrue(state.exists("k"));
        assertEquals("v", state.get("k"));
        state.delete("k");
        assertFalse(state.exists("k"));
    }

    @Test
    void expiredValueIsNotReturned() {
        StateStore state = store();
        state.set("k", "v", Duration.ofMillis(-1));
        assertNull(state.get("k"));
    }

    @Test
    void setKeepsMembersUntilTtl() {
        StateStore state = store();
        state.addToSet("keys", "record_1", Duration.ofSeconds(60));
        state.addToSet("keys", "record_2", Duration.ofSeconds(60));
        assertEquals(2, state.members("keys").size());
        assertTrue(state.members("keys").contains("record_1"));
    }
}
