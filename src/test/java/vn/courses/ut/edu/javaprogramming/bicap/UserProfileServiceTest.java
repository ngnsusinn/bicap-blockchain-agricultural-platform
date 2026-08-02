package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.courses.ut.edu.javaprogramming.bicap.dto.UpdateProfileRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.UserProfileResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.UserProfileService;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        Role farmRole = Role.builder().name("FARM_MANAGER").description("Farm Manager").build();
        sampleUser = User.builder()
                .id(10L)
                .email("farmmanager@bicap.com")
                .password("encoded_password")
                .fullName("Chủ Trang Trại Cu Cũ")
                .phone("0912345678")
                .avatarUrl("https://example.com/old-avatar.png")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(farmRole))
                .build();
    }

    @Test
    @DisplayName("BICAP-8: Lấy thông tin hồ sơ cá nhân thành công")
    void getProfile_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(farmRepository.findByUserId(10L)).thenReturn(Collections.emptyList());

        UserProfileResponse response = userProfileService.getProfile(sampleUser);

        assertNotNull(response);
        assertEquals("farmmanager@bicap.com", response.getEmail());
        assertEquals("Chủ Trang Trại Cu Cũ", response.getFullName());
        assertEquals("0912345678", response.getPhone());
        assertEquals("FARM_MANAGER", response.getRole());
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    @DisplayName("BICAP-8: Cập nhật thông tin cá nhân (FullName, Phone, Address, Avatar) thành công")
    void updateProfile_Success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Chủ Trang Trại Mới",
                "0988776655",
                "Long An, Việt Nam",
                "https://example.com/new-avatar.png"
        );

        UserProfileResponse response = userProfileService.updateProfile(sampleUser, request);

        assertNotNull(response);
        assertEquals("Chủ Trang Trại Mới", response.getFullName());
        assertEquals("0988776655", response.getPhone());
        assertEquals("Long An, Việt Nam", response.getAddress());
        assertEquals("https://example.com/new-avatar.png", response.getAvatarUrl());

        // Verify read-only fields remain intact
        assertEquals("farmmanager@bicap.com", response.getEmail());
        assertEquals("FARM_MANAGER", response.getRole());
        assertEquals("ACTIVE", response.getStatus());

        verify(userRepository, times(1)).save(sampleUser);
    }
}
