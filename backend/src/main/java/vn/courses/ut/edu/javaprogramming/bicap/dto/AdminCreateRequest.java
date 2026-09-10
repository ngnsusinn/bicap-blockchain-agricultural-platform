package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class AdminCreateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^()+=.-])[A-Za-z\\d@$!%*?&_#^()+=.-]{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String password;

    @Pattern(regexp = "^(0\\d{9})?$", message = "Phone number must be a valid 10-digit Vietnamese number starting with 0")
    private String phone;

    private String role;

    private List<String> permissions;

    private String status;

    public AdminCreateRequest() {}

    public AdminCreateRequest(String fullName, String email, String password, String phone, String role, List<String> permissions, String status) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public static AdminCreateRequestBuilder builder() {
        return new AdminCreateRequestBuilder();
    }

    public static class AdminCreateRequestBuilder {
        private String fullName;
        private String email;
        private String password;
        private String phone;
        private String role;
        private List<String> permissions;
        private String status;

        AdminCreateRequestBuilder() {}

        public AdminCreateRequestBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public AdminCreateRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public AdminCreateRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public AdminCreateRequestBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public AdminCreateRequestBuilder role(String role) {
            this.role = role;
            return this;
        }

        public AdminCreateRequestBuilder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        public AdminCreateRequestBuilder status(String status) {
            this.status = status;
            return this;
        }

        public AdminCreateRequest build() {
            return new AdminCreateRequest(fullName, email, password, phone, role, permissions, status);
        }
    }
}
