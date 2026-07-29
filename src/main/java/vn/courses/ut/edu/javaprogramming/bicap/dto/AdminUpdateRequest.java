package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class AdminUpdateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^(0\\d{9})?$", message = "Phone number must be a valid 10-digit Vietnamese number starting with 0")
    private String phone;

    private String role;

    private List<String> permissions;

    private String status;

    public AdminUpdateRequest() {}

    public AdminUpdateRequest(String fullName, String phone, String role, List<String> permissions, String status) {
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.permissions = permissions;
        this.status = status;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static AdminUpdateRequestBuilder builder() {
        return new AdminUpdateRequestBuilder();
    }

    public static class AdminUpdateRequestBuilder {
        private String fullName;
        private String phone;
        private String role;
        private List<String> permissions;
        private String status;

        AdminUpdateRequestBuilder() {}

        public AdminUpdateRequestBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public AdminUpdateRequestBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public AdminUpdateRequestBuilder role(String role) {
            this.role = role;
            return this;
        }

        public AdminUpdateRequestBuilder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public AdminUpdateRequestBuilder status(String status) {
            this.status = status;
            return this;
        }

        public AdminUpdateRequest build() {
            return new AdminUpdateRequest(fullName, phone, role, permissions, status);
        }
    }
}
