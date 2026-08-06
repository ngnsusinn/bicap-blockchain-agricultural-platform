package vn.courses.ut.edu.javaprogramming.bicap.service.impl;

import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // Store active SSE connections
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public NotificationListResponse getUserNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new NotificationListResponse(unreadCount, notifications);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 means no timeout
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        return emitter;
    }

    @Override
    public void sendRealTimeAlert(Long userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (Exception e) {
                emitters.remove(userId);
            }
        }
    }
}
