package dev.videobridge.storage;

import dev.videobridge.config.BridgeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(prefix = "bridge.state", name = "type", havingValue = "memory", matchIfMissing = true)
public class MemoryStateStore implements StateStore {

    private record Entry(String value, long expireAt) {
    }

    private final String prefix;
    private final Map<String, Entry> values = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sets = new ConcurrentHashMap<>();
    private final Map<String, Long> setExpireAt = new ConcurrentHashMap<>();

    public MemoryStateStore(BridgeProperties props) {
        this.prefix = props.getState().getKeyPrefix();
    }

    @Override
    public String get(String key) {
        String k = prefix + key;
        Entry entry = values.get(k);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt() <= System.currentTimeMillis()) {
            values.remove(k);
            return null;
        }
        return entry.value();
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        values.put(prefix + key, new Entry(value, System.currentTimeMillis() + ttl.toMillis()));
    }

    @Override
    public void delete(String key) {
        values.remove(prefix + key);
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public void addToSet(String key, String value, Duration ttl) {
        String k = prefix + key;
        sets.computeIfAbsent(k, ignored -> ConcurrentHashMap.newKeySet()).add(value);
        setExpireAt.put(k, System.currentTimeMillis() + ttl.toMillis());
    }

    @Override
    public Set<String> members(String key) {
        String k = prefix + key;
        Long expireAt = setExpireAt.get(k);
        if (expireAt == null || expireAt <= System.currentTimeMillis()) {
            sets.remove(k);
            setExpireAt.remove(k);
            return Set.of();
        }
        return new HashSet<>(sets.getOrDefault(k, Set.of()));
    }
}
