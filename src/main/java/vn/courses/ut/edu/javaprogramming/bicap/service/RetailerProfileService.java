package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.*;
import vn.courses.ut.edu.javaprogramming.bicap.entity.RetailerBusinessProfile;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RetailerBusinessProfileRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

@Service
@Transactional
public class RetailerProfileService {
    private final UserRepository userRepository;
    private final RetailerBusinessProfileRepository businessRepository;
    private final LocalFileStorageService fileStorage;

    public RetailerProfileService(UserRepository userRepository,
                                  RetailerBusinessProfileRepository businessRepository,
                                  LocalFileStorageService fileStorage) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional(readOnly = true)
    public RetailerProfileResponse getProfile() {
        return RetailerProfileResponse.from(requireRetailer());
    }

    public RetailerProfileResponse updateProfile(RetailerProfileRequest request) {
        User user = requireRetailer();
        String phone = request.getPhone().trim();
        if (userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new ConflictException("Phone number is already registered");
        }
        user.setFullName(request.getFullName().trim());
        user.setPhone(phone);
        user.setAddress(trimToNull(request.getAddress()));
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            user.setAvatarUrl(fileStorage.storeAvatar(user.getId(), request.getAvatar()));
        }
        return RetailerProfileResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public RetailerBusinessResponse getBusinessProfile() {
        User user = requireRetailer();
        return businessRepository.findByUserId(user.getId())
                .map(RetailerBusinessResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Business profile was not found"));
    }

    public RetailerBusinessResponse updateBusinessProfile(RetailerBusinessRequest request) {
        User user = requireRetailer();
        RetailerBusinessProfile profile = businessRepository.findByUserId(user.getId())
                .orElseGet(RetailerBusinessProfile::new);
        profile.setUser(user);
        profile.setBusinessName(request.getBusinessName().trim());
        profile.setAddress(request.getAddress().trim());
        profile.setBusinessType(request.getBusinessType());
        profile.setLicenseUrl(fileStorage.storeBusinessLicense(user.getId(), request.getLicense()));
        return RetailerBusinessResponse.from(businessRepository.save(profile));
    }

    private User requireRetailer() {
        User user = CurrentUser.get();
        boolean retailer = user.getRoles().stream()
                .anyMatch(role -> "RETAILER".equalsIgnoreCase(role.getName()));
        if (!retailer) throw new ForbiddenException("Retailer role is required");
        return user;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
