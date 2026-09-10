package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.util.Map;

/**
 * Báo cáo tổng hợp quy trình vận chuyển của một nông trại
 * (BICAP-23 / SRS-FM-017). Đếm lô hàng theo trạng thái và tỷ lệ giao đúng hạn.
 */
public class FarmShipmentSummaryResponse {
    private Long farmId;
    private long total;
    private long pickingUp;
    private long inTransit;
    private long delivered;
    private long returned;
    private long onTimeDelivered;
    private long lateDelivered;

    /** Tỷ lệ giao đúng hạn (0–100), làm tròn 1 chữ số; 0 khi chưa có lô nào giao. */
    private double onTimeRatePercent;

    public FarmShipmentSummaryResponse() {}

    public static FarmShipmentSummaryResponse of(Long farmId, Map<String, Long> byStatus,
                                                 long onTime, long late) {
        FarmShipmentSummaryResponse r = new FarmShipmentSummaryResponse();
        r.farmId = farmId;
        r.pickingUp = byStatus.getOrDefault("PICKING_UP", 0L);
        r.inTransit = byStatus.getOrDefault("IN_TRANSIT", 0L);
        r.delivered = byStatus.getOrDefault("DELIVERED", 0L);
        r.returned = byStatus.getOrDefault("RETURNED", 0L);
        r.total = r.pickingUp + r.inTransit + r.delivered + r.returned;
        r.onTimeDelivered = onTime;
        r.lateDelivered = late;
        long deliveredTotal = onTime + late;
        r.onTimeRatePercent = deliveredTotal == 0 ? 0.0
                : Math.round((onTime * 1000.0) / deliveredTotal) / 10.0;
        return r;
    }

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getPickingUp() { return pickingUp; }
    public void setPickingUp(long pickingUp) { this.pickingUp = pickingUp; }
    public long getInTransit() { return inTransit; }
    public void setInTransit(long inTransit) { this.inTransit = inTransit; }
    public long getDelivered() { return delivered; }
    public void setDelivered(long delivered) { this.delivered = delivered; }
    public long getReturned() { return returned; }
    public void setReturned(long returned) { this.returned = returned; }
    public long getOnTimeDelivered() { return onTimeDelivered; }
    public void setOnTimeDelivered(long onTimeDelivered) { this.onTimeDelivered = onTimeDelivered; }
    public long getLateDelivered() { return lateDelivered; }
    public void setLateDelivered(long lateDelivered) { this.lateDelivered = lateDelivered; }
    public double getOnTimeRatePercent() { return onTimeRatePercent; }
    public void setOnTimeRatePercent(double onTimeRatePercent) { this.onTimeRatePercent = onTimeRatePercent; }
}
