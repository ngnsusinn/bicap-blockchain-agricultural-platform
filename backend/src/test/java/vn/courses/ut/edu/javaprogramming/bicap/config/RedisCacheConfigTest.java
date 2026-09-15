package vn.courses.ut.edu.javaprogramming.bicap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import vn.courses.ut.edu.javaprogramming.bicap.dto.CategoryResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.MarketplaceProductResponse;

/**
 * BICAP-79 — regression test cho lỗi HTTP 500 "An unexpected error occurred" của
 * trang Sàn nông sản (redis cache).
 *
 * <p>Hai lỗi được khoá lại ở đây:
 * <ol>
 *   <li>Cache PUT: serializer mặc định không có {@code JavaTimeModule} nên ném
 *       {@code InvalidDefinitionException} với mọi DTO chứa {@code LocalDate}/
 *       {@code LocalDateTime} ({@code MarketplaceService.detail/detailPublic},
 *       {@code TradingFloorService.getCategories}).</li>
 *   <li>Cache HIT: proxy {@code @Cacheable} của Spring cast giá trị đọc từ Redis về
 *       kiểu trả về của method, nên value phải được ghi kèm type hint — thiếu type hint
 *       sẽ là {@code ClassCastException: LinkedHashMap cannot be cast to
 *       MarketplaceProductResponse} (đã tái hiện bằng cách chạy app thật + Redis).</li>
 * </ol>
 *
 * <p>Test chạy trên serializer thật của {@link RedisCacheConfig} nên không cần Redis
 * server (CI chạy H2 + cache in-memory vẫn pass).
 */
class RedisCacheConfigTest {

    private static MarketplaceProductResponse marketplaceProduct() {
        return new MarketplaceProductResponse(
                1L, "Cà chua hữu cơ BICAP", "Cà chua trồng theo tiêu chuẩn VietGAP",
                List.of("/uploads/ca-chua.jpg"),
                new BigDecimal("15000.00"), 120d, "AVAILABLE",
                2L, "Rau củ",
                9L, "Trang Trại Hữu Cơ Sông Hồng", "Xã Dan Phượng, Hà Nội",
                List.of("VietGAP", "Organic"),
                3L, "Vụ Đông 2026", "Cà chua", "Savior",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20),
                7L, "0x" + "a".repeat(64), "/uploads/qr-7.png", "0x" + "b".repeat(64),
                List.of(new MarketplaceProductResponse.TraceProcess("GIEO_TRONG",
                        LocalDate.of(2026, 9, 2), "Hạt giống F1", null, "Gieo vụ mới", null)),
                LocalDateTime.of(2026, 9, 14, 11, 54));
    }

    /**
     * Danh mục y như {@code TradingFloorService.getCategories()} trả về.
     *
     * <p>{@code Stream.toList()} trả về {@code ImmutableCollections$ListN} (class
     * {@code final} thuộc {@code java.util}) nên <b>không</b> được ghi type hint và sẽ
     * hỏng ở lần cache HIT — đó là lý do service trả về {@code new ArrayList<>(...)}.
     */
    private static List<CategoryResponse> categoryList() {
        return new ArrayList<>(Stream.of(new CategoryResponse(2L, "Rau củ", "Rau ăn lá", "🥬", 5L,
                LocalDateTime.of(2026, 1, 2, 3, 4))).toList());
    }

    @Test
    @DisplayName("Cache PUT không còn ném lỗi với DTO chứa LocalDate/LocalDateTime")
    void cacheValueSerializer_serializesJavaTimeValues() {
        GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheValueSerializer();

        assertThatCode(() -> serializer.serialize(marketplaceProduct())).doesNotThrowAnyException();
        assertThatCode(() -> serializer.serialize(categoryList())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cache HIT trả lại đúng kiểu MarketplaceProductResponse (proxy @Cacheable cast được)")
    void cacheValueSerializer_restoresDeclaredTypeOnCacheHit() {
        GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheValueSerializer();
        MarketplaceProductResponse original = marketplaceProduct();

        Object restored = serializer.deserialize(serializer.serialize(original));

        // Đây chính là cast mà CGLIB cache proxy thực hiện ở lần cache HIT.
        assertThat(restored).isInstanceOf(MarketplaceProductResponse.class);
        assertThat((MarketplaceProductResponse) restored).isEqualTo(original);
    }

    /** ObjectMapper giống cấu hình HTTP của Spring MVC để kiểm tra JSON trả về client. */
    private static ObjectMapper httpMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Test
    @DisplayName("Cache HIT với List<CategoryResponse> (cache bicapCategories) đọc lại được")
    void cacheValueSerializer_roundTripsCategoryList() throws Exception {
        GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheValueSerializer();

        Object restored = serializer.deserialize(serializer.serialize(categoryList()));

        // Proxy @Cacheable cast về List (erased) nên chỉ cần đọc được, không cần đúng element type.
        assertThat(restored).isInstanceOf(List.class);
        JsonNode json = httpMapper().readTree(httpMapper().writeValueAsBytes(restored));
        assertThat(json).hasSize(1);
        assertThat(json.get(0).get("name").asText()).isEqualTo("Rau củ");
        assertThat(json.get(0).get("productCount").asLong()).isEqualTo(5L);
        assertThat(json.get(0).get("createdAt").asText()).isEqualTo("2026-01-02T03:04:00");
    }

    @Test
    @DisplayName("Collection immutable (List.of/Stream.toList) KHÔNG được cache — canary cho TradingFloorService")
    void immutableListValue_cannotBeReadBackFromCache() {
        GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheValueSerializer();
        List<CategoryResponse> immutable = Stream.of(new CategoryResponse(2L, "Rau củ", null, null, 0L, null)).toList();

        byte[] stored = serializer.serialize(immutable);

        // Vì sao TradingFloorService.getCategories() phải trả về ArrayList:
        assertThatThrownBy(() -> serializer.deserialize(stored)).isInstanceOf(SerializationException.class);
    }

    @Test
    @DisplayName("Ngày tháng lưu dạng ISO-8601 trong Redis (không phải mảng số)")
    void cacheValueSerializer_storesTimestampsAsIsoStrings() throws Exception {
        GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheValueSerializer();

        String stored = new String(serializer.serialize(marketplaceProduct()), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(stored).contains("\"createdAt\":\"2026-09-14T11:54:00\"")
                .contains("\"seasonStartDate\":\"2026-09-01\"");
    }

    @Test
    @DisplayName("Serializer mặc định (không JavaTimeModule) là nguyên nhân gây 500 — canary chống hồi quy")
    void defaultSerializer_withoutJavaTimeModule_failsOnJavaTimeValues() {
        GenericJackson2JsonRedisSerializer defaultSerializer = new GenericJackson2JsonRedisSerializer();

        assertThatThrownBy(() -> defaultSerializer.serialize(marketplaceProduct()))
                .isInstanceOf(SerializationException.class)
                .hasMessageContaining("Java 8 date/time");
    }

    @Test
    @DisplayName("HTTP mapper (Spring MVC) vẫn serialize ra đúng JSON như trước")
    void cacheHitJsonMatchesHttpOutput() throws Exception {
        GenericJackson2JsonRedisSerializer serializer = RedisCacheConfig.cacheValueSerializer();

        MarketplaceProductResponse original = marketplaceProduct();
        MarketplaceProductResponse restored =
                (MarketplaceProductResponse) serializer.deserialize(serializer.serialize(original));

        assertThat(httpMapper().writeValueAsString(restored)).isEqualTo(httpMapper().writeValueAsString(original));
    }
}
