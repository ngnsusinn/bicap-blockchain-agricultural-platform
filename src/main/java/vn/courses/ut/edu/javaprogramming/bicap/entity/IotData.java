package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "iot_data")
public class IotData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farm_id", nullable = false)
    private Long farmId;

    @Column(nullable = false)
    private Double temperature;

    @Column(nullable = false)
    private Double humidity;

    @Column(nullable = false)
    private Double ph;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    public IotData() {}

    public IotData(Long farmId, Double temperature, Double humidity, Double ph, LocalDateTime measuredAt) {
        this.farmId = farmId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ph = ph;
        this.measuredAt = measuredAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.measuredAt == null) {
            this.measuredAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }
    public Double getPh() { return ph; }
    public void setPh(Double ph) { this.ph = ph; }
    public LocalDateTime getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(LocalDateTime measuredAt) { this.measuredAt = measuredAt; }
}
