package vn.courses.ut.edu.javaprogramming.bicap.config;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Permission;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PermissionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@SuppressWarnings("null")
public class DatabaseSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public DatabaseSeeder(PermissionRepository permissionRepository, RoleRepository roleRepository, UserRepository userRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Permissions
        Permission adminCreate = seedPermission("ADMIN_CREATE", "Permission to create admin accounts");
        Permission adminRead = seedPermission("ADMIN_READ", "Permission to view admin accounts");
        Permission adminUpdate = seedPermission("ADMIN_UPDATE", "Permission to update admin accounts");
        Permission adminDelete = seedPermission("ADMIN_DELETE", "Permission to delete admin accounts");

        // 2. Seed Roles
        Role superAdminRole = seedRole("SUPER_ADMIN", "Super Administrator with full access",
                Set.of(adminCreate, adminRead, adminUpdate, adminDelete));
        Role adminRole = seedRole("ADMIN", "Administrator with read/write access",
                Set.of(adminCreate, adminRead, adminUpdate));
        Role moderatorRole = seedRole("MODERATOR", "Moderator with read-only access",
                Set.of(adminRead));

        // 3. Seed Users
        seedUser("superadmin@bicap.com", "Superadmin@2026", "Super Admin", "0987654321", superAdminRole);
        seedUser("admin@bicap.com", "Adminpassword@2026", "Admin User", "0912345678", adminRole);
        seedUser("moderator@bicap.com", "Moderator@2026", "Moderator User", "0901234567", moderatorRole);
    }

    private Permission seedPermission(String code, String description) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> permissionRepository.save(
                        Permission.builder()
                                .code(code)
                                .description(description)
                                .build()
                ));
    }

    private Role seedRole(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .map(existingRole -> {
                    // Update permissions if needed
                    existingRole.setPermissions(permissions);
                    return roleRepository.save(existingRole);
                })
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(name)
                                .description(description)
                                .permissions(permissions)
                                .build()
                ));
    }

    private void seedUser(String email, String password, String fullName, String phone, Role role) {
        if (!userRepository.existsByEmail(email)) {
            Set<Role> roles = new HashSet<>();
            roles.add(role);

            userRepository.save(User.builder()
                    .email(email)
                    .password(password) // Storing plain text password for simplified setup/testing as per requirements
                    .fullName(fullName)
                    .phone(phone)
                    .status(UserStatus.ACTIVE)
                    .roles(roles)
                    .build());
        }
    }
}
