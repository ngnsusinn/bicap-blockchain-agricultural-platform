package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmApprovalRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmCertificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.UnauthorizedException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Farm registration approval workflow (BICAP-3 / SRS-ADM-002).
 * - View pending/approved/rejected registrations with their documents
 * - Approve or reject a registration; the farm owner is notified in-app
 */
@Service
@Transactional
@SuppressWarnings("null")
public class FarmApprovalService {

    private final FarmRepository farmRepository;
    private final FarmCertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public FarmApprovalService(FarmRepository farmRepository,
                               FarmCertificationRepository certificationRepository,
                               UserRepository userRepository,
                               NotificationRepository notificationRepository) {
        this.farmRepository = farmRepository;
        this.certificationRepository = certificationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    // ── Actor authorization (mirrors AdminService pattern) ──

    private User requireActor(String actorEmail) {
        if (actorEmail == null || actorEmail.trim().isEmpty()) {
            throw new UnauthorizedException("Actor email header is missing");
        }
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new UnauthorizedException("Actor not found"));
    }

    private boolean isAdminRole(User actor) {
        return actor.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equalsIgnoreCase(role.getName())
                        || "ADMIN".equalsIgnoreCase(role.getName())
                        || "MODERATOR".equalsIgnoreCase(role.getName()));
    }

    private void checkAdminView(String actorEmail) {
        User actor = requireActor(actorEmail);
        if (!isAdminRole(actor)) {
            throw new ForbiddenException("Only admin roles are authorized to view farm registrations");
        }
    }

    private void checkAdminApprove(String actorEmail) {
        User actor = requireActor(actorEmail);
        boolean isApprover = actor.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equalsIgnoreCase(role.getName())
                        || "ADMIN".equalsIgnoreCase(role.getName()));
        if (!isApprover) {
            throw new ForbiddenException("Only SUPER_ADMIN or ADMIN can approve/reject farm registrations");
        }
    }

    // ── Read operations ──

    @Transactional(readOnly = true)
    public Page<FarmResponse> getFarms(FarmStatus status, String search, Pageable pageable, String actorEmail) {
        checkAdminView(actorEmail);
        Page<Farm> farms = farmRepository.findFarmsFiltered(status, search, pageable);
        return farms.map(farm -> toSummary(farm, farm.getUserId()));
    }

    @Transactional(readOnly = true)
    public FarmDetailResponse getFarmDetail(Long id, String actorEmail) {
        checkAdminView(actorEmail);
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm registration not found with id: " + id));
        FarmResponse summary = toSummary(farm, farm.getUserId());
        List<FarmCertificationResponse> certifications = certificationRepository.findByFarmId(farm.getId())
                .stream()
                .map(FarmCertificationResponse::fromEntity)
                .collect(Collectors.toList());
        return new FarmDetailResponse(summary, certifications);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatusCounts(String actorEmail) {
        checkAdminView(actorEmail);
        return Map.of(
                "PENDING", farmRepository.countByStatus(FarmStatus.PENDING),
                "APPROVED", farmRepository.countByStatus(FarmStatus.APPROVED),
                "REJECTED", farmRepository.countByStatus(FarmStatus.REJECTED)
        );
    }

    // ── Approval actions ──

    public FarmResponse approveFarm(Long id, String actorEmail) {
        checkAdminApprove(actorEmail);
        Farm farm = getPendingFarm(id);

        // SRS-ADM-002 E1: warn when no business license/document is attached (surfaced to UI via certificationCount)
        farm.setStatus(FarmStatus.APPROVED);
        Farm saved = farmRepository.save(farm);

        notifyOwner(saved, "SUCCESS",
                "Nông trại của bạn đã được phê duyệt",
                "Chúc mừng! Nông trại \"" + saved.getName() + "\" đã được Admin phê duyệt. "
                        + "Bạn có thể bắt đầu tạo mùa vụ và sử dụng đầy đủ tính năng của hệ thống.");

        return toSummary(saved, saved.getUserId());
    }

    public FarmResponse rejectFarm(Long id, FarmApprovalRequest request, String actorEmail) {
        checkAdminApprove(actorEmail);

        if (request == null || !request.isReject()) {
            throw new BadRequestException("Action must be REJECT for this endpoint");
        }

        // SRS-ADM-002 E2: rejection reason is mandatory
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BadRequestException("Rejection reason is required");
        }

        Farm farm = getPendingFarm(id);
        farm.setStatus(FarmStatus.REJECTED);
        Farm saved = farmRepository.save(farm);

        notifyOwner(saved, "WARNING",
                "Đăng ký nông trại bị từ chối",
                "Nông trại \"" + saved.getName() + "\" đã bị từ chối. Lý do: " + request.getReason().trim()
                        + " — Bạn có thể bổ sung hồ sơ và nộp lại đăng ký.");

        return toSummary(saved, saved.getUserId());
    }

    private Farm getPendingFarm(Long id) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm registration not found with id: " + id));
        if (farm.getStatus() != FarmStatus.PENDING) {
            throw new BadRequestException("Farm registration is not in PENDING status (current: " + farm.getStatus() + ")");
        }
        return farm;
    }

    private void notifyOwner(Farm farm, String type, String title, String content) {
        Notification notification = Notification.builder()
                .userId(farm.getUserId())
                .type(type)
                .title(title)
                .content(content)
                .channel("IN_APP")
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    private FarmResponse toSummary(Farm farm, Long ownerUserId) {
        User owner = userRepository.findById(ownerUserId).orElse(null);
        long certCount = certificationRepository.findByFarmId(farm.getId()).size();
        return FarmResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .address(farm.getAddress())
                .area(farm.getArea())
                .gpsLat(farm.getGpsLat())
                .gpsLng(farm.getGpsLng())
                .status(farm.getStatus())
                .createdAt(farm.getCreatedAt())
                .ownerName(owner != null ? owner.getFullName() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerPhone(owner != null ? owner.getPhone() : null)
                .certificationCount((int) certCount)
                .build();
    }
}
