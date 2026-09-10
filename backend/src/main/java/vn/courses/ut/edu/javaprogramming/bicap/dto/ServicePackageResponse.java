package vn.courses.ut.edu.javaprogramming.bicap.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import java.math.BigDecimal;

public class ServicePackageResponse {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final int durationDays;
    private final String features;
    private final String status;

    public ServicePackageResponse(Long id, String name, String description, BigDecimal price, int durationDays, String features, String status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.durationDays = durationDays;
        this.features = features;
        this.status = status;
    }

    public static ServicePackageResponse fromEntity(ServicePackage pkg) {
        return new ServicePackageResponse(
            pkg.getId(),
            pkg.getName(),
            pkg.getDescription(),
            pkg.getPrice(),
            pkg.getDurationDays(),
            normalizeFeatures(pkg.getFeatures()),
            pkg.getStatus()
        );
    }

    /**
     * Returns {@code features} as a JSON array string.
     *
     * <p>{@code ServicePackage.features} is a plain String mapped to a {@code json}
     * column. MySQL stores/returns the JSON text as-is, but H2 (the local dev
     * default) stores the Java String as a <em>JSON string scalar</em> and returns
     * it quoted and escaped — i.e. {@code "[\"A\",\"B\"]"} instead of
     * {@code ["A","B"]}. Clients that do a single {@code JSON.parse} then receive a
     * String instead of an array and blow up on {@code .map(...)}.
     *
     * <p>Unwrap that extra string layer (idempotent) so every client gets the same
     * shape regardless of the database in use.
     */
    static String normalizeFeatures(String raw) {
        if (raw == null || raw.isBlank()) return "[]";
        String value = raw.trim();
        for (int i = 0; i < 3; i++) {
            if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') break;
            try {
                JsonNode node = MAPPER.readTree(value);
                if (!node.isTextual()) break;
                value = node.asText().trim();
            } catch (Exception e) {
                break;
            }
        }
        return value;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public int getDurationDays() { return durationDays; }
    public String getFeatures() { return features; }
    public String getStatus() { return status; }
}
