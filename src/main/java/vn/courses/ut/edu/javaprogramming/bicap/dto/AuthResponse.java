package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final Long userId;
    private final String email;
    private final String phone;
    private final String fullName;
    private final Set<String> roles;
    private final boolean verificationRequired;

    public AuthResponse(
            String accessToken,
            String tokenType,
            Long userId,
            String email,
            String phone,
            String fullName,
            Set<String> roles) {
        this(accessToken, null, tokenType, userId, email, phone, fullName, roles, false);
    }

    public AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long userId,
            String email,
            String phone,
            String fullName,
            Set<String> roles,
            boolean verificationRequired) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.email = email;
        this.phone = phone;
        this.fullName = fullName;
        this.roles = roles;
        this.verificationRequired = verificationRequired;
    }

    public static AuthResponse fromUser(String accessToken, User user) {
        return fromUser(accessToken, null, user);
    }

    public static AuthResponse fromUser(String accessToken, String refreshToken, User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                roleNames,
                false
        );
    }

    public static AuthResponse pendingVerification(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        return new AuthResponse(
                null, null, "Bearer", user.getId(), user.getEmail(), user.getPhone(),
                user.getFullName(), roleNames, true
        );
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
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

    public boolean isVerificationRequired() {
        return verificationRequired;
    }
}
