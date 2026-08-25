package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Retailer-facing marketplace payload for BICAP-39/40/41. */
public record MarketplaceProductResponse(
        Long id, String name, String description, List<String> images,
        BigDecimal price, Double quantity, String availability,
        Long categoryId, String categoryName,
        Long farmId, String farmName, String farmAddress,
        List<String> certifications,
        Long seasonId, String seasonName, String productType, String variety,
        LocalDate seasonStartDate, LocalDate harvestDate,
        Long exportId, String traceHash, String qrImage, String transactionHash,
        List<TraceProcess> processes, LocalDateTime createdAt) {

    public record TraceProcess(String processType, LocalDate executionDate,
                               String materials, String images, String notes, String transactionHash) {}
}
