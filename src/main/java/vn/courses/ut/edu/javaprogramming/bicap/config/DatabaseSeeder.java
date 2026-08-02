package vn.courses.ut.edu.javaprogramming.bicap.config;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Permission;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PermissionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@SuppressWarnings("null")
public class DatabaseSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final FarmCertificationRepository farmCertificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(PermissionRepository permissionRepository, RoleRepository roleRepository, UserRepository userRepository,
                          FarmRepository farmRepository, FarmCertificationRepository farmCertificationRepository,
                          PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.farmCertificationRepository = farmCertificationRepository;
        this.passwordEncoder = passwordEncoder;
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
        
        // Seed Functional Roles
        Role farmManagerRole = seedRole("FARM_MANAGER", "Farm Manager for managing farms, seasons, and exports", Set.of());
        Role retailerRole = seedRole("RETAILER", "Retailer for purchasing products and tracking orders", Set.of());
        seedRole("SHIPPING_MGR", "Shipping Manager for coordinating deliveries", Set.of());
        seedRole("SHIP_DRIVER", "Shipping Driver for executing shipments", Set.of());
        seedRole("GUEST", "Guest user for browsing products and educational content", Set.of());

        // 3. Seed Users
        seedUser("superadmin@bicap.com", "Superadmin@2026", "Super Admin", "0987654321", superAdminRole);
        seedUser("admin@bicap.com", "Adminpassword@2026", "Admin User", "0912345678", adminRole);
        seedUser("moderator@bicap.com", "Moderator@2026", "Moderator User", "0901234567", moderatorRole);
        User farmOwner1 = seedUser("farm@bicap.com", "Farmpassword@2026", "Chủ Trang Trại BICAP", "0922334455", farmManagerRole);
        User farmOwner2 = seedUser("farm@bicap.vn", "Farmpassword@2026", "Chủ Trang Trại BICAP VN", "0922334456", farmManagerRole);
        seedUser("retailer@bicap.com", "Retailpassword@2026", "Nhà Bán Lẻ BICAP", "0933445566", retailerRole);
        seedUser("retail@bicap.com", "Retailpassword@2026", "Nhà Bán Lẻ BICAP Short", "0933445567", retailerRole);

        // 4. Seed Sample Farm Registrations (BICAP-3 — admin approval queue)
        seedFarm(farmOwner1.getId(), "Trang Trại Xanh Đồng Nai", "Xã Long An, Huyện Long Thành, Đồng Nai",
                12.5, 10.824610, 107.058112, FarmStatus.PENDING,
                "Giấy phép kinh doanh số 0312345678", "https://bicap.vn/docs/dongnai-license.pdf", LocalDate.now().plusYears(5));
        seedFarm(farmOwner2.getId(), "HTX Nông Sản Sạch Lâm Đồng", "Xã Đạ Đờn, Huyện Lâm Hà, Lâm Đồng",
                25.0, 11.811100, 108.366700, FarmStatus.PENDING,
                "Giấy chứng nhận VietGAP", "https://bicap.vn/docs/lamdong-vietgap.pdf", LocalDate.now().plusYears(2));
        seedFarm(farmOwner1.getId(), "Trang Trại Hữu Cơ Sông Hồng", "Xã Đan Phượng, Hà Nội",
                8.0, 21.122300, 105.681300, FarmStatus.APPROVED,
                "Giấy chứng nhận Organic", "https://bicap.vn/docs/hanoi-organic.pdf", LocalDate.now().plusYears(3));
        seedFarm(farmOwner2.getId(), "Vườn Sạch Tiền Giang", "Xã Tân Lập, Huyện Tân Phước, Tiền Giang",
                15.75, 10.467500, 106.209900, FarmStatus.REJECTED,
                "Giấy phép kinh doanh số 0123456789", "https://bicap.vn/docs/tiengiang-license.pdf", LocalDate.now().plusYears(1));
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

    private User seedUser(String email, String password, String fullName, String phone, Role role) {
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        return userRepository.findByEmail(email).map(
                existingUser -> {
                    existingUser.setPassword(passwordEncoder.encode(password));
                    existingUser.setFullName(fullName);
                    existingUser.setPhone(phone);
                    existingUser.setStatus(UserStatus.ACTIVE);
                    existingUser.setRoles(roles);
                    return userRepository.save(existingUser);
                }).orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .fullName(fullName)
                        .phone(phone)
                        .status(UserStatus.ACTIVE)
                        .roles(roles)
                        .build())
        );
    }

    private void seedFarm(Long ownerUserId, String name, String address, double area,
                          double gpsLat, double gpsLng, FarmStatus status,
                          String certType, String certFileUrl, LocalDate certExpiry) {
        Farm farm = farmRepository.findByUserId(ownerUserId).orElseGet(() -> farmRepository.save(
                Farm.builder()
                        .userId(ownerUserId)
                        .name(name)
                        .address(address)
                        .area(area)
                        .gpsLat(gpsLat)
                        .gpsLng(gpsLng)
                        .status(status)
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        boolean hasCertification = !farmCertificationRepository.findByFarmId(farm.getId()).isEmpty();
        if (!hasCertification && certType != null && !certType.isEmpty()) {
            farmCertificationRepository.save(FarmCertification.builder()
                    .farmId(farm.getId())
                    .type(certType)
                    .fileUrl(certFileUrl)
                    .expiryDate(certExpiry)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }
}
