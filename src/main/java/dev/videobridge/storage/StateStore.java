package dev.videobridge.storage;

import java.time.Duration;
import java.util.Set;

/** token、录像状态、下载进度这类短命数据，默认放内存，也可以切到 Redis */
public interface StateStore {

    String get(String key);

    void set(String key, String value, Duration ttl);

    void delete(String key);

    boolean exists(String key);

    void addToSet(String key, String value, Duration ttl);

    Set<String> members(String key);
}
