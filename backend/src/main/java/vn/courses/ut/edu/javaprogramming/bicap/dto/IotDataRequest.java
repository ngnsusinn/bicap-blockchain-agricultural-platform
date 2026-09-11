package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Sensor reading pushed by a farm's IoT gateway (BICAP-14 / SRS-FM-010).
 *
 * <p>C4 fix: the payload used to be accepted without any validation, so a missing field
 * crashed the service with an NPE / DB constraint error and out-of-range values were
 * persisted. The ranges below are the physical plausibility envelope of the sensors.
 */
public class IotDataRequest {

    @NotNull(message = "farmId is required")
    @Positive(message = "farmId must be a positive id")
    private Long farmId;

    @NotNull(message = "temperature is required")
    @DecimalMin(value = "-30.0", message = "temperature is below the supported range")
    @DecimalMax(value = "70.0", message = "temperature is above the supported range")
    private Double temperature;

    @NotNull(message = "humidity is required")
    @DecimalMin(value = "0.0", message = "humidity must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "humidity must be between 0 and 100")
    private Double humidity;

    @NotNull(message = "ph is required")
    @DecimalMin(value = "0.0", message = "pH must be between 0 and 14")
    @DecimalMax(value = "14.0", message = "pH must be between 0 and 14")
    private Double ph;

    public IotDataRequest() {}

    public Long getFarmId() { return farmId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Double getHumidity() { return humidity; }
    public void setHumidity(Double humidity) { this.humidity = humidity; }

    public Double getPh() { return ph; }
    public void setPh(Double ph) { this.ph = ph; }
}
