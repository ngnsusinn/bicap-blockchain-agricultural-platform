package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExportResponse {
    private Long id;
    private Long seasonId;
    private LocalDate exportDate;
    private Double quantity;
    private String destination;
    private String txHash;
    private LocalDateTime createdAt;

    public ExportResponse() {
    }

    public ExportResponse(Long id, Long seasonId, LocalDate exportDate, Double quantity, String destination, String txHash, LocalDateTime createdAt) {
        this.id = id;
        this.seasonId = seasonId;
        this.exportDate = exportDate;
        this.quantity = quantity;
        this.destination = destination;
        this.txHash = txHash;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSeasonId() { return seasonId; }
    public void setSeasonId(Long seasonId) { this.seasonId = seasonId; }

    public LocalDate getExportDate() { return exportDate; }
    public void setExportDate(LocalDate exportDate) { this.exportDate = exportDate; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long seasonId;
        private LocalDate exportDate;
        private Double quantity;
        private String destination;
        private String txHash;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder seasonId(Long seasonId) { this.seasonId = seasonId; return this; }
        public Builder exportDate(LocalDate exportDate) { this.exportDate = exportDate; return this; }
        public Builder quantity(Double quantity) { this.quantity = quantity; return this; }
        public Builder destination(String destination) { this.destination = destination; return this; }
        public Builder txHash(String txHash) { this.txHash = txHash; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ExportResponse build() {
            return new ExportResponse(id, seasonId, exportDate, quantity, destination, txHash, createdAt);
        }
    }
}
