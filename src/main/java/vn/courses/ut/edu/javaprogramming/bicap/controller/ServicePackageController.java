package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ServicePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.ServicePackageService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-packages")
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    public ServicePackageController(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;
    }

    /** Public — Farm Manager & Guest: list ACTIVE packages only. */
    @GetMapping
    public ResponseEntity<List<ServicePackageResponse>> getAllActivePackages() {
        return ResponseEntity.ok(servicePackageService.getAllActivePackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(servicePackageService.getPackageById(id));
    }

    // ── Admin CRUD (SUPER_ADMIN / ADMIN) ──────────────────────────────────────

    /** Admin: list ALL packages (ACTIVE + INACTIVE). */
    @GetMapping("/admin/all")
    public ResponseEntity<List<ServicePackageResponse>> getAllPackagesAdmin() {
        return ResponseEntity.ok(servicePackageService.getAllPackagesAdmin());
    }

    /** Admin: create a new service package. */
    @PostMapping("/admin")
    public ResponseEntity<ServicePackageResponse> createPackage(
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicePackageService.createPackage(body));
    }

    /** Admin: update an existing package (partial update). */
    @PutMapping("/admin/{id}")
    public ResponseEntity<ServicePackageResponse> updatePackage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(servicePackageService.updatePackage(id, body));
    }

    /**
     * Admin: delete a package. If active subscriptions exist, the package is
     * soft-deleted (status → INACTIVE) to preserve referential integrity.
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        servicePackageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}
