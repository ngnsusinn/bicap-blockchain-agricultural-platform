package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AddCertificationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmCertificationResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmRegistrationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lets a Farm Manager register a new farm from the farm portal (H-7 / BICAP-7).
 * The farm is created in PENDING status and flows through the admin approval workflow (BICAP-3).
 *
 * Extended in BICAP-9 / SRS-FM-003:
 * - createOrUpdateFarm  — tạo mới nếu chưa có farm, cập nhật nếu đã có
 * - updateFarm          — cập nhật thông tin, chuyển status → PENDING, thông báo Admin
 * - getCertifications   — xem danh sách chứng nhận/giấy phép
 * - addCertification    — thêm chứng nhận mới, chuyển status → PENDING, thông báo Admin
 * - deleteCertification — xóa chứng nhận khỏi nông trại
 */
@Service
@Transactional
public class FarmRegistrationService {

    private final FarmRepository farmRepository;
    private final FarmCertificationRepository farmCertificationRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public FarmRegistrationService(FarmRepository farmRepository,
                                   FarmCertificationRepository farmCertificationRepository,
                                   NotificationRepository notificationRepository,
                                   UserRepository userRepository) {
        this.farmRepository = farmRepository;
        this.farmCertificationRepository = farmCertificationRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ── BICAP-7 (existing) ──────────────────────────────────────────────────────

    /**
     * Đăng ký nông trại mới (BICAP-7). Farm bắt đầu với status PENDING.
     */
    public Farm registerFarm(FarmRegistrationRequest request) {
        User actor = CurrentUser.get();

        if (farmRepository.findByName(request.getName().trim()).isPresent()) {
            throw new ConflictException("A farm with this name is already registered");
        }

        Farm farm = Farm.builder()
                .userId(actor.getId())
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .area(request.getArea())
                .gpsLat(request.getGpsLat())
                .gpsLng(request.getGpsLng())
                .description(request.getDescription())
                .productTypes(request.getProductTypes())
                .status(FarmStatus.PENDING)
                .build();
        return farmRepository.save(farm);
    }

    /**
     * Trả về tất cả farm của user đang đăng nhập (M-2).
     */
    @Transactional(readOnly = true)
    public List<Farm> getMyFarms() {
        User actor = CurrentUser.get();
        return farmRepository.findByUserId(actor.getId());
    }

    // ── BICAP-9 (new) ───────────────────────────────────────────────────────────

    /**
     * Tạo hoặc cập nhật thông tin nông trại (BICAP-9 / SRS-FM-003).
     * - Nếu user chưa có farm → tạo mới, status = PENDING.
     * - Nếu đã có farm → cập nhật farm đầu tiên, status → PENDING (BR1).
     * Admin luôn nhận thông báo sau khi tạo/cập nhật (BR2).
     */
    public Farm createOrUpdateFarm(FarmUpdateRequest request) {
        User actor = CurrentUser.get();
        List<Farm> existingFarms = farmRepository.findByUserId(actor.getId());

        if (existingFarms.isEmpty()) {
            // Chưa có farm → tạo mới
            if (farmRepository.findByName(request.getName().trim()).isPresent()) {
                throw new ConflictException("A farm with this name is already registered");
            }

            Farm newFarm = Farm.builder()
                    .userId(actor.getId())
                    .name(request.getName().trim())
                    .address(request.getAddress().trim())
                    .area(request.getArea())
                    .gpsLat(request.getGpsLat())
                    .gpsLng(request.getGpsLng())
                    .description(request.getDescription())
                    .productTypes(request.getProductTypes())
                    .status(FarmStatus.PENDING)
                    .build();

            Farm saved = farmRepository.save(newFarm);

            notifyAdmins(saved, actor.getFullName(),
                    "Nông trại mới cần xét duyệt",
                    "Chủ trang trại \"" + actor.getFullName() + "\" đã đăng ký nông trại mới \""
                            + saved.getName() + "\". Vui lòng xem xét và phê duyệt hồ sơ.");
            return saved;
        } else {
            // Đã có farm → cập nhật farm đầu tiên
            return updateFarm(existingFarms.get(0).getId(), request);
        }
    }

    /**
     * Cập nhật thông tin nông trại theo farmId (BICAP-9 / SRS-FM-003).
     * Chỉ chủ sở hữu mới được cập nhật. Status → PENDING (BR1). Admin được thông báo (BR2).
     */
    public Farm updateFarm(Long farmId, FarmUpdateRequest request) {
        User actor = CurrentUser.get();
        Farm farm = getFarmAndVerifyOwnership(farmId, actor);

        // Kiểm tra tên trùng với nông trại khác
        farmRepository.findByName(request.getName().trim())
                .filter(existing -> !existing.getId().equals(farmId))
                .ifPresent(existing -> {
                    throw new ConflictException("A farm with this name is already registered");
                });

        farm.setName(request.getName().trim());
        farm.setAddress(request.getAddress().trim());
        farm.setArea(request.getArea());
        farm.setGpsLat(request.getGpsLat());
        farm.setGpsLng(request.getGpsLng());
        farm.setDescription(request.getDescription());
        farm.setProductTypes(request.getProductTypes());

        // BR1: status → PENDING sau khi cập nhật
        farm.setStatus(FarmStatus.PENDING);

        Farm saved = farmRepository.save(farm);

        // BR2: thông báo Admin
        notifyAdmins(saved, actor.getFullName(),
                "Nông trại cần xét duyệt lại",
                "Chủ trang trại \"" + actor.getFullName() + "\" đã cập nhật thông tin nông trại \""
                        + saved.getName() + "\". Vui lòng xem xét và phê duyệt lại hồ sơ.");
        return saved;
    }

    /**
     * Lấy danh sách chứng nhận/giấy phép của nông trại (BICAP-9).
     * Chỉ chủ sở hữu mới xem được.
     */
    @Transactional(readOnly = true)
    public List<FarmCertificationResponse> getCertifications(Long farmId) {
        User actor = CurrentUser.get();
        getFarmAndVerifyOwnership(farmId, actor);
        return farmCertificationRepository.findByFarmId(farmId).stream()
                .map(FarmCertificationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Thêm chứng nhận/giấy phép kinh doanh vào nông trại (BICAP-9 / SRS-FM-003).
     * Status farm → PENDING (BR1). Admin được thông báo (BR2).
     */
    public FarmCertificationResponse addCertification(Long farmId, AddCertificationRequest request) {
        User actor = CurrentUser.get();
        Farm farm = getFarmAndVerifyOwnership(farmId, actor);

        FarmCertification cert = FarmCertification.builder()
                .farmId(farmId)
                .type(request.getType().trim())
                .fileUrl(request.getFileUrl().trim())
                .expiryDate(request.getExpiryDate())
                .build();

        FarmCertification saved = farmCertificationRepository.save(cert);

        // BR1: status farm → PENDING
        farm.setStatus(FarmStatus.PENDING);
        farmRepository.save(farm);

        // BR2: thông báo Admin
        notifyAdmins(farm, actor.getFullName(),
                "Nông trại cập nhật giấy phép/chứng nhận",
                "Chủ trang trại \"" + actor.getFullName() + "\" đã tải lên chứng nhận \""
                        + request.getType().trim() + "\" cho nông trại \""
                        + farm.getName() + "\". Vui lòng xem xét và phê duyệt lại hồ sơ.");

        return FarmCertificationResponse.fromEntity(saved);
    }

    /**
     * Xóa chứng nhận khỏi nông trại (BICAP-9).
     * Chỉ chủ sở hữu mới được xóa.
     */
    public void deleteCertification(Long farmId, Long certId) {
        User actor = CurrentUser.get();
        getFarmAndVerifyOwnership(farmId, actor);

        FarmCertification cert = farmCertificationRepository.findById(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found with id: " + certId));

        if (!cert.getFarmId().equals(farmId)) {
            throw new BadRequestException("Certification does not belong to this farm");
        }

        farmCertificationRepository.delete(cert);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    /**
     * Tìm farm và xác minh chủ sở hữu.
     * Ném ResourceNotFoundException nếu không tìm thấy.
     * Ném ForbiddenException nếu không phải chủ sở hữu.
     */
    private Farm getFarmAndVerifyOwnership(Long farmId, User actor) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));

        if (!farm.getUserId().equals(actor.getId())) {
            throw new ForbiddenException("You do not have permission to modify this farm");
        }

        return farm;
    }

    /**
     * Gửi thông báo đến toàn bộ Admin/Super-Admin (BR2).
     */
    private void notifyAdmins(Farm farm, String actorName, String title, String content) {
        List<User> admins = userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName())
                                || "SUPER_ADMIN".equalsIgnoreCase(r.getName())))
                .collect(Collectors.toList());

        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .userId(admin.getId())
                    .type("INFO")
                    .title(title)
                    .content(content)
                    .channel("IN_APP")
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        }
    }
}
