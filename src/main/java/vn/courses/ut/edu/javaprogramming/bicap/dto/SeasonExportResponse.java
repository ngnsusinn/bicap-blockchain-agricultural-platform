package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SeasonExportResponse(Long id, Long farmId, Long seasonId, BigDecimal quantity,
        String unit, LocalDate exportDate, String warehouse, String status,
        String transactionHash, String traceHash, String qrImage, LocalDateTime createdAt) {
    public static SeasonExportResponse from(SeasonExport value) {
        return new SeasonExportResponse(value.getId(), value.getFarmId(), value.getSeasonId(),
                value.getQuantity(), value.getUnit(), value.getExportDate(), value.getWarehouse(),
                value.getStatus().name(), value.getTransactionHash(), value.getTraceHash(),
                value.getQrImage(), value.getCreatedAt());
    }
}
