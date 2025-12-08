package com.gdn.project.waroenk.member.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ReadFrom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfiguration {
  private final RedisProperties redisProperties;
  private final ObjectMapper redisObjectMapper;

  public CacheConfiguration(
      RedisProperties redisProperties,
      @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
    this.redisProperties = redisProperties;
    this.redisObjectMapper = redisObjectMapper;
  }

  // Custom properties for Sentinel (not using Spring's sentinel properties to avoid auto-config issues)
  @Value("${REDIS_SENTINEL_MASTER:}")
  private String sentinelMaster;

  @Value("${REDIS_SENTINEL_NODES:}")
  private String sentinelNodes;

  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    // Check if Sentinel is configured via environment variables
    if (isSentinelConfigured()) {
      log.info("🔴 Redis Sentinel mode enabled - connecting to master: {}", sentinelMaster);
      return createSentinelConnectionFactory();
    } else {
      log.info("🔴 Redis Standalone mode enabled - connecting to {}:{}",
          redisProperties.getHost(), redisProperties.getPort());
      return createStandaloneConnectionFactory();
    }
  }

  /**
   * Check if Redis Sentinel is configured via environment variables
   */
  private boolean isSentinelConfigured() {
    return StringUtils.hasText(sentinelMaster) && StringUtils.hasText(sentinelNodes);
  }

  /**
   * Create connection factory for Redis Sentinel (HA mode)
   */
  private LettuceConnectionFactory createSentinelConnectionFactory() {
    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .readFrom(ReadFrom.REPLICA_PREFERRED)
        .commandTimeout(Duration.ofSeconds(2))
        .build();

    RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
        .master(sentinelMaster);

    // Parse and add sentinel nodes (format: "host1:port1,host2:port2,host3:port3")
    Arrays.stream(sentinelNodes.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .forEach(node -> {
          String[] hostAndPort = node.split(":");
          if (hostAndPort.length == 2) {
            sentinelConfig.sentinel(RedisNode.newRedisNode()
                .listeningAt(hostAndPort[0], Integer.parseInt(hostAndPort[1]))
                .build());
            log.debug("Added Sentinel node: {}:{}", hostAndPort[0], hostAndPort[1]);
          }
        });

    // Set password if configured
    if (StringUtils.hasText(redisProperties.getPassword())) {
      sentinelConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
    }

    LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig, clientConfig);
    factory.afterPropertiesSet();
    return factory;
  }

  /**
   * Create connection factory for standalone Redis (single instance)
   */
  private LettuceConnectionFactory createStandaloneConnectionFactory() {
    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.ofSeconds(2))
        .build();

    RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
    standaloneConfig.setHostName(redisProperties.getHost());
    standaloneConfig.setPort(redisProperties.getPort());
    standaloneConfig.setDatabase(redisProperties.getDatabase());

    // Set password if configured
    if (StringUtils.hasText(redisProperties.getPassword())) {
      standaloneConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
    }

    // Set username if configured (Redis 6+ ACL)
    if (StringUtils.hasText(redisProperties.getUsername())) {
      standaloneConfig.setUsername(redisProperties.getUsername());
    }

    LettuceConnectionFactory factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
    factory.afterPropertiesSet();
    return factory;
  }

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
    // Use Redis-specific ObjectMapper with type info for proper deserialization
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

    RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
        .disableCachingNullValues()
        .entryTtl(Duration.ofMinutes(30))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
    redisCacheConfiguration.usePrefix();

    return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory)
        .cacheDefaults(redisCacheConfiguration)
        .build();
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    // Use Redis-specific ObjectMapper with type info for proper deserialization
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

    final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(jsonSerializer);
    redisTemplate.setHashValueSerializer(jsonSerializer);
    redisTemplate.setConnectionFactory(redisConnectionFactory);
    return redisTemplate;
  }
}
