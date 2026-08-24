package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> getMyNotifications() {
        // Lấy thông tin xác thực an toàn từ SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            userId = user.getId();
        }

        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User user = CurrentUser.get();
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(user.getId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        User user = CurrentUser.get();
        return ResponseEntity.ok(notificationService.markAsRead(id, user.getId()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        User user = CurrentUser.get();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        User user = CurrentUser.get();
        return notificationService.subscribe(user.getId());
    }
}