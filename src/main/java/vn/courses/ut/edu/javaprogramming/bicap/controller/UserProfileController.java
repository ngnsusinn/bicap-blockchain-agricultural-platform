package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ChangePasswordRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.UpdateProfileRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.UserProfileResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.service.UserProfileService;

import java.util.Map;

/**
 * Controller for Farm Manager Profile Management (BICAP-8 / SRS-FM-002).
 */
@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * Fetch current user's profile info.
     */
    @GetMapping({"/api/profile", "/api/farm-manager/profile", "/api/users/profile"})
    public ResponseEntity<UserProfileResponse> getProfile() {
        User currentUser = CurrentUser.get();
        UserProfileResponse profile = userProfileService.getProfile(currentUser);
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user's profile info (Editable: Avatar, Full Name, Phone, Address).
     * Read-only fields (Email, Password, Role, Status, Created Date) are preserved.
     */
    @PutMapping({"/api/profile", "/api/farm-manager/profile", "/api/users/profile"})
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User currentUser = CurrentUser.get();
        UserProfileResponse updatedProfile = userProfileService.updateProfile(currentUser, request);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Đổi mật khẩu cho người dùng đang đăng nhập (Settings).
     */
    @PostMapping("/api/profile/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User currentUser = CurrentUser.get();
        userProfileService.changePassword(currentUser, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
