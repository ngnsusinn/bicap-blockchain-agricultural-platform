package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.time.LocalDateTime;

/**
 * DTO returned for Farm Manager personal profile (BICAP-8).
 */
public class UserProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String email, String fullName, String phone, String address,
                               String avatarUrl, String role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
