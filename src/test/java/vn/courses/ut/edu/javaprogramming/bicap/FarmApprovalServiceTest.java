package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmApprovalRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.FarmApprovalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unused", "null"})
public class FarmApprovalServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private FarmCertificationRepository certificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private FarmApprovalService farmApprovalService;

    private User superAdmin;
    private User farmOwner;
    private Farm pendingFarm;

    @BeforeEach
    void setUp() {
        Role superAdminRole = Role.builder().id(1L).name("SUPER_ADMIN").permissions(new HashSet<>()).build();
        Role farmManagerRole = Role.builder().id(4L).name("FARM_MANAGER").permissions(new HashSet<>()).build();

        superAdmin = User.builder()
                .id(1L).email("super@bicap.com").password("Secret@2026")
                .fullName("Super Admin").status(UserStatus.ACTIVE).roles(Set.of(superAdminRole))
                .build();

        farmOwner = User.builder()
                .id(10L).email("farmer@bicap.com").password("Secret@2026")
                .fullName("Chủ Trang Trại").status(UserStatus.ACTIVE).roles(Set.of(farmManagerRole))
                .build();

        pendingFarm = Farm.builder()
                .id(100L).userId(10L).name("Trang Trại Xanh")
                .address("Đồng Nai").area(12.5)
                .gpsLat(10.8).gpsLng(107.0)
                .status(FarmStatus.PENDING)
                .build();
    }

    @Test
    void approveFarm_shouldSetStatusApproved_andNotifyOwner() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmResponse result = farmApprovalService.approveFarm(100L, "super@bicap.com");

        assertEquals(FarmStatus.APPROVED, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void rejectFarm_withoutReason_shouldThrowBadRequest() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));

        FarmApprovalRequest request = new FarmApprovalRequest("REJECT", "");

        assertThrows(BadRequestException.class,
                () -> farmApprovalService.rejectFarm(100L, request, "super@bicap.com"));
        verify(farmRepository, never()).save(any());
    }

    @Test
    void rejectFarm_withReason_shouldSetStatusRejected() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmApprovalRequest request = new FarmApprovalRequest("REJECT", "Thiếu giấy phép kinh doanh hợp lệ");
        FarmResponse result = farmApprovalService.rejectFarm(100L, request, "super@bicap.com");

        assertEquals(FarmStatus.REJECTED, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void approveFarm_alreadyApproved_shouldThrowBadRequest() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        Farm approvedFarm = Farm.builder()
                .id(101L).userId(10L).name("Đã Duyệt")
                .address("Hà Nội").area(8.0)
                .status(FarmStatus.APPROVED)
                .build();
        when(farmRepository.findById(101L)).thenReturn(Optional.of(approvedFarm));

        assertThrows(BadRequestException.class,
                () -> farmApprovalService.approveFarm(101L, "super@bicap.com"));
    }

    @Test
    void approveFarm_byNonAdmin_shouldThrowForbidden() {
        when(userRepository.findByEmail("farmer@bicap.com")).thenReturn(Optional.of(farmOwner));

        assertThrows(ForbiddenException.class,
                () -> farmApprovalService.approveFarm(100L, "farmer@bicap.com"));
    }

    @Test
    void approveFarm_unknownFarm_shouldThrowNotFound() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> farmApprovalService.approveFarm(999L, "super@bicap.com"));
    }

    @Test
    void getFarmDetail_shouldIncludeCertifications() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        FarmCertification cert = FarmCertification.builder()
                .id(1L).farmId(100L).type("VietGAP")
                .fileUrl("https://bicap.vn/docs/cert.pdf")
                .build();
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of(cert));

        FarmDetailResponse detail = farmApprovalService.getFarmDetail(100L, "super@bicap.com");

        assertEquals("Trang Trại Xanh", detail.getName());
        assertEquals(1, detail.getCertifications().size());
        assertEquals("VietGAP", detail.getCertifications().get(0).getType());
    }

    @Test
    void getFarmDetail_byNonAdmin_shouldThrowForbidden() {
        when(userRepository.findByEmail("farmer@bicap.com")).thenReturn(Optional.of(farmOwner));

        assertThrows(ForbiddenException.class,
                () -> farmApprovalService.getFarmDetail(100L, "farmer@bicap.com"));
    }
}
