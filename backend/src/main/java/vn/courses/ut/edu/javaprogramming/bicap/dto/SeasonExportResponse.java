package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Season export receipt.
 *
 * <p>{@code chainMode} is {@code LIVE} when the receipt was actually broadcast to
 * VeChainThor and {@code MOCK} when it is a simulated development receipt — the UI uses it
 * to avoid claiming an on-chain record that does not exist (F1).
 */
public record SeasonExportResponse(Long id, Long farmId, Long seasonId, BigDecimal quantity,
        String unit, LocalDate exportDate, String warehouse, String status,
        String transactionHash, String traceHash, String chainMode, String qrImage,
        LocalDateTime createdAt) {
    public static SeasonExportResponse from(SeasonExport value) {
        return new SeasonExportResponse(value.getId(), value.getFarmId(), value.getSeasonId(),
                value.getQuantity(), value.getUnit(), value.getExportDate(), value.getWarehouse(),
                value.getStatus().name(), value.getTransactionHash(), value.getTraceHash(),
                value.getChainMode(), value.getQrImage(), value.getCreatedAt());
    }
}
