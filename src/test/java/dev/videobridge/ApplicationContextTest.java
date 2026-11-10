package dev.videobridge;

import dev.videobridge.core.ChannelRouter;
import dev.videobridge.storage.ChannelRepository;
import dev.videobridge.storage.StateStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ApplicationContextTest {

    @Autowired
    private ChannelRouter router;

    @Autowired
    private ChannelRepository repository;

    @Autowired
    private StateStore stateStore;

    @Test
    void contextStartsWithDefaultMemoryState() {
        assertNotNull(router);
        assertNotNull(repository);
        assertTrue(repository.findAll().isEmpty());
        assertTrue(stateStore instanceof dev.videobridge.storage.MemoryStateStore);
    }
}
