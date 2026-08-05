package vn.courses.ut.edu.javaprogramming.bicap.controller;

import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*") // Allows standalone frontend to connect easily
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<NotificationListResponse> getUserNotifications(@PathVariable Long userId) {
        NotificationListResponse response = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        Notification updated = notificationService.markAsRead(id);
        return ResponseEntity.ok(updated);
    }

    // SSE Endpoint for Real-time push notifications
    @GetMapping("/stream/{userId}")
    public SseEmitter stream(@PathVariable Long userId) {
        return notificationService.subscribe(userId);
    }
}
