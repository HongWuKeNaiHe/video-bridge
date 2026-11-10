package dev.videobridge.storage;

import dev.videobridge.config.BridgeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "bridge.state", name = "type", havingValue = "redis")
public class RedisStateStore implements StateStore {

    private final StringRedisTemplate redis;
    private final String prefix;

    public RedisStateStore(StringRedisTemplate redis, BridgeProperties props) {
        this.redis = redis;
        this.prefix = props.getState().getKeyPrefix();
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(prefix + key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(prefix + key, value, ttl);
    }

    @Override
    public void delete(String key) {
        redis.delete(prefix + key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(prefix + key));
    }

    @Override
    public void addToSet(String key, String value, Duration ttl) {
        String k = prefix + key;
        redis.opsForSet().add(k, value);
        redis.expire(k, ttl);
    }

    @Override
    public Set<String> members(String key) {
        Set<String> values = redis.opsForSet().members(prefix + key);
        return values == null ? Set.of() : values;
    }
}
