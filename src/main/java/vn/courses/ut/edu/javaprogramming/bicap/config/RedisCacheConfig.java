package vn.courses.ut.edu.javaprogramming.bicap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis caching (BICAP-79 / SRS-API-008).
 *
 * <p>Graceful degradation: if the configured Redis server is unreachable at startup
 * (typical for local dev / CI which run on H2 without Redis), the application falls
 * back to an in-memory {@link ConcurrentMapCacheManager} instead of failing to boot.
 * Which provider is active is logged once at startup.
 *
 * <p>Tune with: {@code app.cache.enabled}, {@code app.cache.ttl-seconds}
 * (Redis connection itself: {@code spring.data.redis.*}).
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    public static final String CACHE_CATEGORIES = "bicapCategories";
    public static final String CACHE_MARKETPLACE_DETAIL = "bicapMarketplaceDetail";

    @Value("${app.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.cache.ttl-seconds:60}")
    private long ttlSeconds;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        if (cacheEnabled && redisAvailable(connectionFactory)) {
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(ttlSeconds))
                    .disableCachingNullValues()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new GenericJackson2JsonRedisSerializer()));
            log.info("Cache provider: REDIS ({}), TTL {}s", connectionFactory.getClass().getSimpleName(), ttlSeconds);
            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(config)
                    .build();
        }
        log.warn("Cache provider: IN-MEMORY fallback (Redis disabled or unreachable at "
                + "{}). Set SPRING_REDIS_HOST/PORT/PASSWORD to enable distributed caching.",
                connectionFactory.getClass().getSimpleName());
        return new ConcurrentMapCacheManager(CACHE_CATEGORIES, CACHE_MARKETPLACE_DETAIL);
    }

    /** Quick PING with the configured (short) connect timeout — never blocks startup long. */
    private boolean redisAvailable(RedisConnectionFactory factory) {
        try (RedisConnection connection = factory.getConnection()) {
            connection.ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis ping failed: {}", e.getMessage());
            return false;
        }
    }
}
