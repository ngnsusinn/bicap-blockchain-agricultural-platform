package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/admin/announcements} — a platform-wide announcement
 * shown to guests and to every signed-in user's notification feed (C-3 / BICAP-69).
 */
public class AnnouncementRequest {

    /** Optional category: ANNOUNCEMENT (default), PRODUCT, EDUCATION, EVENT. */
    @Size(max = 30)
    private String type;

    @NotBlank(message = "Title is required")
    @Size(max = 150)
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 4000)
    private String content;

    public AnnouncementRequest() {
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
