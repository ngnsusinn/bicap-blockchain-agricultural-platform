package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-app notification service (BICAP-77 / SRS-API-006).
 *
 * <p>Read + write endpoints are resolved against the authenticated user via
 * {@link vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser}, so a user
 * can only ever see or mutate their own notifications. Other domain services (IoT, order,
 * farm approval, ...) call {@link #sendNotification} to fan out an event to a user's
 * in-app inbox and, for critical events, an email.
 */
public interface NotificationService {

    /** The current user's notifications (newest first) plus the unread badge count. */
    NotificationListResponse getUserNotifications(Long userId);

    /** Total unread notifications for the user (badge count). */
    long getUnreadCount(Long userId);

    /**
     * Marks one notification read — only allowed for its owner.
     *
     * @throws vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException
     *         when the notification does not exist
     * @throws vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException
     *         when the caller is not the notification's owner
     */
    NotificationResponse markAsRead(Long notificationId, Long currentUserId);

    /** Marks all of the user's unread notifications as read. */
    void markAllAsRead(Long userId);

    // SSE Real-time methods

    /** Registers a long-lived SSE connection for the user's real-time stream. */
    SseEmitter subscribe(Long userId);

    /**
     * Persists a notification, pushes it to the user's live SSE stream and, when
     * {@code sendEmail} is set, also emails the user.
     */
    void sendNotification(Long userId, String type, String title, String content, boolean sendEmail);
}
