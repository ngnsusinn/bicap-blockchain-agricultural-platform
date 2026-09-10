package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SeasonDetailResponse extends SeasonResponse {
    
    private List<ProcessResponse> processes;
    private List<ExportResponse> exports;
    private String farmName;

    public SeasonDetailResponse() {
        super();
    }

    public SeasonDetailResponse(Long id, Long farmId, String name, String productType, String variety, Double area, LocalDate startDate, LocalDate endDate, String status, String txHash, LocalDateTime createdAt, List<ProcessResponse> processes, List<ExportResponse> exports, String farmName) {
        super(id, farmId, name, productType, variety, area, startDate, endDate, status, txHash, createdAt);
        this.processes = processes;
        this.exports = exports;
        this.farmName = farmName;
    }

    public List<ProcessResponse> getProcesses() {
        return processes;
    }

    public void setProcesses(List<ProcessResponse> processes) {
        this.processes = processes;
    }

    public List<ExportResponse> getExports() {
        return exports;
    }

    public void setExports(List<ExportResponse> exports) {
        this.exports = exports;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }
}
