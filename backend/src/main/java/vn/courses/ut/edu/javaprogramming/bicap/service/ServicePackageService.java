package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ServicePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ServicePackageRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ServicePackageService {

    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "ADMIN");
    /** Đọc danh mục gói: MODERATOR là vai trò chỉ đọc nên cũng được xem. */
    private static final Set<String> ADMIN_VIEW_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "MODERATOR");

    private final ServicePackageRepository servicePackageRepository;
    private final SubscriptionRepository subscriptionRepository;

    public ServicePackageService(ServicePackageRepository servicePackageRepository,
                                 SubscriptionRepository subscriptionRepository) {
        this.servicePackageRepository = servicePackageRepository;
        this.subscriptionRepository   = subscriptionRepository;
    }

    // ── Public (Farm Manager) ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ServicePackageResponse> getAllActivePackages() {
        return servicePackageRepository.findAllByStatus("ACTIVE").stream()
                .map(ServicePackageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServicePackageResponse getPackageById(Long id) {
        // Public detail endpoint: an INACTIVE package that the public list hides must not be
        // retrievable by guessing its id. Admins read every status via /admin/all.
        return servicePackageRepository.findById(id)
                .filter(pkg -> "ACTIVE".equals(pkg.getStatus()))
                .map(ServicePackageResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found: " + id));
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    /** Admin: list ALL packages regardless of status (MODERATOR được phép xem). */
    @Transactional(readOnly = true)
    public List<ServicePackageResponse> getAllPackagesAdmin() {
        requireAdminView();
        return servicePackageRepository.findAll().stream()
                .map(ServicePackageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** Admin: create a new service package. */
    public ServicePackageResponse createPackage(Map<String, Object> body) {
        requireAdmin();
        String name = requireStr(body, "name");
        if (servicePackageRepository.findAll().stream()
                .anyMatch(p -> name.equalsIgnoreCase(p.getName()))) {
            throw new ConflictException("Service package with name '" + name + "' already exists");
        }
        ServicePackage pkg = new ServicePackage();
        pkg.setName(name);
        pkg.setDescription(requireStr(body, "description"));
        pkg.setPrice(new BigDecimal(requireStr(body, "price")));
        pkg.setDurationDays(Integer.parseInt(requireStr(body, "durationDays")));
        pkg.setFeatures(body.getOrDefault("features", "[]").toString());
        pkg.setStatus("ACTIVE");
        return ServicePackageResponse.fromEntity(servicePackageRepository.save(pkg));
    }

    /** Admin: update an existing package. */
    public ServicePackageResponse updatePackage(Long id, Map<String, Object> body) {
        requireAdmin();
        ServicePackage pkg = findPackage(id);
        if (body.containsKey("name"))        pkg.setName(body.get("name").toString());
        if (body.containsKey("description")) pkg.setDescription(body.get("description").toString());
        if (body.containsKey("price"))       pkg.setPrice(new BigDecimal(body.get("price").toString()));
        if (body.containsKey("durationDays"))pkg.setDurationDays(Integer.parseInt(body.get("durationDays").toString()));
        if (body.containsKey("features"))    pkg.setFeatures(body.get("features").toString());
        if (body.containsKey("status")) {
            String newStatus = body.get("status").toString();
            if (!Set.of("ACTIVE", "INACTIVE").contains(newStatus)) {
                throw new BadRequestException("Invalid status: " + newStatus + " (ACTIVE or INACTIVE)");
            }
            pkg.setStatus(newStatus);
        }
        return ServicePackageResponse.fromEntity(servicePackageRepository.save(pkg));
    }

    /**
     * Admin: delete (or deactivate) a package.
     * If the package has active subscriptions, we deactivate instead of hard-delete
     * to preserve referential integrity.
     */
    public void deletePackage(Long id) {
        requireAdmin();
        ServicePackage pkg = findPackage(id);
        long activeCount = subscriptionRepository.findAll().stream()
                .filter(s -> id.equals(s.getPackageId()))
                .filter(s -> s.getStatus() != null &&
                             s.getStatus().name().equals("ACTIVE"))
                .count();
        if (activeCount > 0) {
            // Soft-delete: deactivate so existing subscriptions remain valid
            pkg.setStatus("INACTIVE");
            servicePackageRepository.save(pkg);
        } else {
            servicePackageRepository.delete(pkg);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ServicePackage findPackage(Long id) {
        return servicePackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found: " + id));
    }

    private String requireStr(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null || v.toString().isBlank()) {
            throw new BadRequestException("Field '" + key + "' is required");
        }
        return v.toString().trim();
    }

    private void requireAdmin() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, ADMIN_ROLES);
    }

    private void requireAdminView() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, ADMIN_VIEW_ROLES);
    }
}
