package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmApprovalRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmNotesUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmStatusUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unused", "null"})
public class FarmApprovalServiceTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private FarmCertificationRepository certificationRepository;

    @Mock
    private vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository seasonRepository;

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
    void getFarmDetail_shouldIncludeSeasonHistory() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of());
        when(seasonRepository.findByFarmId(100L)).thenReturn(List.of(
                new FarmingSeason(9L, 100L, "Vụ Rau 2026", "Rau ăn lá", "Cải xanh",
                        5.0, java.time.LocalDate.of(2026, 1, 10), null, "HARVESTED", "0xtx", null)));

        FarmDetailResponse detail = farmApprovalService.getFarmDetail(100L, "super@bicap.com");

        assertNotNull(detail.getSeasons());
        assertEquals(1, detail.getSeasons().size());
        assertEquals("Vụ Rau 2026", detail.getSeasons().get(0).getName());
        assertEquals("HARVESTED", detail.getSeasons().get(0).getStatus());
    }

    @Test
    void getFarmDetail_byNonAdmin_shouldThrowForbidden() {
        when(userRepository.findByEmail("farmer@bicap.com")).thenReturn(Optional.of(farmOwner));

        assertThrows(ForbiddenException.class,
                () -> farmApprovalService.getFarmDetail(100L, "farmer@bicap.com"));
    }

    @Test
    void getFarms_shouldBatchLoadOwnersAndCertCounts() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        Page<Farm> page = new PageImpl<>(List.of(pendingFarm), PageRequest.of(0, 10), 1);
        when(farmRepository.findFarmsFiltered(any(), any(), any())).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(farmOwner));
        FarmCertification cert = FarmCertification.builder()
                .id(1L).farmId(100L).type("VietGAP")
                .fileUrl("https://bicap.vn/docs/cert.pdf")
                .build();
        when(certificationRepository.findByFarmIdIn(any())).thenReturn(List.of(cert));

        Page<FarmResponse> result = farmApprovalService.getFarms(null, null, PageRequest.of(0, 10), "super@bicap.com");

        assertEquals(1, result.getTotalElements());
        assertEquals("Trang Trại Xanh", result.getContent().get(0).getName());
        assertEquals("Chủ Trang Trại", result.getContent().get(0).getOwnerName());
        assertEquals(1, result.getContent().get(0).getCertificationCount());

        // Batched — exactly one owner query and one cert query for the whole page, never per-farm
        verify(userRepository, times(1)).findAllById(any());
        verify(certificationRepository, times(1)).findByFarmIdIn(any());
        verify(certificationRepository, never()).findByFarmId(anyLong());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getFarmDetail_shouldLoadCertsOnce() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        FarmCertification cert = FarmCertification.builder()
                .id(1L).farmId(100L).type("VietGAP")
                .fileUrl("https://bicap.vn/docs/cert.pdf")
                .build();
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of(cert));

        FarmDetailResponse detail = farmApprovalService.getFarmDetail(100L, "super@bicap.com");

        assertEquals(1, detail.getCertificationCount());
        assertEquals(1, detail.getCertifications().size());
        // The same cert query feeds both count and list — only one call
        verify(certificationRepository, times(1)).findByFarmId(100L);
    }

    // ── Farm management (BICAP-4 / SRS-ADM-003) ──

    private Farm approvedFarm;

    private Farm approvedFarmEntity() {
        if (approvedFarm == null) {
            approvedFarm = Farm.builder()
                    .id(101L).userId(10L).name("Đã Duyệt")
                    .address("Hà Nội").area(8.0)
                    .status(FarmStatus.APPROVED)
                    .build();
        }
        return approvedFarm;
    }

    @Test
    void updateStatus_shouldChangeOperatingStatus() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(101L)).thenReturn(Optional.of(approvedFarmEntity()));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(101L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("SUSPENDED");
        FarmResponse result = farmApprovalService.updateStatus(101L, request, "super@bicap.com");

        assertEquals(FarmStatus.SUSPENDED, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void updateStatus_suspendedToApproved_shouldReactivate() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        Farm suspendedFarm = Farm.builder()
                .id(102L).userId(10L).name("Tạm Ngưng")
                .address("Hà Nội").area(8.0)
                .status(FarmStatus.SUSPENDED)
                .build();
        when(farmRepository.findById(102L)).thenReturn(Optional.of(suspendedFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(102L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("APPROVED");
        FarmResponse result = farmApprovalService.updateStatus(102L, request, "super@bicap.com");

        assertEquals(FarmStatus.APPROVED, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void updateStatus_onPendingFarm_shouldThrowBadRequest() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));

        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("APPROVED");
        assertThrows(BadRequestException.class,
                () -> farmApprovalService.updateStatus(100L, request, "super@bicap.com"));
        verify(farmRepository, never()).save(any());
    }

    @Test
    void updateStatus_onRejectedFarm_shouldThrowBadRequest() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        Farm rejectedFarm = Farm.builder()
                .id(103L).userId(10L).name("Bị Từ Chối")
                .address("Tiền Giang").area(15.0)
                .status(FarmStatus.REJECTED)
                .build();
        when(farmRepository.findById(103L)).thenReturn(Optional.of(rejectedFarm));

        // A REJECTED farm may only resubmit to PENDING — APPROVED directly is blocked.
        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("APPROVED");
        assertThrows(BadRequestException.class,
                () -> farmApprovalService.updateStatus(103L, request, "super@bicap.com"));
        verify(farmRepository, never()).save(any());
    }

    @Test
    void updateStatus_rejectedToPending_shouldAllowResubmit() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        Farm rejectedFarm = Farm.builder()
                .id(103L).userId(10L).name("Bị Từ Chối")
                .address("Tiền Giang").area(15.0)
                .status(FarmStatus.REJECTED)
                .build();
        when(farmRepository.findById(103L)).thenReturn(Optional.of(rejectedFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(103L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("PENDING");
        FarmResponse result = farmApprovalService.updateStatus(103L, request, "super@bicap.com");

        assertEquals(FarmStatus.PENDING, result.getStatus());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void updateStatus_sameStatus_shouldBeIdempotent() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(101L)).thenReturn(Optional.of(approvedFarmEntity()));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(101L)).thenReturn(List.of());

        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("APPROVED");
        FarmResponse result = farmApprovalService.updateStatus(101L, request, "super@bicap.com");

        assertEquals(FarmStatus.APPROVED, result.getStatus());
        verify(farmRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void updateNotes_shouldSetAdminNotes() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmNotesUpdateRequest request = new FarmNotesUpdateRequest("  Hồ sơ đầy đủ, chủ trại uy tín.  ");
        FarmResponse result = farmApprovalService.updateNotes(100L, request, "super@bicap.com");

        assertEquals("Hồ sơ đầy đủ, chủ trại uy tín.", result.getAdminNotes());
    }

    @Test
    void updateNotes_blank_shouldClearNotes() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        FarmNotesUpdateRequest request = new FarmNotesUpdateRequest("   ");
        FarmResponse result = farmApprovalService.updateNotes(100L, request, "super@bicap.com");

        assertNull(result.getAdminNotes());
    }

    @Test
    void updateNotes_maxLengthPlusPadding_shouldPassBecauseTrimmedFirst() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        when(certificationRepository.findByFarmId(100L)).thenReturn(List.of());
        when(farmRepository.save(any(Farm.class))).thenAnswer(inv -> inv.getArgument(0));

        // 2000 meaningful chars + 5 trailing spaces: raw length 2005 > 2000, but the
        // stored (trimmed) value fits — the old @Size-on-raw bug would have rejected it.
        String body = "a".repeat(2000) + "     ";
        FarmNotesUpdateRequest request = new FarmNotesUpdateRequest(body);
        FarmResponse result = farmApprovalService.updateNotes(100L, request, "super@bicap.com");

        assertEquals("a".repeat(2000), result.getAdminNotes());
    }

    @Test
    void updateNotes_trimmedLengthOverLimit_shouldThrowBadRequest() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(100L)).thenReturn(Optional.of(pendingFarm));

        FarmNotesUpdateRequest request = new FarmNotesUpdateRequest("b".repeat(2001));
        assertThrows(BadRequestException.class,
                () -> farmApprovalService.updateNotes(100L, request, "super@bicap.com"));
        verify(farmRepository, never()).save(any());
    }

    @Test
    void updateStatus_byNonAdmin_shouldThrowForbidden() {
        when(userRepository.findByEmail("farmer@bicap.com")).thenReturn(Optional.of(farmOwner));

        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("SUSPENDED");
        assertThrows(ForbiddenException.class,
                () -> farmApprovalService.updateStatus(100L, request, "farmer@bicap.com"));
    }

    @Test
    void updateStatus_unknownFarm_shouldThrowNotFound() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(farmRepository.findById(999L)).thenReturn(Optional.empty());

        FarmStatusUpdateRequest request = new FarmStatusUpdateRequest("SUSPENDED");
        assertThrows(ResourceNotFoundException.class,
                () -> farmApprovalService.updateStatus(999L, request, "super@bicap.com"));
    }
}
