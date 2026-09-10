package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ReportCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ReportResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ReportRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import vn.courses.ut.edu.javaprogramming.bicap.service.ReportService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BICAP-27 / SRS-FM-021 — unit tests cho ReportService: mọi vai trò gửi được báo cáo,
 * chỉ Admin xem/xử lý, phản hồi gửi thông báo tới người báo cáo.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ReportServiceTest {

    @Mock ReportRepository reports;
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(reports, users, notifications);
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void login(String role, Long id, String name) {
        Role r = Role.builder().name(role).permissions(Set.of()).build();
        User u = User.builder().id(id).email(name + "@bicap.vn").fullName(name).status(UserStatus.ACTIVE).roles(Set.of(r)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
    }

    private Report openReport(Long id, Long reporterId) {
        Report r = new Report();
        r.setId(id);
        r.setReporterId(reporterId);
        r.setReporterRole("FARM_MANAGER");
        r.setType(Report.TYPE_COMPLAINT);
        r.setSubject("Đơn sai số lượng");
        r.setContent("Nội dung báo cáo chi tiết về đơn hàng.");
        r.setStatus(Report.STATUS_OPEN);
        return r;
    }

    @Test
    void createReport_persistsOpenReportAndNotifiesAdmins() {
        login("FARM_MANAGER", 7L, "Khuong");
        when(reports.save(any())).thenAnswer(inv -> { Report r = inv.getArgument(0); r.setId(100L); return r; });
        User admin = User.builder().id(1L).email("admin@bicap.vn").fullName("Admin").status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().name("ADMIN").permissions(Set.of()).build())).build();
        when(users.findDistinctByRoles_NameIn(anyCollection())).thenReturn(List.of(admin));

        ReportResponse result = service.createReport(
                new ReportCreateRequest("COMPLAINT", "Đơn sai số lượng", "Nội dung báo cáo chi tiết về đơn hàng.", 42L));

        assertEquals(100L, result.getId());
        assertEquals("OPEN", result.getStatus());
        assertEquals("FARM_MANAGER", result.getReporterRole());
        verify(reports).save(argThat(r -> r.getStatus().equals(Report.STATUS_OPEN)
                && r.getType().equals(Report.TYPE_COMPLAINT)
                && r.getReporterId().equals(7L)
                && r.getRelatedOrderId().equals(42L)));
        verify(notifications).sendNotification(eq(1L), eq("WARNING"), anyString(), anyString(), eq(false));
    }

    @Test
    void createReport_rejectsInvalidType() {
        login("FARM_MANAGER", 7L, "Khuong");
        assertThrows(BadRequestException.class, () -> service.createReport(
                new ReportCreateRequest("SPAM", "Tiêu đề", "Nội dung đủ dài ở đây nhé.", null)));
        verifyNoInteractions(reports);
    }

    @Test
    void getMyReports_returnsOnlyOwnReports() {
        login("FARM_MANAGER", 7L, "Khuong");
        when(reports.findByReporterIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(openReport(1L, 7L)));

        List<ReportResponse> result = service.getMyReports();

        assertEquals(1, result.size());
        verify(reports).findByReporterIdOrderByCreatedAtDesc(7L);
    }

    @Test
    void getReports_forbidsNonAdmin() {
        login("FARM_MANAGER", 7L, "Khuong");
        assertThrows(ForbiddenException.class, () -> service.getReports(null, null, null));
        verifyNoInteractions(reports);
    }

    @Test
    void getReports_adminSeesFilteredList() {
        login("ADMIN", 1L, "Admin");
        when(reports.findFiltered(eq("OPEN"), isNull(), isNull())).thenReturn(List.of(openReport(1L, 7L)));
        when(users.findById(7L)).thenReturn(Optional.of(
                User.builder().id(7L).fullName("Khuong").status(UserStatus.ACTIVE).roles(Set.of()).build()));

        List<ReportResponse> result = service.getReports("open", null, null);

        assertEquals(1, result.size());
        assertEquals("Khuong", result.get(0).getReporterName());
    }

    @Test
    void handleReport_updatesStatusAndNotifiesReporter() {
        login("ADMIN", 1L, "Admin");
        when(reports.findById(5L)).thenReturn(Optional.of(openReport(5L, 7L)));
        when(reports.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReportResponse result = service.handleReport(5L, "RESOLVED", "Đã liên hệ xử lý.");

        assertEquals("RESOLVED", result.getStatus());
        assertEquals("Đã liên hệ xử lý.", result.getAdminResponse());
        verify(notifications).sendNotification(eq(7L), eq("INFO"), anyString(), anyString(), eq(false));
    }

    @Test
    void handleReport_requiresAdminResponse() {
        login("ADMIN", 1L, "Admin");
        when(reports.findById(5L)).thenReturn(Optional.of(openReport(5L, 7L)));
        assertThrows(BadRequestException.class, () -> service.handleReport(5L, "RESOLVED", "  "));
        verify(reports, never()).save(any());
    }

    @Test
    void handleReport_rejectsUnknownStatus() {
        login("ADMIN", 1L, "Admin");
        when(reports.findById(5L)).thenReturn(Optional.of(openReport(5L, 7L)));
        assertThrows(BadRequestException.class, () -> service.handleReport(5L, "WONTFIX", "ok"));
    }

    @Test
    void handleReport_notFound() {
        login("ADMIN", 1L, "Admin");
        when(reports.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.handleReport(99L, "RESOLVED", "ok"));
    }
}
