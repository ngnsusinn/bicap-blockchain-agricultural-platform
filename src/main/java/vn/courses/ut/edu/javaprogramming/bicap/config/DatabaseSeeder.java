package vn.courses.ut.edu.javaprogramming.bicap.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Category;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Permission;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Report;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ShipmentTracking;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.repository.CategoryRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PermissionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ReportRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ServicePackageRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ShipmentTrackingRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.VehicleRepository;

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
    private final DriverRepository driverRepository;
        private final VehicleRepository vehicleRepository;
        private final ShipmentRepository shipmentRepository;
        private final ShipmentTrackingRepository shipmentTrackingRepository;
        private final OrderRepository orderRepository;
        private final ReportRepository reportRepository;
        private final NotificationRepository notificationRepository;

    public DatabaseSeeder(PermissionRepository permissionRepository, RoleRepository roleRepository, UserRepository userRepository,
                          FarmRepository farmRepository, FarmCertificationRepository farmCertificationRepository,
                          CategoryRepository categoryRepository, PasswordEncoder passwordEncoder,
                          ServicePackageRepository servicePackageRepository,
                          SubscriptionRepository subscriptionRepository,
                          FarmingSeasonRepository farmingSeasonRepository,
                          ProductRepository productRepository,
                          DriverRepository driverRepository,
                          VehicleRepository vehicleRepository,
                          ShipmentRepository shipmentRepository,
                          ShipmentTrackingRepository shipmentTrackingRepository,
                          OrderRepository orderRepository,
                          ReportRepository reportRepository,
                          NotificationRepository notificationRepository) {
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
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.shipmentRepository = shipmentRepository;
        this.shipmentTrackingRepository = shipmentTrackingRepository;
        this.orderRepository = orderRepository;
        this.reportRepository = reportRepository;
        this.notificationRepository = notificationRepository;
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
        Role shippingMgrRole = seedRole("SHIPPING_MGR", "Shipping Manager for coordinating deliveries", Set.of());
        Role shipDriverRole = seedRole("SHIP_DRIVER", "Shipping Driver for executing shipments", Set.of());
        seedRole("GUEST", "Guest user for browsing products and educational content", Set.of());

        // 3. Seed Users
        seedUser("superadmin@bicap.com", "Superadmin@2026", "Super Admin", "0987654321", superAdminRole);
        seedUser("admin@bicap.com", "Adminpassword@2026", "Admin User", "0912345678", adminRole);
        seedUser("moderator@bicap.com", "Moderator@2026", "Moderator User", "0901234567", moderatorRole);
        User farmOwner1 = seedUser("farm@bicap.com", "Farmpassword@2026", "Chủ Trang Trại BICAP", "0922334455", farmManagerRole);
        User farmOwner2 = seedUser("farm@bicap.vn", "Farmpassword@2026", "Chủ Trang Trại BICAP VN", "0922334456", farmManagerRole);
        seedUser("retailer@bicap.com", "Retailpassword@2026", "Nhà Bán Lẻ BICAP", "0933445566", retailerRole);
        seedUser("retail@bicap.com", "Retailpassword@2026", "Nhà Bán Lẻ BICAP Short", "0933445567", retailerRole);

        // Seed Shipping test users (BICAP-76)
        seedUser("shipping_mgr@bicap.com", "Shipping@2026", "Shipping Manager Test", "0988000001", shippingMgrRole);
        User driverUser = seedUser("driver@bicap.com", "Driver@2026", "Shipping Driver Test", "0988000002", shipDriverRole);
        User driverUser2 = seedUser("driver2@bicap.com", "Driver@2026", "Shipping Driver Available", "0988000003", shipDriverRole);
        Driver seededDriver = seedDriverProfile(driverUser, "012345678901", "B2-000001");
        Driver availableDriver = seedDriverProfile(driverUser2, "012345678902", "B2-000002");

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

        // 6. Seed Subscription for Farm 3
        seedSubscription(farm3.getId());

        // 7. Seed FarmingSeason + Product ACTIVE (BICAP-75 test data)
        //    Dùng Farm 3 (APPROVED) vì chỉ farm APPROVED mới có thể bán hàng
        FarmingSeason season = seedFarmingSeason(farm3.getId(),
                "Vụ Rau Xanh 2026", "Rau ăn lá", "Cải xanh hữu cơ",
                5.0, java.time.LocalDate.of(2026, 1, 10));
        Category rauCategory = categoryRepository.findByName("Rau ăn lá")
                .orElse(categoryRepository.findAll().stream().findFirst().orElse(null));
        if (rauCategory != null && season != null) {
            Product seededProduct = seedProduct(season.getId(), rauCategory.getId(),
                    "Cải xanh hữu cơ BICAP",
                    "Cải xanh trồng theo chuẩn hữu cơ, không sử dụng thuốc bảo vệ thực vật. Nguồn gốc rõ ràng, có chứng nhận VietGAP.",
                    15000.0, 500.0);
            seedShippingTestData(seededProduct, farmOwner1, seedUserByEmail("retailer@bicap.com"),
                    seededDriver, availableDriver, seedUserByEmail("shipping_mgr@bicap.com"));
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

    private void seedSubscription(Long farmId) {
        // Seed 3-tier packages idempotently
        seedServicePackage(
            "Gói Cơ Bản",
            "Phù hợp cho trang trại nhỏ đang bắt đầu số hoá quy trình sản xuất.",
            new BigDecimal("500000"), 365,
            "[\"Quản lý 1 mùa vụ\",\"Ghi nhật ký quy trình (tối đa 10 bước)\",\"Xuất kho & tạo mã QR\",\"Truy xuất nguồn gốc cơ bản\",\"Hỗ trợ email\"]",
            "ACTIVE");
        ServicePackage pro = seedServicePackage(
            "Gói Chuyên Nghiệp",
            "Dành cho trang trại đang mở rộng với nhiều mùa vụ và đối tác bán lẻ.",
            new BigDecimal("1500000"), 365,
            "[\"Không giới hạn mùa vụ\",\"Ghi nhật ký quy trình không giới hạn\",\"Xuất kho & mã QR Blockchain VeChainThor\",\"Tích hợp cảm biến IoT cơ bản\",\"Quản lý đơn hàng & nhà bán lẻ\",\"Theo dõi vận chuyển\",\"Báo cáo phân tích\",\"Hỗ trợ ưu tiên 24/7\"]",
            "ACTIVE");
        seedServicePackage(
            "Gói Doanh Nghiệp",
            "Giải pháp toàn diện cho HTX và trang trại quy mô lớn cần tích hợp sâu Blockchain.",
            new BigDecimal("5000000"), 365,
            "[\"Tất cả tính năng Chuyên Nghiệp\",\"Dashboard IoT nâng cao (nhiệt độ, độ ẩm, pH)\",\"Ghi dữ liệu tự động lên Blockchain VeChainThor\",\"Phân tích nâng cao & báo cáo tùy chỉnh\",\"API tích hợp bên thứ ba\",\"Quản lý đa nông trại\",\"Dedicated Account Manager\",\"SLA 99.9% uptime\"]",
            "ACTIVE");

        // Ensure the demo farm always has the Professional package.
        Subscription activeSubscription = subscriptionRepository
                .findByFarmIdAndStatus(farmId, SubscriptionStatus.ACTIVE)
                .orElse(null);
        if (activeSubscription == null) {
            Subscription sub = new Subscription();
            sub.setFarmId(farmId);
            sub.setPackageId(pro.getId());
            sub.setStartDate(LocalDate.now());
            sub.setEndDate(LocalDate.now().plusDays(365));
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(sub);
        } else if (!pro.getId().equals(activeSubscription.getPackageId())
                || activeSubscription.getEndDate() == null
                || activeSubscription.getStartDate() == null
                || !activeSubscription.getEndDate().equals(activeSubscription.getStartDate().plusDays(365))) {
            activeSubscription.setPackageId(pro.getId());
            LocalDate startDate = activeSubscription.getStartDate() == null
                    ? LocalDate.now()
                    : activeSubscription.getStartDate();
            activeSubscription.setStartDate(startDate);
            activeSubscription.setEndDate(startDate.plusDays(365));
            subscriptionRepository.save(activeSubscription);
        }
    }

    /** Idempotent: creates a ServicePackage only if a package with the same name does not exist. */
    private ServicePackage seedServicePackage(String name, String description,
                                               java.math.BigDecimal price, int durationDays,
                                               String features, String status) {
                return servicePackageRepository.findAll().stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst()
                                .map(existing -> {
                                        if (existing.getDurationDays() != durationDays) {
                                                existing.setDurationDays(durationDays);
                                                return servicePackageRepository.save(existing);
                                        }
                                        return existing;
                                })
                                .orElseGet(() -> {
                    ServicePackage sp = new ServicePackage();
                    sp.setName(name);
                    sp.setDescription(description);
                    sp.setPrice(price);
                    sp.setDurationDays(durationDays);
                    sp.setFeatures(features);
                    sp.setStatus(status);
                    return servicePackageRepository.save(sp);
                });
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
        private Product seedProduct(Long seasonId, Long categoryId, String name,
                                                                String description, double price, double quantity) {
                return productRepository.findAll().stream()
                                .filter(p -> seasonId.equals(p.getSeasonId()) && name.equals(p.getName()))
                                .findFirst()
                                .orElseGet(() -> {
                                        Product p = new Product();
                                        p.setSeasonId(seasonId);
                                        p.setCategoryId(categoryId);
                                        p.setName(name);
                                        p.setDescription(description);
                                        p.setPrice(new BigDecimal(String.valueOf(price)));
                                        p.setQuantity(quantity);
                                        p.setStatus("ACTIVE");
                                        return productRepository.save(p);
                                });
    }

    /**
     * Seeds a Driver profile linked to the given User.
     * Only creates the driver record if one does not already exist for this user (BICAP-76).
     */
        private Driver seedDriverProfile(User user, String citizenId, String licenseNumber) {
                if (user == null) return null;
                Driver existing = driverRepository.findByUserId(user.getId()).orElse(null);
                if (existing != null) return existing;
                if (driverRepository.existsByCitizenId(citizenId)
                                || driverRepository.existsByLicenseNumber(licenseNumber)) return null;

        Driver driver = new Driver();
        driver.setUserId(user.getId());
        driver.setCitizenId(citizenId);
        driver.setLicenseNumber(licenseNumber);
        driver.setStatus(Driver.STATUS_IDLE);
                return driverRepository.save(driver);
    }

        private User seedUserByEmail(String email) {
                return userRepository.findByEmail(email).orElse(null);
        }

        /** Creates a complete, repeatable Shipping demo dataset for BICAP-54 through BICAP-62. */
        private void seedShippingTestData(Product product, User farmOwner, User retailer,
                                                                          Driver seededDriver, Driver availableDriver, User shippingManager) {
                if (product == null || retailer == null || seededDriver == null || availableDriver == null) return;

                Vehicle assignedVehicle = seedVehicle("51A-00001", "Xe tải", 2.5, Vehicle.STATUS_IN_USE);
                seedVehicle("51A-00002", "Xe van", 1.5, Vehicle.STATUS_AVAILABLE);
                seededDriver.setVehicleId(assignedVehicle.getId());
                seededDriver.setStatus(Driver.STATUS_ON_TRIP);
                driverRepository.save(seededDriver);
                availableDriver.setVehicleId(null);
                availableDriver.setStatus(Driver.STATUS_IDLE);
                driverRepository.save(availableDriver);

                seedShippingOrder(product, retailer, "SHIP-DEMO-WAITING", Order.STATUS_DEPOSIT_PAID);
                Order activeOrder = seedShippingOrder(product, retailer, "SHIP-DEMO-ACTIVE", Order.STATUS_IN_TRANSIT);
                Shipment shipment = shipmentRepository.findByOrderId(activeOrder.getId()).orElseGet(() -> {
                        Shipment created = new Shipment();
                        created.setOrderId(activeOrder.getId());
                        created.setDriverId(seededDriver.getId());
                        created.setVehicleId(assignedVehicle.getId());
                        created.setStatus(Shipment.STATUS_PICKING_UP);
                        created.setRouteSummary("Kho Sông Hồng → Trung tâm phân phối Hà Nội");
                        return shipmentRepository.save(created);
                });

                seedTracking(shipment.getId(), "PICKUP_CONFIRMED", 21.1223, 105.6813, "Tài xế đã nhận hàng tại trang trại.");
                seedTracking(shipment.getId(), "REPORT_DELAY", 21.0285, 105.8542, "Giao thông đông, dự kiến trễ 30 phút.");

                if (shippingManager != null) {
                        seedReport(shippingManager.getId(), "SHIPPING_MGR", "INCIDENT", "Cập nhật lô vận chuyển demo",
                                        "Lô demo đang được theo dõi để kiểm thử quy trình vận chuyển.", activeOrder.getId());
                }
                seedNotification(farmOwner, "Cập nhật vận chuyển", "Lô hàng demo đã được tiếp nhận và đang trên đường giao.");
                seedNotification(retailer, "Cập nhật đơn hàng", "Đơn hàng demo đã được bàn giao cho đơn vị vận chuyển.");
        }

        private Vehicle seedVehicle(String licensePlate, String type, double capacity, String status) {
                return vehicleRepository.findByLicensePlate(licensePlate).orElseGet(() -> {
                        Vehicle vehicle = new Vehicle();
                        vehicle.setLicensePlate(licensePlate);
                        vehicle.setType(type);
                        vehicle.setCapacity(capacity);
                        vehicle.setStatus(status);
                        return vehicleRepository.save(vehicle);
                });
        }

        private Order seedShippingOrder(Product product, User retailer, String marker, String status) {
                return orderRepository.findAll().stream()
                                .filter(order -> marker.equals(order.getDepositCode()))
                                .findFirst()
                                .orElseGet(() -> {
                                        Order order = new Order();
                                        order.setProductId(product.getId());
                                        order.setRetailerId(retailer.getId());
                                        order.setQuantity(20.0);
                                        order.setPrice(product.getPrice());
                                        order.setDepositRate(0.3);
                                        order.setDepositAmount(product.getPrice().multiply(BigDecimal.valueOf(20.0)).multiply(BigDecimal.valueOf(0.3)));
                                        order.setDepositCode(marker);
                                        order.setStatus(status);
                                        order.setDeliveryAddr("12 Trần Duy Hưng, Hà Nội");
                                        order.setDesiredDeliveryDate(LocalDate.now().plusDays(3));
                                        order.setNotes("Dữ liệu demo Shipping để kiểm thử.");
                                        order.setAcceptedAt(java.time.LocalDateTime.now().minusHours(2));
                                        return orderRepository.save(order);
                                });
        }

        private void seedTracking(Long shipmentId, String status, double lat, double lng, String notes) {
                boolean exists = shipmentTrackingRepository.findAll().stream()
                                .anyMatch(t -> shipmentId.equals(t.getShipmentId()) && status.equals(t.getStatus()));
                if (!exists) {
                        ShipmentTracking tracking = new ShipmentTracking();
                        tracking.setShipmentId(shipmentId);
                        tracking.setStatus(status);
                        tracking.setGpsLat(lat);
                        tracking.setGpsLng(lng);
                        tracking.setNotes(notes);
                        shipmentTrackingRepository.save(tracking);
                }
        }

        private void seedReport(Long reporterId, String role, String type, String subject,
                                                        String content, Long relatedOrderId) {
                boolean exists = reportRepository.findAll().stream()
                                .anyMatch(r -> reporterId.equals(r.getReporterId()) && subject.equals(r.getSubject()));
                if (!exists) {
                        Report report = new Report();
                        report.setReporterId(reporterId);
                        report.setReporterRole(role);
                        report.setType(type);
                        report.setSubject(subject);
                        report.setContent(content);
                        report.setRelatedOrderId(relatedOrderId);
                        report.setStatus(Report.STATUS_OPEN);
                        reportRepository.save(report);
                }
        }

        private void seedNotification(User recipient, String title, String content) {
                if (recipient == null) return;
                boolean exists = notificationRepository.findByUserIdOrderByCreatedAtDesc(recipient.getId()).stream()
                                .anyMatch(n -> title.equals(n.getTitle()));
                if (!exists) {
                        notificationRepository.save(Notification.builder()
                                        .userId(recipient.getId())
                                        .type("SHIPPING")
                                        .title(title)
                                        .content(content)
                                        .channel("IN_APP")
                                        .isRead(false)
                                        .build());
                }
        }
}
