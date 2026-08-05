package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {
    NotificationListResponse getUserNotifications(Long userId);
    Notification markAsRead(Long notificationId);
    
    // SSE Real-time methods
    SseEmitter subscribe(Long userId);
    void sendRealTimeAlert(Long userId, Notification notification);
}
