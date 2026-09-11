package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Educational article/video published to the public (guest) area — F5 fix.
 *
 * <p>Previously the guest "Kiến thức nông nghiệp" screen rendered hard-coded mock arrays in
 * the browser with no backend at all. Content is now stored, served by a real API and
 * manageable in the database.
 */
@Entity
@Table(name = "educational_contents")
public class EducationalContent {

    public static final String TYPE_ARTICLE = "ARTICLE";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_DRAFT = "DRAFT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String type = TYPE_ARTICLE;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(length = 300)
    private String tags;

    @Column(nullable = false, length = 20)
    private String status = STATUS_DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public EducationalContent() {
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (publishedAt == null && STATUS_PUBLISHED.equals(status)) {
            publishedAt = now;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String title;
        private String summary;
        private String content;
        private String type = TYPE_ARTICLE;
        private String videoUrl;
        private String coverImageUrl;
        private String tags;
        private String status = STATUS_DRAFT;
        private LocalDateTime publishedAt;

        public Builder title(String title) { this.title = title; return this; }
        public Builder summary(String summary) { this.summary = summary; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder videoUrl(String videoUrl) { this.videoUrl = videoUrl; return this; }
        public Builder coverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; return this; }
        public Builder tags(String tags) { this.tags = tags; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }

        public EducationalContent build() {
            EducationalContent value = new EducationalContent();
            value.title = title;
            value.summary = summary;
            value.content = content;
            value.type = type;
            value.videoUrl = videoUrl;
            value.coverImageUrl = coverImageUrl;
            value.tags = tags;
            value.status = status;
            value.publishedAt = publishedAt;
            return value;
        }
    }
}
