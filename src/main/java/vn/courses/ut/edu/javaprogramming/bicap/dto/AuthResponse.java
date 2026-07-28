package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final Long userId;
    private final String email;
    private final String phone;
    private final String fullName;
    private final Set<String> roles;

    public AuthResponse(
            String accessToken,
            String tokenType,
            Long userId,
            String email,
            String phone,
            String fullName,
            Set<String> roles) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
        this.roles = roles;
    }

    public static AuthResponse fromUser(String accessToken, User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        return new AuthResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                roleNames
        );
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
