package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BroadcastNotificationRequest {
    @NotBlank
    private String target;

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    @Size(min = 10, max = 4000)
    private String content;

    private boolean sendEmail;

    public BroadcastNotificationRequest() {
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(boolean sendEmail) {
        this.sendEmail = sendEmail;
    }
}
