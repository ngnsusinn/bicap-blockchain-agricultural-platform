package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.util.SearchUtils;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmApprovalRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmCertificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmNotesUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmStatusUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
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

    // ── Actor authorization — delegated to the shared ActorAuthorizer ──

    private void checkAdminView(String actorEmail) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);
    }

    private void checkAdminApprove(String actorEmail) {
        ActorAuthorizer.requireAdminWrite(userRepository, actorEmail);
    }

    // ── Read operations ──

    @Transactional(readOnly = true)
    public Page<FarmResponse> getFarms(FarmStatus status, String search, Pageable pageable, String actorEmail) {
        checkAdminView(actorEmail);
        Page<Farm> farms = farmRepository.findFarmsFiltered(status, SearchUtils.escapeLike(search), pageable);

        // Batch-load owners and certification counts for the whole page (avoids N+1:
        // one owner query + one cert query per farm would otherwise run for each row).
        List<Farm> content = farms.getContent();
        if (content.isEmpty()) {
            return Page.empty(farms.getPageable());
        }
        Map<Long, User> owners = userRepository.findAllById(
                        content.stream().map(Farm::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Long> certCounts = certificationRepository
                .findByFarmIdIn(content.stream().map(Farm::getId).collect(Collectors.toList()))
                .stream().collect(Collectors.groupingBy(FarmCertification::getFarmId, Collectors.counting()));

        return farms.map(farm -> toSummary(farm, owners.get(farm.getUserId()),
                certCounts.getOrDefault(farm.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public FarmDetailResponse getFarmDetail(Long id, String actorEmail) {
        checkAdminView(actorEmail);
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm registration not found with id: " + id));

        // Load certifications ONCE — the same list feeds both the count and the detail
        // (previously the count and the list were two separate queries).
        List<FarmCertification> certs = certificationRepository.findByFarmId(farm.getId());
        User owner = userRepository.findById(farm.getUserId()).orElse(null);
        FarmResponse summary = toSummary(farm, owner, certs.size());
        List<FarmCertificationResponse> certifications = certs.stream()
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

    // ── Farm management (BICAP-4 / SRS-ADM-003) ──

    public FarmResponse updateStatus(Long id, FarmStatusUpdateRequest request, String actorEmail) {
        checkAdminApprove(actorEmail);
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + id));

        // Guard: the management endpoint only applies to farms that already passed the
        // BICAP-3 approval workflow. PENDING/REJECTED farms must be handled through
        // the approve/reject endpoints — flipping them straight to APPROVED here would
        // bypass the approval process, and PENDING → SUSPENDED would strand the farm
        // forever (it could never return to the approval queue).
        if (farm.getStatus() == FarmStatus.PENDING || farm.getStatus() == FarmStatus.REJECTED) {
            throw new BadRequestException("Farm is in " + farm.getStatus()
                    + " status — use the approval endpoints (/approve, /reject) instead of status management");
        }

        FarmStatus newStatus = FarmStatus.valueOf(request.getStatus());
        if (farm.getStatus() == newStatus) {
            return toSummary(farm, farm.getUserId());
        }
        farm.setStatus(newStatus);
        Farm saved = farmRepository.save(farm);

        if (newStatus == FarmStatus.SUSPENDED) {
            notifyOwner(saved, "WARNING", "Nông trại bị tạm ngưng hoạt động",
                    "Nông trại \"" + saved.getName() + "\" đã bị tạm ngưng hoạt động bởi Admin.");
        } else if (newStatus == FarmStatus.INACTIVE) {
            notifyOwner(saved, "WARNING", "Nông trại ngừng hoạt động",
                    "Nông trại \"" + saved.getName() + "\" đã bị Admin đặt ở trạng thái ngừng hoạt động.");
        } else if (newStatus == FarmStatus.APPROVED) {
            notifyOwner(saved, "SUCCESS", "Nông trại đã được kích hoạt lại",
                    "Nông trại \"" + saved.getName() + "\" đã được Admin kích hoạt hoạt động trở lại.");
        }

        return toSummary(saved, saved.getUserId());
    }

    private static final int NOTES_MAX_LENGTH = 2000;

    public FarmResponse updateNotes(Long id, FarmNotesUpdateRequest request, String actorEmail) {
        checkAdminApprove(actorEmail);
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + id));

        if (request == null || request.getNotes() == null) {
            return toSummary(farm, farm.getUserId());
        }

        // Trim FIRST, then validate the stored value — trailing/leading whitespace must
        // not make a fitting note fail the length check (SRS-ADM-003: max 2000 chars).
        String notes = request.getNotes().trim();
        if (notes.length() > NOTES_MAX_LENGTH) {
            throw new BadRequestException("Notes must not exceed " + NOTES_MAX_LENGTH + " characters");
        }

        farm.setAdminNotes(notes.isEmpty() ? null : notes);
        Farm saved = farmRepository.save(farm);
        return toSummary(saved, saved.getUserId());
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

    /** Convenience for single-farm flows (approve/reject/status/notes) — loads owner + count eagerly. */
    private FarmResponse toSummary(Farm farm, Long ownerUserId) {
        User owner = userRepository.findById(ownerUserId).orElse(null);
        long certCount = certificationRepository.findByFarmId(farm.getId()).size();
        return toSummary(farm, owner, certCount);
    }

    /** Core mapper — receives pre-loaded owner + certification count (used by batched list endpoints). */
    private FarmResponse toSummary(Farm farm, User owner, long certCount) {
        return FarmResponse.builder()
                .id(farm.getId())
                .name(farm.getName())
                .address(farm.getAddress())
                .area(farm.getArea())
                .gpsLat(farm.getGpsLat())
                .gpsLng(farm.getGpsLng())
                .description(farm.getDescription())
                .productTypes(farm.getProductTypes())
                .adminNotes(farm.getAdminNotes())
                .status(farm.getStatus())
                .createdAt(farm.getCreatedAt())
                .updatedAt(farm.getUpdatedAt())
                .ownerName(owner != null ? owner.getFullName() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerPhone(owner != null ? owner.getPhone() : null)
                .certificationCount((int) certCount)
                .build();
    }
}
