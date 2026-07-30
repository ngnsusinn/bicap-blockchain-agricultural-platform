package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDate;

public class SubscriptionResponse {
    private final Long id;
    private final Long farmId;
    private final String packageName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String status;

    public SubscriptionResponse(Long id, Long farmId, String packageName, LocalDate startDate, LocalDate endDate, String status) {
        this.id = id;
        this.farmId = farmId;
        this.packageName = packageName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getFarmId() { return farmId; }
    public String getPackageName() { return packageName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
}
