package vn.courses.ut.edu.javaprogramming.bicap.service.impl;

import vn.courses.ut.edu.javaprogramming.bicap.dto.IotDataRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.IotData;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.IotDataRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.IotDataService;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class IotDataServiceImpl implements IotDataService {

    private final IotDataRepository iotDataRepository;
    private final FarmRepository farmRepository;
    private final NotificationService notificationService;

    public IotDataServiceImpl(IotDataRepository iotDataRepository,
                              FarmRepository farmRepository,
                              NotificationService notificationService) {
        this.iotDataRepository = iotDataRepository;
        this.farmRepository = farmRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public IotData saveAndCheckThresholds(IotDataRequest request) {
        IotData data = new IotData();
        data.setFarmId(request.getFarmId());
        data.setTemperature(request.getTemperature());
        data.setHumidity(request.getHumidity());
        data.setPh(request.getPh());
        data.setMeasuredAt(LocalDateTime.now());
        
        IotData saved = iotDataRepository.save(data);

        // Check thresholds
        boolean tempIssue = request.getTemperature() < 15 || request.getTemperature() > 40;
        boolean humidIssue = request.getHumidity() < 30 || request.getHumidity() > 90;
        boolean phIssue = request.getPh() < 5.5 || request.getPh() > 7.5;

        if (tempIssue || humidIssue || phIssue) {
            Farm farm = farmRepository.findById(request.getFarmId()).orElse(null);
            if (farm != null) {
                StringBuilder msg = new StringBuilder("Cảnh báo khẩn cấp từ cảm biến: ");
                if (tempIssue) msg.append(String.format("Nhiệt độ bất thường (%.1f°C). ", request.getTemperature()));
                if (humidIssue) msg.append(String.format("Độ ẩm bất thường (%.1f%%). ", request.getHumidity()));
                if (phIssue) msg.append(String.format("Độ pH bất thường (%.1f). ", request.getPh()));

                // Persist, push to the live SSE stream and email the farm owner.
                notificationService.sendNotification(farm.getUserId(), "URGENT", "Cảnh báo khẩn cấp IoT", msg.toString(), true);
            }
        }
        return saved;
    }

    @Override
    @Scheduled(cron = "0 59 23 * * ?")
    public void generateDailySummary() {
        List<Farm> farms = farmRepository.findAll();
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        
        for (Farm farm : farms) {
            List<IotData> dailyData = iotDataRepository.findByFarmIdAndMeasuredAtBetween(farm.getId(), startOfDay, endOfDay);
            if (!dailyData.isEmpty()) {
                double avgTemp = dailyData.stream().mapToDouble(IotData::getTemperature).average().orElse(0.0);
                double avgHumid = dailyData.stream().mapToDouble(IotData::getHumidity).average().orElse(0.0);
                double avgPh = dailyData.stream().mapToDouble(IotData::getPh).average().orElse(0.0);

                notificationService.sendNotification(farm.getUserId(), "PERIODIC", "Báo cáo IoT tổng hợp ngày",
                        String.format("Tổng kết ngày: Nhiệt độ TB %.1f°C, Độ ẩm TB %.1f%%, pH TB %.1f.", avgTemp, avgHumid, avgPh),
                        false);
            }
        }
    }
}
