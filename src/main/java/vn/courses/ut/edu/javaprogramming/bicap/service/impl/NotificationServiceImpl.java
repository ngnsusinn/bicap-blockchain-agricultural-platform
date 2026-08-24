package vn.courses.ut.edu.javaprogramming.bicap.service.impl;

import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import vn.courses.ut.edu.javaprogramming.bicap.service.VerificationEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-app notification service backed by {@link NotificationRepository} for persistence,
 * SSE for real-time delivery and {@link VerificationEmailService} for critical-event emails.
 *
 * <p>SSE emitters are tracked per user (a list, so several browser tabs survive
 * simultaneously); a scheduled heartbeat keeps idle connections alive through proxies.
 * Emitters are in-memory only — after a restart clients reconnect on their own.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    /** Constant for the in-app channel recorded on every persisted notification. */
    public static final String CHANNEL_IN_APP = "IN_APP";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final VerificationEmailService emailService;

    // Active SSE connections, one emitter per open browser tab.
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   VerificationEmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getUserNotifications(Long userId) {
        List<Notification> notifications;

        // [BICAP-69] Hỗ trợ Guest chưa đăng nhập: lấy tất cả thông báo hệ thống mới nhất
        if (userId == null) {
            notifications = notificationRepository.findAllByOrderByCreatedAtDesc();
            List<NotificationResponse> responses = notifications.stream()
                    .map(NotificationResponse::from)
                    .toList();
            return new NotificationListResponse(0, responses);
        }

        // Logic cũ khi đã đăng nhập (Farm Manager, Retailer, Admin...)
        notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationResponse::from)
                .toList();
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new NotificationListResponse(unreadCount, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("Cannot modify another user's notification");
        }
        notification.setIsRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 means no timeout
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        return emitter;
    }

    @Override
    @Transactional
    public void sendNotification(Long userId, String type, String title, String content, boolean sendEmail) {
        Notification saved = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .channel(CHANNEL_IN_APP)
                .isRead(false)
                .build());

        sendToEmitters(userId, NotificationResponse.from(saved));

        if (sendEmail) {
            sendAlertEmail(userId, title, content);
        }
    }

    /**
     * Heartbeat keeps idle SSE connections alive through proxies/load balancers
     * (event-comment frames carry no data).
     */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, list) -> list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception e) {
                // send() throws IllegalStateException too when the response is gone.
                removeEmitter(userId, emitter);
            }
        }));
    }

    private void sendToEmitters(Long userId, NotificationResponse notification) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }
        userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (Exception e) {
                removeEmitter(userId, emitter);
            }
        });
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId, userEmitters);
            }
        }
    }

    private void sendAlertEmail(Long userId, String title, String content) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Notification email skipped: user {} not found", userId);
            return;
        }
        try {
            emailService.sendNotificationEmail(user.getEmail(), title, content);
        } catch (RuntimeException ex) {
            // An SMTP outage must not roll back the persisted notification.
            log.error("Failed to send notification email to {}: {}", user.getEmail(), ex.getMessage());
        }
    }
}