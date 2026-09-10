package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.dto.*;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RetailerBusinessProfileRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.LocalFileStorageService;
import vn.courses.ut.edu.javaprogramming.bicap.service.RetailerProfileService;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetailerProfileServiceTest {
    @Mock UserRepository userRepository;
    @Mock RetailerBusinessProfileRepository businessRepository;
    @Mock LocalFileStorageService fileStorage;
    @InjectMocks RetailerProfileService service;

    private User retailer;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(8L).name("RETAILER").permissions(Set.of()).build();
        retailer = User.builder()
                .id(10L).email("retailer@example.com").password("hashed")
                .fullName("Old Name").phone("0912345678")
                .status(UserStatus.ACTIVE).roles(Set.of(role)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(retailer, null, retailer.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateProfilePersistsRequiredFieldsAndAvatar() {
        RetailerProfileRequest request = new RetailerProfileRequest();
        request.setFullName("New Retailer");
        request.setPhone("0987654321");
        request.setAddress("  12 Nguyễn Huệ  ");
        request.setAvatar(new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[]{1}));
        when(fileStorage.storeAvatar(eq(10L), any())).thenReturn("/uploads/avatar.png");
        when(userRepository.save(retailer)).thenReturn(retailer);

        RetailerProfileResponse response = service.updateProfile(request);

        assertEquals("New Retailer", response.fullName());
        assertEquals("0987654321", response.phone());
        assertEquals("12 Nguyễn Huệ", response.address());
        assertEquals("/uploads/avatar.png", response.avatarUrl());
    }

    @Test
    void updateProfileRejectsPhoneOwnedByAnotherUser() {
        RetailerProfileRequest request = new RetailerProfileRequest();
        request.setFullName("Retailer");
        request.setPhone("0987654321");
        when(userRepository.existsByPhoneAndIdNot("0987654321", 10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.updateProfile(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateBusinessProfileStoresRequiredLicenseAndEnum() {
        RetailerBusinessRequest request = new RetailerBusinessRequest();
        request.setBusinessName("BICAP Mart");
        request.setAddress("Quận 1, TP.HCM");
        request.setBusinessType(BusinessType.SUPERMARKET);
        request.setLicense(new MockMultipartFile(
                "license", "license.pdf", "application/pdf", new byte[]{1}));
        when(businessRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(fileStorage.storeBusinessLicense(eq(10L), any())).thenReturn("/uploads/license.pdf");
        when(businessRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RetailerBusinessResponse response = service.updateBusinessProfile(request);

        assertEquals("BICAP Mart", response.businessName());
        assertEquals(BusinessType.SUPERMARKET, response.businessType());
        assertEquals("/uploads/license.pdf", response.licenseUrl());
    }

    @Test
    void getBusinessProfileReturnsExistingData() {
        RetailerBusinessProfile profile = new RetailerBusinessProfile();
        profile.setId(4L);
        profile.setUser(retailer);
        profile.setBusinessName("BICAP Store");
        profile.setAddress("Hà Nội");
        profile.setBusinessType(BusinessType.RETAIL_STORE);
        profile.setLicenseUrl("/uploads/license.png");
        when(businessRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        RetailerBusinessResponse response = service.getBusinessProfile();

        assertEquals(4L, response.id());
        assertEquals("BICAP Store", response.businessName());
    }
}
