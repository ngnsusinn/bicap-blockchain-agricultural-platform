package vn.courses.ut.edu.javaprogramming.bicap.service.impl;

import vn.courses.ut.edu.javaprogramming.bicap.dto.IotDataRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.IotData;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.IotDataRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.IotDataService;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class IotDataServiceImpl implements IotDataService {

    @Autowired
    private IotDataRepository iotDataRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

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

                Notification alert = new Notification();
                alert.setUserId(farm.getUserId());
                alert.setType("URGENT");
                alert.setTitle("Cảnh báo khẩn cấp IoT");
                alert.setContent(msg.toString());
                alert.setChannel("IN_APP_PUSH");
                alert.setIsRead(false);
                alert.setCreatedAt(LocalDateTime.now());
                
                Notification savedAlert = notificationRepository.save(alert);
                
                // Trigger Real-time push via SSE
                notificationService.sendRealTimeAlert(farm.getUserId(), savedAlert);
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
                
                Notification summary = new Notification();
                summary.setUserId(farm.getUserId());
                summary.setType("PERIODIC");
                summary.setTitle("Báo cáo IoT tổng hợp ngày");
                summary.setContent(String.format("Tổng kết ngày: Nhiệt độ TB %.1f°C, Độ ẩm TB %.1f%%, pH TB %.1f.", avgTemp, avgHumid, avgPh));
                summary.setChannel("IN_APP");
                summary.setIsRead(false);
                summary.setCreatedAt(LocalDateTime.now());
                
                Notification savedSummary = notificationRepository.save(summary);
                
                // Push summary via SSE as well (optional, but good UX)
                notificationService.sendRealTimeAlert(farm.getUserId(), savedSummary);
            }
        }
    }
}
