package vn.courses.ut.edu.javaprogramming.bicap.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.BroadcastNotificationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;

/**
 * In-app notification service backed by {@link NotificationRepository} for persistence
 * and SSE for real-time delivery.
 *
 * <p>SSE emitters are tracked per user (a list, so several browser tabs survive
 * simultaneously); a scheduled heartbeat keeps idle connections alive through proxies.
 * Emitters are in-memory only — after a restart clients reconnect on their own.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /** Constant for the in-app channel recorded on every persisted notification. */
    public static final String CHANNEL_IN_APP = "IN_APP";

    /** Event mở đầu stream — client biết kết nối đã được thiết lập (không phải thông báo). */
    public static final String CONNECTED_EVENT = "connected";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Active SSE connections, one emitter per open browser tab.
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getUserNotifications(Long userId) {
        List<Notification> notifications;

        // C-3 fix: unauthenticated guests only see platform-wide announcements
        // (is_system = true). The previous implementation returned every notification in
        // the database, exposing private farm/retailer/shipper messages.
        if (userId == null) {
            notifications = notificationRepository.findBySystemTrueOrderByCreatedAtDesc();
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
        // System announcements have no owner (userId == null) and cannot be marked read
        // per user — they are read-only for guests.
        if (notification.getUserId() == null || currentUserId == null
                || !notification.getUserId().equals(currentUserId)) {
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

        // Gửi ngay một event "connected" (được Spring buffer lại vì emitter chưa được
        // khởi tạo): giúp trình duyệt nhận header + mở kết nối ngay lập tức thay vì đợi
        // nhịp heartbeat 25s, và giúp phân biệt "stream đang sống" với "stream treo".
        try {
            emitter.send(SseEmitter.event().name(CONNECTED_EVENT).data("ok"));
        } catch (Exception e) {
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    @Override
    @Transactional
    public void sendNotification(Long userId, String type, String title, String content) {
        Notification saved = notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .channel(CHANNEL_IN_APP)
                .isRead(false)
                .build());

        sendToEmitters(userId, NotificationResponse.from(saved));
    }

    @Override
    @Transactional
    public NotificationResponse publishSystemAnnouncement(String type, String title, String content) {
        // A system announcement has no individual recipient — it is what unauthenticated
        // guests are allowed to read (C-3 / BICAP-69).
        Notification saved = notificationRepository.save(Notification.builder()
                .userId(null)
                .type(type == null || type.isBlank() ? "ANNOUNCEMENT" : type.trim().toUpperCase(Locale.ROOT))
                .title(title.trim())
                .content(content.trim())
                .channel(CHANNEL_IN_APP)
                .isRead(false)
                .system(true)
                .build());
        return NotificationResponse.from(saved);
    }

    @Override
    @Transactional
    public int broadcast(BroadcastNotificationRequest request) {
        User actor = CurrentUser.get();
        boolean shippingManager = actor.getRoles().stream()
                .anyMatch(role -> "SHIPPING_MGR".equalsIgnoreCase(role.getName()));
        if (!shippingManager) {
            throw new ForbiddenException("Only Shipping Managers can broadcast notifications");
        }

        Set<String> targets = switch (request.getTarget().trim().toUpperCase()) {
            case "FARM_MANAGER" -> Set.of("FARM_MANAGER");
            case "RETAILER" -> Set.of("RETAILER");
            case "BOTH", "ALL" -> Set.of("FARM_MANAGER", "RETAILER");
            default -> throw new BadRequestException("Target must be FARM_MANAGER, RETAILER, or BOTH");
        };
        List<User> recipients = userRepository.findDistinctByRoles_NameIn(targets).stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .toList();
        recipients.forEach(user -> sendNotification(
                user.getId(), "SHIPPING", request.getTitle().trim(), request.getContent().trim()));
        return recipients.size();
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
                // complete() để Spring đóng emitter hẳn, tránh ghi lại vào socket đã chết
                // ở nhịp heartbeat kế tiếp (mỗi lần ghi lỗi sinh một IOException lan ra
                // tầng async của servlet container).
                removeEmitter(userId, emitter);
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // emitter đã chết — không còn gì để dọn.
                }
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
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // emitter đã chết — không còn gì để dọn.
                }
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
}