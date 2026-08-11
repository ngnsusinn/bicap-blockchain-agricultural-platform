package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationListResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;
import vn.courses.ut.edu.javaprogramming.bicap.service.VerificationEmailService;
import vn.courses.ut.edu.javaprogramming.bicap.service.impl.NotificationServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BICAP-77 / SRS-API-006: read/mark-as-read ownership checks, mark-all, and
 * persist + SSE/email fan-out for sendNotification.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VerificationEmailService emailService;

    private NotificationService notificationService;

    private User farmOwner;
    private Notification notification;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository, emailService);

        farmOwner = User.builder()
                .id(10L).email("farmer@bicap.com").password("x")
                .fullName("Chủ Trang Trại").status(UserStatus.ACTIVE).roles(Set.of())
                .build();

        notification = Notification.builder()
                .id(1L).userId(10L).type("IOT_ALERT").title("Cảnh báo IoT")
                .content("Nhiệt độ bất thường").channel("IN_APP").isRead(false)
                .createdAt(LocalDateTime.of(2026, 8, 10, 9, 0))
                .build();
    }

    @Test
    void getUserNotifications_returnsUnreadCountAndMappedDtos() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(notification));
        when(notificationRepository.countByUserIdAndIsReadFalse(10L)).thenReturn(3L);

        NotificationListResponse response = notificationService.getUserNotifications(10L);

        assertEquals(3L, response.getUnreadCount());
        assertEquals(1, response.getNotifications().size());
        NotificationResponse dto = response.getNotifications().get(0);
        assertEquals(1L, dto.getId());
        assertEquals("Cảnh báo IoT", dto.getTitle());
        assertFalse(dto.getIsRead());
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndIsReadFalse(10L)).thenReturn(5L);

        assertEquals(5L, notificationService.getUnreadCount(10L));
    }

    @Test
    void markAsRead_ownerCanRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        NotificationResponse response = notificationService.markAsRead(1L, 10L);

        assertTrue(response.getIsRead());
        assertTrue(notification.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_anotherUser_throwsForbidden() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(ForbiddenException.class, () -> notificationService.markAsRead(1L, 99L));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_missingNotification_throwsNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(999L, 10L));
    }

    @Test
    void markAllAsRead_delegatesToRepository() {
        notificationService.markAllAsRead(10L);

        verify(notificationRepository).markAllAsReadByUserId(10L);
    }

    @Test
    void sendNotification_persistsWithInAppChannel() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(7L);
            return n;
        });

        notificationService.sendNotification(10L, "ORDER_CREATED", "Đơn hàng mới", "Có đơn mới", false);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(10L, saved.getUserId());
        assertEquals("ORDER_CREATED", saved.getType());
        assertEquals("IN_APP", saved.getChannel());
        assertFalse(saved.getIsRead());

        // No email requested → no email send, no user lookup.
        verify(emailService, never()).sendNotificationEmail(any(), any(), any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void sendNotification_withEmail_sendsToUser() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));

        notificationService.sendNotification(10L, "FARM_APPROVED", "Duyệt trang trại", "Trang trại đã được duyệt", true);

        verify(emailService).sendNotificationEmail(eq("farmer@bicap.com"), eq("Duyệt trang trại"), eq("Trang trại đã được duyệt"));
    }

    @Test
    void sendNotification_emailForMissingUser_isSkipped() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        notificationService.sendNotification(10L, "FARM_APPROVED", "Duyệt trang trại", "nội dung", true);

        verify(emailService, never()).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void sendNotification_emailFailure_doesNotBreak() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(farmOwner));
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .sendNotificationEmail(any(), any(), any());

        // Must not propagate — the persisted notification still succeeds.
        assertDoesNotThrow(() ->
                notificationService.sendNotification(10L, "ORDER_ACCEPTED", "Đơn được duyệt", "nội dung", true));
        verify(emailService).sendNotificationEmail(any(), any(), any());
    }
}
