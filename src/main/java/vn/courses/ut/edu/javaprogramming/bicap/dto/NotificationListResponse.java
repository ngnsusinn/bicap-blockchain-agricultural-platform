package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import java.util.List;

public class NotificationListResponse {
    private long unreadCount;
    private List<Notification> notifications;

    public NotificationListResponse() {}

    public NotificationListResponse(long unreadCount, List<Notification> notifications) {
        this.unreadCount = unreadCount;
        this.notifications = notifications;
    }

    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }

    public List<Notification> getNotifications() { return notifications; }
    public void setNotifications(List<Notification> notifications) { this.notifications = notifications; }
}
