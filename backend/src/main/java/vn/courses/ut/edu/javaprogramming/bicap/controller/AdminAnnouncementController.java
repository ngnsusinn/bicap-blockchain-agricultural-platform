package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AnnouncementRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.NotificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.NotificationService;

/**
 * Admin management of platform-wide announcements (C-3 / BICAP-69).
 *
 * <p>These are the only notifications an unauthenticated guest can read, so this is the
 * write side of the guest "general notifications" requirement (SRS role: Guest).
 */
@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AdminAnnouncementController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> publish(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @Valid @RequestBody AnnouncementRequest request) {
        ActorAuthorizer.requireAdminWrite(userRepository, actorEmail);
        NotificationResponse published = notificationService.publishSystemAnnouncement(
                request.getType(), request.getTitle(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(published);
    }
}
