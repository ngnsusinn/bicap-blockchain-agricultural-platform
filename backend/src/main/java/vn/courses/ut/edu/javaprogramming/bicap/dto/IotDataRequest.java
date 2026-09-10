package vn.courses.ut.edu.javaprogramming.bicap.dto;

public class IotDataRequest {
    private Long farmId;
    private Double temperature;
    private Double humidity;
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
