package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ChangePasswordRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.UpdateProfileRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.UserProfileResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.util.List;

/**
 * Service for handling Farm Manager personal profile views and updates (BICAP-8).
 */
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(UserRepository userRepository, FarmRepository farmRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Gets profile details for the authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getId()));

        String address = user.getAddress();
        if (address == null || address.trim().isEmpty()) {
            List<Farm> farms = farmRepository.findByUserId(user.getId());
            if (!farms.isEmpty() && farms.get(0).getAddress() != null) {
                address = farms.get(0).getAddress();
            }
        }

        String primaryRole = user.getRoles() != null && !user.getRoles().isEmpty()
                ? user.getRoles().iterator().next().getName()
                : "FARM_MANAGER";

        String statusStr = user.getStatus() != null ? user.getStatus().name() : "ACTIVE";

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                address,
                user.getAvatarUrl(),
                primaryRole,
                statusStr,
                user.getCreatedAt()
        );
    }

    /**
     * Updates editable fields of current user profile (Avatar, Full Name, Phone Number, Address).
     * Read-only fields (Email, Password, Role, Status, Created Date) are preserved.
     */
    @Transactional
    public UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getId()));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());

            // Synchronize with user's farm address if applicable
            List<Farm> farms = farmRepository.findByUserId(user.getId());
            if (!farms.isEmpty()) {
                for (Farm farm : farms) {
                    farm.setAddress(request.getAddress().trim());
                    farmRepository.save(farm);
                }
            }
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User savedUser = userRepository.save(user);

        String primaryRole = savedUser.getRoles() != null && !savedUser.getRoles().isEmpty()
                ? savedUser.getRoles().iterator().next().getName()
                : "FARM_MANAGER";

        String statusStr = savedUser.getStatus() != null ? savedUser.getStatus().name() : "ACTIVE";

        return new UserProfileResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getPhone(),
                savedUser.getAddress(),
                savedUser.getAvatarUrl(),
                primaryRole,
                statusStr,
                savedUser.getCreatedAt()
        );
    }

    /**
     * Đổi mật khẩu cho người dùng đang đăng nhập (Settings). Yêu cầu mật khẩu hiện tại
     * đúng và mật khẩu mới khớp xác nhận.
     */
    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getId()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password confirmation does not match");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
