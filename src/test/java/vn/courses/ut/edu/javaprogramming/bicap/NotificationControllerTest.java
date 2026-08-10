package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.controller.NotificationController;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.GlobalExceptionHandler;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BICAP-77 / SRS-API-006: the notification endpoints are resolved against the
 * authenticated principal via {@link CurrentUser} — no client-supplied userId.
 */
class NotificationControllerTest {

    private NotificationService notificationService;
    private MockMvc mockMvc;

    private User farmOwner;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Role farmRole = Role.builder().id(4L).name("FARM_MANAGER").permissions(Set.of()).build();
        farmOwner = User.builder()
                .id(10L).email("farmer@bicap.com").password("x")
                .fullName("Chủ Trang Trại").status(UserStatus.ACTIVE).roles(Set.of(farmRole))
                .build();
        authenticateAs(farmOwner);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private NotificationResponse notificationResponse() {
        return new NotificationResponse(1L, "IOT_ALERT", "Cảnh báo IoT",
                "Nhiệt độ bất thường", false, LocalDateTime.of(2026, 8, 10, 9, 0));
    }

    @Test
    void getMyNotifications_returnsListForAuthenticatedUser() throws Exception {
        when(notificationService.getUserNotifications(10L))
                .thenReturn(new NotificationListResponse(1L, List.of(notificationResponse())));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(1))
                .andExpect(jsonPath("$.notifications[0].title").value("Cảnh báo IoT"));

        verify(notificationService).getUserNotifications(10L);
    }

    @Test
    void getMyNotifications_withoutAuthentication_throwsUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUnreadCount_returnsBadgeCount() throws Exception {
        when(notificationService.getUnreadCount(10L)).thenReturn(4L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4));

        verify(notificationService).getUnreadCount(10L);
    }

    @Test
    void markAsRead_returnsUpdatedNotification() throws Exception {
        when(notificationService.markAsRead(1L, 10L)).thenReturn(notificationResponse());

        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    void markAsRead_anotherUsersNotification_returnsForbidden() throws Exception {
        when(notificationService.markAsRead(1L, 10L)).thenThrow(new ForbiddenException("Cannot modify another user's notification"));

        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAllAsRead_returnsOk() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all"))
                .andExpect(status().isOk());

        verify(notificationService).markAllAsRead(10L);
    }

    @Test
    void stream_returnsSseEmitter() throws Exception {
        SseEmitter emitter = new SseEmitter(0L);
        when(notificationService.subscribe(10L)).thenReturn(emitter);

        mockMvc.perform(get("/api/notifications/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());

        verify(notificationService).subscribe(10L);
    }
}
