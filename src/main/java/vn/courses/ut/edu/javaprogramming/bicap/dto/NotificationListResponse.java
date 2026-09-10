package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.util.List;

/**
 * Payload for {@code GET /api/notifications} — the current user's notifications
 * newest-first together with the total unread count for the badge.
 */
public class NotificationListResponse {
    private long unreadCount;
    private List<NotificationResponse> notifications;

    public NotificationListResponse() {
    }

    public NotificationListResponse(long unreadCount, List<NotificationResponse> notifications) {
        this.unreadCount = unreadCount;
        this.notifications = notifications;
    }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public List<NotificationResponse> getNotifications() { return notifications; }
    public void setNotifications(List<NotificationResponse> notifications) { this.notifications = notifications; }
}
