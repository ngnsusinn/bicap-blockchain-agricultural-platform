package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmRegistrationService;
import vn.courses.ut.edu.javaprogramming.bicap.service.LocalFileStorageService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BICAP-9 / SRS-FM-003 — cập nhật thông tin nông trại và tải giấy phép/chứng nhận.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class FarmRegistrationServiceTest {

    @Mock FarmRepository farms;
    @Mock FarmCertificationRepository certs;
    @Mock LocalFileStorageService fileStorage;
    FarmRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new FarmRegistrationService(farms, certs, fileStorage);
        loginFarmManager(7L);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void loginFarmManager(Long userId) {
        Role role = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        User u = User.builder().id(userId).email("farm@bicap.vn").status(UserStatus.ACTIVE).roles(Set.of(role)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    private Farm ownedFarm() {
        return Farm.builder().id(2L).userId(7L).name("Farm Cu").address("Cu").area(100d).status(FarmStatus.APPROVED).build();
    }

    private FarmUpdateRequest updateReq(String name) {
        FarmUpdateRequest r = new FarmUpdateRequest();
        r.setName(name);
        r.setAddress("Đồng Nai");
        r.setArea(250d);
        r.setGpsLat(10.9);
        r.setGpsLng(106.8);
        r.setDescription("Vườn rau hữu cơ");
        r.setProductTypes("rau, củ");
        return r;
    }

    @Test
    void updateFarm_persistsNewDetails() {
        when(farms.findById(2L)).thenReturn(Optional.of(ownedFarm()));
        when(farms.findByName("Farm Mới")).thenReturn(Optional.empty());
        when(farms.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Farm result = service.updateFarm(2L, updateReq("Farm Mới"));

        assertEquals("Farm Mới", result.getName());
        assertEquals("Đồng Nai", result.getAddress());
        assertEquals(250d, result.getArea());
        assertEquals(FarmStatus.APPROVED, result.getStatus());
    }

    @Test
    void updateFarm_rejectsDuplicateName() {
        when(farms.findById(2L)).thenReturn(Optional.of(ownedFarm()));
        when(farms.findByName("Farm Khác")).thenReturn(Optional.of(
                Farm.builder().id(3L).userId(999L).name("Farm Khác").address("x").area(1d).build()));

        assertThrows(ConflictException.class, () -> service.updateFarm(2L, updateReq("Farm Khác")));
        verify(farms, never()).save(any());
    }

    @Test
    void updateFarm_rejectsFarmOfAnotherOwner() {
        when(farms.findById(2L)).thenReturn(Optional.of(
                Farm.builder().id(2L).userId(999L).name("Other").address("x").area(1d).build()));
        assertThrows(ForbiddenException.class, () -> service.updateFarm(2L, updateReq("Farm Mới")));
    }

    @Test
    void updateFarm_notFound() {
        when(farms.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.updateFarm(2L, updateReq("Farm Mới")));
    }

    @Test
    void addCertification_storesFileAndPersistsRecord() {
        when(farms.findById(2L)).thenReturn(Optional.of(ownedFarm()));
        when(fileStorage.storeBusinessLicense(eq(7L), any())).thenReturn("/uploads/farms/7/license.pdf");
        when(certs.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "license.pdf", "application/pdf", new byte[]{1, 2, 3});

        FarmCertification result = service.addCertification(2L, "BUSINESS_LICENSE", LocalDate.of(2027, 1, 1), file);

        assertEquals("BUSINESS_LICENSE", result.getType());
        assertEquals(2L, result.getFarmId());
        assertEquals("/uploads/farms/7/license.pdf", result.getFileUrl());
    }

    @Test
    void addCertification_requiresFile() {
        when(farms.findById(2L)).thenReturn(Optional.of(ownedFarm()));
        MockMultipartFile empty = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{});
        assertThrows(ConflictException.class, () -> service.addCertification(2L, "VIETGAP", null, empty));
        verify(certs, never()).save(any());
    }

    @Test
    void getCertifications_returnsFarmDocuments() {
        when(farms.findById(2L)).thenReturn(Optional.of(ownedFarm()));
        when(certs.findByFarmId(2L)).thenReturn(List.of(
                FarmCertification.builder().id(1L).farmId(2L).type("VIETGAP").fileUrl("/u/1.pdf").expiryDate(LocalDate.now()).build()));

        assertEquals(1, service.getCertifications(2L).size());
    }
}
