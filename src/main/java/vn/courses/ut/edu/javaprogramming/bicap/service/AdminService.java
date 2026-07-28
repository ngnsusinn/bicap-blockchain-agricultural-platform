package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.UnauthorizedException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PermissionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public AdminService(UserRepository userRepository, RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^()+=.-])[A-Za-z\\d@$!%*?&_#^()+=.-]{8,}$";

    private void checkSuperAdmin(String actorEmail) {
        if (actorEmail == null || actorEmail.trim().isEmpty()) {
            throw new UnauthorizedException("Actor email header is missing");
        }
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new UnauthorizedException("Actor not found"));
        boolean isSuperAdmin = actor.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equalsIgnoreCase(role.getName()));
        if (!isSuperAdmin) {
            throw new ForbiddenException("Only SUPER_ADMIN is authorized for this operation");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || !password.matches(PASSWORD_PATTERN)) {
            throw new BadRequestException("Password does not meet complexity requirements. It must contain at least 8 characters, an uppercase letter, a lowercase letter, a digit, and a special character.");
        }
    }

    @Transactional(readOnly = true)
    public Page<AdminResponse> getAdmins(UserStatus status, String role, String search, Pageable pageable) {
        return userRepository.findAdminsFiltered(status, role, search, pageable)
                .map(AdminResponse::fromUser);
    }

    @Transactional(readOnly = true)
    public AdminResponse getAdminById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return AdminResponse.fromUser(user);
    }

    public AdminResponse createAdmin(AdminCreateRequest request, String actorEmail) {
        // Rule BR1: Only SUPER_ADMIN can create other admin accounts
        checkSuperAdmin(actorEmail);

        // Error on: Email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use: " + request.getEmail());
        }

        // Error on: Weak password
        validatePasswordStrength(request.getPassword());

        // Rule BR3: Newly created accounts default to ACTIVE status if not specified
        UserStatus statusVal = UserStatus.ACTIVE;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                statusVal = UserStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
        }

        // Resolve roles
        Set<Role> roles = new HashSet<>();
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            Role role = roleRepository.findByName(request.getRole().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));
            roles.add(role);
        } else {
            Role defaultRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new ResourceNotFoundException("Default ADMIN role not found"));
            roles.add(defaultRole);
        }

        // Validate permissions if present (must exist in system)
        if (request.getPermissions() != null) {
            for (String permCode : request.getPermissions()) {
                permissionRepository.findByCode(permCode.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permCode));
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword()) // Storing plaintext as mock security for now (no full JWT / password encoder requested)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(statusVal)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return AdminResponse.fromUser(savedUser);
    }

    public AdminResponse updateAdmin(Long id, AdminUpdateRequest request, String actorEmail) {
        checkSuperAdmin(actorEmail);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                user.setStatus(UserStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
        }

        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            Role role = roleRepository.findByName(request.getRole().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRole()));
            user.getRoles().clear();
            user.getRoles().add(role);
        }

        if (request.getPermissions() != null) {
            for (String permCode : request.getPermissions()) {
                permissionRepository.findByCode(permCode.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permCode));
            }
        }

        User updatedUser = userRepository.save(user);
        return AdminResponse.fromUser(updatedUser);
    }

    public void deleteAdmin(Long id, String actorEmail) {
        // Rule BR1: Only SUPER_ADMIN can delete other admin accounts
        checkSuperAdmin(actorEmail);

        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Admin cannot delete themselves - BR E3
        if (target.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new BadRequestException("Admin cannot delete themselves");
        }

        // Rule BR2: Soft-delete is marked by setting status to INACTIVE
        target.setStatus(UserStatus.INACTIVE);
        userRepository.save(target);
    }
}
