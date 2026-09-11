package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * In-app notification for a user (BICAP-3 approval/rejection feedback, BICAP-77 notification module).
 * Maps to the `notifications` table.
 */
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Recipient. {@code null} marks a platform-wide announcement (guest-visible) rather
     * than a message addressed to one user — see {@link #system}.
     */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String channel;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * C-3 fix: only notifications flagged as system announcements are exposed to
     * unauthenticated guests. Previously the anonymous feed returned every user's
     * notifications, leaking private messages between farms and retailers.
     */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(Long id, Long userId, String type, String title, String content, String channel, Boolean isRead, LocalDateTime createdAt) {
        this(id, userId, type, title, content, channel, isRead, false, createdAt);
    }

    public Notification(Long id, Long userId, String type, String title, String content, String channel,
                        Boolean isRead, boolean system, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.channel = channel;
        this.isRead = isRead;
        this.system = system;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    public static class NotificationBuilder {
        private Long id;
        private Long userId;
        private String type;
        private String title;
        private String content;
        private String channel;
        private Boolean isRead;
        private boolean system = false;
        private LocalDateTime createdAt;

        NotificationBuilder() {}

        public NotificationBuilder id(Long id) { this.id = id; return this; }
        public NotificationBuilder userId(Long userId) { this.userId = userId; return this; }
        public NotificationBuilder type(String type) { this.type = type; return this; }
        public NotificationBuilder title(String title) { this.title = title; return this; }
        public NotificationBuilder content(String content) { this.content = content; return this; }
        public NotificationBuilder channel(String channel) { this.channel = channel; return this; }
        public NotificationBuilder isRead(Boolean isRead) { this.isRead = isRead; return this; }
        public NotificationBuilder system(boolean system) { this.system = system; return this; }
        public NotificationBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Notification build() {
            return new Notification(id, userId, type, title, content, channel, isRead, system, createdAt);
        }
    }
}
