package vn.courses.ut.edu.javaprogramming.bicap.config;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Permission;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.repository.CategoryRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PermissionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ServicePackageRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.math.BigDecimal;
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
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServicePackageRepository servicePackageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final FarmingSeasonRepository farmingSeasonRepository;
    private final ProductRepository productRepository;

    public DatabaseSeeder(PermissionRepository permissionRepository, RoleRepository roleRepository, UserRepository userRepository,
                          FarmRepository farmRepository, FarmCertificationRepository farmCertificationRepository,
                          CategoryRepository categoryRepository, PasswordEncoder passwordEncoder,
                          ServicePackageRepository servicePackageRepository,
                          SubscriptionRepository subscriptionRepository,
                          FarmingSeasonRepository farmingSeasonRepository,
                          ProductRepository productRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.farmCertificationRepository = farmCertificationRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.servicePackageRepository = servicePackageRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.farmingSeasonRepository = farmingSeasonRepository;
        this.productRepository = productRepository;
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

        // 4. Seed Sample Farm Registrations (BICAP-3 — admin approval queue; BICAP-4 — management list)
        seedFarm(farmOwner1.getId(), "Trang Trại Xanh Đồng Nai", "Xã Long An, Huyện Long Thành, Đồng Nai",
                12.5, 10.824610, 107.058112, FarmStatus.PENDING,
                "Trang trại chuyên canh rau sạch theo tiêu chuẩn VietGAP, cung cấp rau ăn lá cho các chuỗi siêu thị khu vực Đông Nam Bộ.",
                "Rau ăn lá, Rau gia vị, Dưa leo",
                "Giấy phép kinh doanh số 0312345678", "https://bicap.vn/docs/dongnai-license.pdf", LocalDate.now().plusYears(5));
        seedFarm(farmOwner2.getId(), "HTX Nông Sản Sạch Lâm Đồng", "Xã Đạ Đờn, Huyện Lâm Hà, Lâm Đồng",
                25.0, 11.811100, 108.366700, FarmStatus.PENDING,
                "Hợp tác xã sản xuất rau củ quả công nghệ cao trên vùng đất cao nguyên, có nhà màng và hệ thống tưới tự động.",
                "Bắp cải, Súp lơ, Cà chua, Dâu tây",
                "Giấy chứng nhận VietGAP", "https://bicap.vn/docs/lamdong-vietgap.pdf", LocalDate.now().plusYears(2));
        Farm farm3 = seedFarm(farmOwner1.getId(), "Trang Trại Hữu Cơ Sông Hồng", "Xã Đan Phượng, Hà Nội",
                8.0, 21.122300, 105.681300, FarmStatus.APPROVED,
                "Trang trại nông nghiệp hữu cơ ngoại thành Hà Nội, chuyên canh tác không hóa chất theo chứng nhận Organic.",
                "Lúa hữu cơ, Rau hữu cơ, Gà thả vườn",
                "Giấy chứng nhận Organic", "https://bicap.vn/docs/hanoi-organic.pdf", LocalDate.now().plusYears(3));
        seedFarm(farmOwner2.getId(), "Vườn Sạch Tiền Giang", "Xã Tân Lập, Huyện Tân Phước, Tiền Giang",
                15.75, 10.467500, 106.209900, FarmStatus.REJECTED,
                "Vườn cây ăn trái miền Tây, tập trung sản xuất trái cây sạch xuất khẩu sang thị trường châu Âu.",
                "Xoài cát Hòa Lộc, Chôm chôm, Sầu riêng",
                "Giấy phép kinh doanh số 0123456789", "https://bicap.vn/docs/tiengiang-license.pdf", LocalDate.now().plusYears(1));

        // 5. Seed default Product Categories (BICAP-5 — product monitoring catalog)
        seedCategory("Rau ăn lá", "Các loại rau ăn lá, rau gia vị", "🥬");
        seedCategory("Củ quả", "Các loại củ, quả", "🥔");
        seedCategory("Trái cây", "Các loại trái cây", "🍎");
        seedCategory("Lúa gạo", "Lúa, gạo, các loại ngũ cốc", "🌾");
        seedCategory("Thủy hải sản", "Cá, tôm, các loại thủy sản", "🐟");
        seedCategory("Thịt - Trứng - Sữa", "Thịt gia súc, gia cầm, trứng, sữa", "🥩");
        seedCategory("Khác", "Các sản phẩm nông nghiệp khác", "📦");

        // 6. Seed service packages and a sample subscription for Farm 3.
        ServicePackage basicPackage = seedServicePackage("BICAP - Cơ Bản", new BigDecimal("100000"));
        seedServicePackage("BICAP - Premium", new BigDecimal("500000"));
        seedSubscription(farm3.getId(), basicPackage);

        // 7. Seed FarmingSeason + Product ACTIVE (BICAP-75 test data)
        //    Dùng Farm 3 (APPROVED) vì chỉ farm APPROVED mới có thể bán hàng
        FarmingSeason season = seedFarmingSeason(farm3.getId(),
                "Vu Rau Xanh 2026", "Rau an la", "Cai xanh huu co",
                5.0, java.time.LocalDate.of(2026, 1, 10));
        Category rauCategory = categoryRepository.findByName("Rau an la")
                .orElse(categoryRepository.findAll().stream().findFirst().orElse(null));
        if (rauCategory != null && season != null) {
            seedProduct(season.getId(), rauCategory.getId(),
                    "Cai xanh huu co BICAP",
                    "Cai xanh trong theo chuan huu co, khong su dung thuoc bao ve thuc vat. Nguon goc ro rang, co chung nhan VietGAP.",
                    15000.0, 500.0);
        }
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

    /**
     * Seeds one product category per unique {@code name}. Existing categories are left
     * untouched (only created when missing), matching the seedPermission behavior so a
     * live catalog edited by an operator is never reverted on reboot.
     */
    private void seedCategory(String name, String description, String icon) {
        categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name(name)
                                .description(description)
                                .icon(icon)
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

        // Security (C-2 / M-15): NEVER overwrite an existing user's password, roles,
        // name, phone or status. A live account that an operator already changed (or
        // that was created through the portal) must not be silently reverted to the
        // seeded demo credentials on every boot. The seeder only creates missing users.
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .fullName(fullName)
                        .phone(phone)
                        .status(UserStatus.ACTIVE)
                        .roles(roles)
                        .build())
        );
    }

    /**
     * Seeds one farm per unique {@code name}. Existing farms are matched by name and
     * backfilled with the current seed values (description, productTypes, status...),
     * so re-running the seeder never duplicates farms nor leaves stale demo data —
     * a farmer may own several farms, so owner-based dedup is intentionally avoided.
     */
    private Farm seedFarm(Long ownerUserId, String name, String address, double area,
                          double gpsLat, double gpsLng, FarmStatus status,
                          String description, String productTypes,
                          String certType, String certFileUrl, LocalDate certExpiry) {
        Farm farm = farmRepository.findByName(name).map(existing -> {
            existing.setUserId(ownerUserId);
            existing.setAddress(address);
            existing.setArea(area);
            existing.setGpsLat(gpsLat);
            existing.setGpsLng(gpsLng);
            existing.setDescription(description);
            existing.setProductTypes(productTypes);
            existing.setStatus(status);
            return farmRepository.save(existing);
        }).orElseGet(() -> farmRepository.save(
                Farm.builder()
                        .userId(ownerUserId)
                        .name(name)
                        .address(address)
                        .area(area)
                        .gpsLat(gpsLat)
                        .gpsLng(gpsLng)
                        .description(description)
                        .productTypes(productTypes)
                        .status(status)
                        .build()
        ));

        boolean hasCertification = !farmCertificationRepository.findByFarmId(farm.getId()).isEmpty();
        if (!hasCertification && certType != null && !certType.isEmpty()) {
            farmCertificationRepository.save(FarmCertification.builder()
                    .farmId(farm.getId())
                    .type(certType)
                    .fileUrl(certFileUrl)
                    .expiryDate(certExpiry)
                    .build());
        }
        return farm;
    }

    private ServicePackage seedServicePackage(String name, BigDecimal price) {
        ServicePackage servicePackage = servicePackageRepository.findByName(name)
                .orElseGet(() -> {
                    // Migrate the previous seed name instead of creating a duplicate basic package.
                    if ("BICAP - Cơ Bản".equals(name)) {
                        return servicePackageRepository.findByName("Goi Dich Vu Co Ban")
                                .orElseGet(ServicePackage::new);
                    }
                    return new ServicePackage();
                });
        servicePackage.setName(name);
        servicePackage.setPrice(price);
        servicePackage.setDurationDays(365);
        servicePackage.setStatus("ACTIVE");
        return servicePackageRepository.save(servicePackage);
    }

    private void seedSubscription(Long farmId, ServicePackage servicePackage) {

        boolean exists = subscriptionRepository.findByFarmIdAndStatus(farmId, SubscriptionStatus.ACTIVE).isPresent();
        if (!exists) {
            Subscription sub = new Subscription();
            sub.setFarmId(farmId);
            sub.setPackageId(servicePackage.getId());
            sub.setStartDate(LocalDate.now());
            sub.setEndDate(LocalDate.now().plusDays(365));
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(sub);
        }
    }

    /**
     * Seeds one FarmingSeason per unique (farmId + name).
     * Dung cho BICAP-75 test data: tao season de co product.
     */
    private FarmingSeason seedFarmingSeason(Long farmId, String name, String productType,
                                             String variety, Double area, java.time.LocalDate startDate) {
        return farmingSeasonRepository.findAll().stream()
                .filter(s -> farmId.equals(s.getFarmId()) && name.equals(s.getName()))
                .findFirst()
                .orElseGet(() -> {
                    FarmingSeason s = new FarmingSeason();
                    s.setFarmId(farmId);
                    s.setName(name);
                    s.setProductType(productType);
                    s.setVariety(variety);
                    s.setArea(area);
                    s.setStartDate(startDate);
                    s.setStatus("HARVESTED"); // san sang len san
                    return farmingSeasonRepository.save(s);
                });
    }

    /**
     * Seeds one Product ACTIVE per unique (seasonId + name).
     * Dung cho BICAP-75 test data: Retailer co the dat mua ngay.
     */
    private void seedProduct(Long seasonId, Long categoryId, String name,
                             String description, double price, double quantity) {
        boolean exists = productRepository.findAll().stream()
                .anyMatch(p -> seasonId.equals(p.getSeasonId()) && name.equals(p.getName()));
        if (!exists) {
            Product p = new Product();
            p.setSeasonId(seasonId);
            p.setCategoryId(categoryId);
            p.setName(name);
            p.setDescription(description);
            p.setPrice(new BigDecimal(String.valueOf(price)));
            p.setQuantity(quantity);
            p.setStatus("ACTIVE");
            productRepository.save(p);
        }
    }
}
