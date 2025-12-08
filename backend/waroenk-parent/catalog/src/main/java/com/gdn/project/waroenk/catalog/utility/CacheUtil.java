package com.gdn.project.waroenk.catalog.utility;

import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheUtil<T> {
  private final RedisTemplate<String, T> redisTemplate;
  private final ValueOperations<String, T> valueOperations;
  private final ListOperations<String, T> listOperations;

  @Autowired
  public CacheUtil(RedisTemplate<String, T> redisTemplate) {
    this.redisTemplate = redisTemplate;
    this.valueOperations = redisTemplate.opsForValue();
    this.listOperations = redisTemplate.opsForList();
  }

  private RedisConnection getConnection() {
    RedisConnectionFactory factory = this.redisTemplate.getConnectionFactory();
    Objects.requireNonNull(factory, "Connection factory must not be null");
    RedisConnection connection = factory.getConnection();
    if (connection.isClosed()) {
      throw new RedisConnectionException("Redis connection is closed");
    }
    return connection;
  }

  public boolean flushAll() {
    try {
      log.info("Flushing all redis cache");
      getConnection().serverCommands().flushAll();
      return true;
    } catch (Exception err) {
      log.warn("Fail to flush all redis cache");
      return false;
    }
  }

  public boolean flushKeysByPattern(String pattern) {
    try {
      // Use scan to retrieve keys matching the pattern
      Set<String> keysToDelete = new HashSet<>();
      redisTemplate.scan(ScanOptions.scanOptions().match(pattern).build()).forEachRemaining(keysToDelete::add);

      // Delete the retrieved keys
      if (!keysToDelete.isEmpty()) {
        log.info("Flushing redis cache with pattern {}", pattern);
        redisTemplate.delete(keysToDelete);
      }
      return true;
    } catch (Exception err) {
      log.warn("Fail to flush redis cache with pattern {}", pattern);
      return false;
    }
  }

  public void putValue(String key, T value, long timeout, TimeUnit timeUnit) {
    try {
      valueOperations.set(key, value, timeout, timeUnit);
    } catch (SerializationException e) {
      log.error("Failed to serialize value for key: {}. Error: {}", key, e.getMessage());
    }
  }

  public void putValue(String key, T value) {
    putValue(key, value, 1, TimeUnit.HOURS);
  }

  /**
   * Get value from cache. If deserialization fails (e.g., due to schema change),
   * the corrupted entry is automatically evicted and null is returned.
   */
  public T getValue(String key) {
    try {
      return valueOperations.get(key);
    } catch (SerializationException | ClassCastException e) {
      log.warn("Failed to deserialize cache entry for key: {}. Evicting corrupted entry. Error: {}",
          key,
          e.getMessage());
      evictSafely(key);
      return null;
    } catch (Exception e) {
      log.warn("Unexpected error reading cache for key: {}. Evicting entry. Error: {}", key, e.getMessage());
      evictSafely(key);
      return null;
    }
  }

  public void setExpire(String key, long timeout, TimeUnit timeUnit) {
    try {
      redisTemplate.expire(key, timeout, timeUnit);
    } catch (Exception e) {
      log.warn("Failed to set expiration for key: {}. Error: {}", key, e.getMessage());
    }
  }

  public void removeValue(String key) {
    try {
      redisTemplate.delete(Objects.requireNonNull(redisTemplate.keys(key)));
    } catch (Exception e) {
      log.warn("Failed to remove value for key: {}. Error: {}", key, e.getMessage());
    }
  }

  /**
   * Get list from cache. If deserialization fails, the corrupted entry is evicted.
   */
  public List<T> getList(String key) {
    try {
      return listOperations.range(key, 0, -1);
    } catch (SerializationException | ClassCastException e) {
      log.warn("Failed to deserialize cache list for key: {}. Evicting corrupted entry. Error: {}",
          key,
          e.getMessage());
      evictSafely(key);
      return Collections.emptyList();
    } catch (Exception e) {
      log.warn("Unexpected error reading cache list for key: {}. Evicting entry. Error: {}", key, e.getMessage());
      evictSafely(key);
      return Collections.emptyList();
    }
  }

  public void addList(String key, T value) {
    try {
      listOperations.leftPush(key, value);
    } catch (SerializationException e) {
      log.error("Failed to serialize list value for key: {}. Error: {}", key, e.getMessage());
    }
  }

  public void addList(String key, T value, int timeout, TimeUnit timeUnit) {
    try {
      listOperations.leftPush(key, value);
      setExpire(key, timeout, timeUnit);
    } catch (SerializationException e) {
      log.error("Failed to serialize list value for key: {}. Error: {}", key, e.getMessage());
    }
  }

  /**
   * Safely evict a cache entry without throwing exceptions
   */
  private void evictSafely(String key) {
    try {
      redisTemplate.delete(key);
      log.info("Evicted corrupted cache entry: {}", key);
    } catch (Exception e) {
      log.error("Failed to evict corrupted cache entry: {}. Error: {}", key, e.getMessage());
    }
  }
}
