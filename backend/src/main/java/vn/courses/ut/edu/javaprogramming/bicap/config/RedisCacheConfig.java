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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Duration;

/**
 * Redis caching (BICAP-79 / SRS-API-008).
 *
 * <p><b>Production posture:</b> Redis is a hard dependency. If {@code app.cache.enabled=true}
 * (mặc định) mà Redis không kết nối được thì ứng dụng <b>không khởi động</b> — không còn
 * fallback cache in-memory ngầm. Muốn tắt cache một cách có ý thức (chỉ dùng cho test/CI)
 * thì đặt {@code app.cache.enabled=false}.
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

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Serializer JSON cho value trong cache (BICAP-79).
     *
     * <p>Bug đã sửa: {@code new GenericJackson2JsonRedisSerializer()} tự dựng một
     * {@code ObjectMapper} <b>không</b> đăng ký {@code JavaTimeModule}, nên mọi giá trị
     * cache chứa {@code LocalDate}/{@code LocalDateTime} (ví dụ
     * {@code MarketplaceProductResponse}, {@code CategoryResponse}) ném
     * {@code InvalidDefinitionException: Java 8 date/time type java.time.LocalDateTime
     * not supported by default} ngay tại bước cache PUT. Exception này thoát ra khỏi
     * controller, bị {@code GlobalExceptionHandler} bọc thành HTTP 500
     * "An unexpected error occurred" — đúng lỗi hiển thị trên trang Sàn nông sản.
     *
     * <p>Serializer mặc định vẫn được giữ (đã bật default typing) vì proxy
     * {@code @Cacheable} của Spring <b>cast</b> giá trị đọc từ Redis về đúng kiểu trả về
     * của method: thiếu type hint sẽ thành
     * {@code ClassCastException: LinkedHashMap cannot be cast to MarketplaceProductResponse}
     * ở lần cache HIT (lỗi đã tái hiện khi chạy app thật + Redis). Chỉ bổ sung
     * {@code JavaTimeModule} và ghi ngày tháng dạng ISO-8601.
     *
     * <p><b>Lưu ý cho cache mới:</b> giá trị cache là collection <i>immutable</i>
     * ({@code List.of()}, {@code Stream.toList()} → {@code ImmutableCollections$ListN},
     * class {@code final} thuộc {@code java.util}) sẽ không được ghi type hint, mà lúc
     * đọc lại {@code RedisSerializer} yêu cầu type hint cho kiểu {@code Object} →
     * {@code SerializationException} ở lần cache HIT. Vì vậy
     * {@code TradingFloorService.getCategories()} trả về {@code ArrayList} (xem
     * {@code RedisCacheConfigTest}).
     */
    static GenericJackson2JsonRedisSerializer cacheValueSerializer() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        serializer.configure(mapper -> {
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        });
        return serializer;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Tắt cache phải là quyết định có ý thức (chỉ dành cho test/CI) — không phải fallback ngầm.
        if (!cacheEnabled) {
            log.warn("Cache provider: IN-MEMORY vì app.cache.enabled=false (chỉ dùng cho test/CI — "
                    + "production phải để APP_CACHE_ENABLED=true và cấu hình Redis).");
            return new ConcurrentMapCacheManager(CACHE_CATEGORIES, CACHE_MARKETPLACE_DETAIL);
        }

        // Production: Redis là dependency bắt buộc. Không kết nối được ⇒ dừng khởi động,
        // thay vì âm thầm chạy bằng cache in-memory.
        if (!redisAvailable(connectionFactory)) {
            throw new IllegalStateException("Không kết nối được Redis tại " + redisHost + ":" + redisPort
                    + ". Production yêu cầu Redis cho cache (BICAP-79): kiểm tra SPRING_REDIS_HOST, "
                    + "SPRING_REDIS_PORT, SPRING_REDIS_PASSWORD (và SPRING_REDIS_SSL nếu server dùng TLS). "
                    + "Nếu thật sự muốn tắt cache, đặt APP_CACHE_ENABLED=false.");
        }

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttlSeconds))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(cacheValueSerializer()));
        log.info("Cache provider: REDIS {}:{} ({}), TTL {}s",
                redisHost, redisPort, connectionFactory.getClass().getSimpleName(), ttlSeconds);
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
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
