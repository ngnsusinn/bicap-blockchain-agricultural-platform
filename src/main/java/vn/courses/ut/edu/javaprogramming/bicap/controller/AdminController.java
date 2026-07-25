package vn.courses.ut.edu.javaprogramming.bicap.controller;

import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<Page<AdminResponse>> getAdmins(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<AdminResponse> admins = adminService.getAdmins(status, role, search, pageable);
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminResponse> getAdminById(@PathVariable Long id) {
        AdminResponse admin = adminService.getAdminById(id);
        return ResponseEntity.ok(admin);
    }

    @PostMapping
    public ResponseEntity<AdminResponse> createAdmin(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @Valid @RequestBody AdminCreateRequest request) {
        AdminResponse created = adminService.createAdmin(request, actorEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminResponse> updateAdmin(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequest request) {
        AdminResponse updated = adminService.updateAdmin(id, request, actorEmail);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(
            @RequestHeader("X-Actor-Email") String actorEmail,
            @PathVariable Long id) {
        adminService.deleteAdmin(id, actorEmail);
        return ResponseEntity.noContent().build();
    }
}
