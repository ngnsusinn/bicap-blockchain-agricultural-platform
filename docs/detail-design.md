# TÀI LIỆU THIẾT KẾ CHI TIẾT (DETAIL DESIGN DOCUMENT)

## DỰ ÁN: TÍCH HỢP BLOCKCHAIN TRONG SẢN XUẤT NÔNG SẢN SẠCH (BICAP)

| Thông tin | Chi tiết |
|---|---|
| **Tên dự án (EN)** | Blockchain Integration in Clean Agricultural Production |
| **Tên dự án (VN)** | Tích hợp Blockchain trong sản xuất nông sản sạch |
| **Viết tắt** | BICAP |
| **Loại tài liệu** | Detail Design Document |
| **Phiên bản tài liệu** | 1.0 |
| **Ngày tạo** | 02/08/2026 |
| **Trạng thái** | Bản nháp (Draft) |
| **Tham chiếu** | User Requirements v2.0, SRS v1.1, Architecture Design v1.0, Database Setup v1.0 |
| **Mã Jira** | BICAP-94 |

---

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Thiết kế chi tiết Backend API](#2-thiết-kế-chi-tiết-backend-api)
   - 2.1. [Cấu trúc Package và Class Diagram tổng quan](#21-cấu-trúc-package-và-class-diagram-tổng-quan)
   - 2.2. [Module Auth (Xác thực & Phân quyền)](#22-module-auth-xác-thực--phân-quyền)
   - 2.3. [Module Admin (Quản trị hệ thống)](#23-module-admin-quản-trị-hệ-thống)
   - 2.4. [Module Farm Management (Quản lý nông trại)](#24-module-farm-management-quản-lý-nông-trại)
   - 2.5. [Module Retailer (Nhà bán lẻ)](#25-module-retailer-nhà-bán-lẻ)
   - 2.6. [Module Order (Đơn hàng & Giao dịch)](#26-module-order-đơn-hàng--giao-dịch)
   - 2.7. [Module Shipping (Quản lý vận chuyển)](#27-module-shipping-quản-lý-vận-chuyển)
   - 2.8. [Module Payment (Thanh toán)](#28-module-payment-thanh-toán)
   - 2.9. [Module Blockchain (Tích hợp VeChainThor)](#29-module-blockchain-tích-hợp-vechainthor)
   - 2.10. [Module Notification (Thông báo)](#210-module-notification-thông-báo)
   - 2.11. [Module IoT (Cảm biến)](#211-module-iot-cảm-biến)
   - 2.12. [Module Content & Report](#212-module-content--report)
3. [Thiết kế chi tiết Cơ sở dữ liệu](#3-thiết-kế-chi-tiết-cơ-sở-dữ-liệu)
   - 3.1. [Entity-Relationship Diagram (ERD) chi tiết](#31-entity-relationship-diagram-erd-chi-tiết)
   - 3.2. [Mô tả chi tiết từng bảng](#32-mô-tả-chi-tiết-từng-bảng)
   - 3.3. [Chiến lược Indexing](#33-chiến-lược-indexing)
   - 3.4. [Redis Cache Schema](#34-redis-cache-schema)
4. [Thiết kế chi tiết Frontend Web Apps](#4-thiết-kế-chi-tiết-frontend-web-apps)
   - 4.1. [Cấu trúc Component tổng quan](#41-cấu-trúc-component-tổng-quan)
   - 4.2. [Admin Web App](#42-admin-web-app)
   - 4.3. [Farm Management Web App](#43-farm-management-web-app)
   - 4.4. [Retailer Web App](#44-retailer-web-app)
   - 4.5. [Shipping Management Web App](#45-shipping-management-web-app)
   - 4.6. [Shipping Driver Mobile App](#46-shipping-driver-mobile-app)
   - 4.7. [Guest App](#47-guest-app)
5. [Thiết kế chi tiết Smart Contract](#5-thiết-kế-chi-tiết-smart-contract)
   - 5.1. [FarmingSeasonContract](#51-farmingseasoncontract)
   - 5.2. [FarmingProcessContract](#52-farmingprocesscontract)
   - 5.3. [ExportContract](#53-exportcontract)
   - 5.4. [TraceabilityContract](#54-traceabilitycontract)
6. [Đặc tả chi tiết REST API](#6-đặc-tả-chi-tiết-rest-api)
   - 6.1. [API Naming Convention](#61-api-naming-convention)
   - 6.2. [Auth API](#62-auth-api)
   - 6.3. [Admin API](#63-admin-api)
   - 6.4. [Farm API](#64-farm-api)
   - 6.5. [Retailer API](#65-retailer-api)
   - 6.6. [Order API](#66-order-api)
   - 6.7. [Shipping API](#67-shipping-api)
   - 6.8. [Payment API](#68-payment-api)
   - 6.9. [Blockchain API](#69-blockchain-api)
   - 6.10. [Notification API](#610-notification-api)
   - 6.11. [IoT API](#611-iot-api)
7. [Sequence Diagram cho các luồng nghiệp vụ chính](#7-sequence-diagram-cho-các-luồng-nghiệp-vụ-chính)
   - 7.1. [Luồng đăng ký và xác thực](#71-luồng-đăng-ký-và-xác-thực)
   - 7.2. [Luồng tạo mùa vụ và ghi Blockchain](#72-luồng-tạo-mùa-vụ-và-ghi-blockchain)
   - 7.3. [Luồng đặt mua và thanh toán](#73-luồng-đặt-mua-và-thanh-toán)
   - 7.4. [Luồng vận chuyển và giao nhận](#74-luồng-vận-chuyển-và-giao-nhận)
   - 7.5. [Luồng truy xuất nguồn gốc QR Code](#75-luồng-truy-xuất-nguồn-gốc-qr-code)
8. [Thiết kế bảo mật chi tiết](#8-thiết-kế-bảo-mật-chi-tiết)
9. [Phụ lục](#9-phụ-lục)

---

## 1. Giới thiệu

### 1.1. Mục đích tài liệu

Tài liệu Thiết kế Chi tiết (Detail Design Document) này mô tả chi tiết thiết kế kỹ thuật của từng module trong hệ thống BICAP, bao gồm:

- **Class Diagram** cho các lớp backend (Entity, Repository, Service, Controller, DTO)
- **Component Diagram** cho các ứng dụng Frontend
- **Sequence Diagram** cho các luồng nghiệp vụ chính
- **Database Schema** chi tiết cho toàn bộ 23 bảng
- **API Contract** cho tất cả các endpoint REST
- **Smart Contract Design** cho các hợp đồng thông minh trên VeChainThor

### 1.2. Đối tượng đọc

| Đối tượng | Mục đích sử dụng |
|-----------|-----------------|
| **Developers** | Triển khai code theo thiết kế chi tiết |
| **Testers** | Xây dựng test case dựa trên sequence diagram và API contract |
| **Kiến trúc sư** | Kiểm tra tính nhất quán giữa thiết kế tổng quan và chi tiết |
| **Project Manager** | Theo dõi tiến độ triển khai từng module |

### 1.3. Tài liệu tham chiếu

| STT | Tài liệu | Phiên bản | Mã Jira |
|-----|----------|-----------|---------|
| 1 | User Requirements Document | 2.0 | BICAP-91 |
| 2 | Software Requirement Specifications (SRS) | 1.1 | BICAP-92 |
| 3 | Architecture Design Document | 1.0 | BICAP-93 |
| 4 | Database & Cache Configuration | 1.0 | BICAP-79 |

### 1.4. Quy ước ký hiệu UML

| Sơ đồ | Công cụ | Quy ước |
|-------|---------|---------|
| Class Diagram | UML 2.0 | Mũi tên thực (association), mũi tên rỗng (inheritance), diamond (aggregation/composition) |
| Sequence Diagram | UML 2.0 | Vertical lifeline, horizontal message arrows, activation boxes |
| ERD | Crow's Foot | PK: khóa chính, FK: khóa ngoại, solid line: quan hệ bắt buộc |

---

## 2. Thiết kế chi tiết Backend API

### 2.1. Cấu trúc Package và Class Diagram tổng quan

#### 2.1.1. Cấu trúc Package

```
vn.courses.ut.edu.javaprogramming.bicap/
│
├── Application.java                         # Main entry point (@SpringBootApplication)
│
├── config/                                  # Configuration classes
│   ├── SecurityConfig.java                  # Spring Security + JWT + RBAC
│   ├── CorsConfig.java                      # CORS configuration
│   ├── DatabaseSeeder.java                  # Seed data (roles, permissions, admin accounts)
│   └── SepayConfig.java                     # Sepay payment gateway configuration
│
├── common/                                  # Cross-cutting shared components
│   └── security/                            # Security infrastructure
│       ├── JwtTokenProvider.java            # JWT generation & validation
│       ├── JwtAuthenticationFilter.java     # JWT extraction from HTTP header
│       └── CustomUserDetailsService.java    # UserDetails loading from database
│
├── entity/                                  # JPA Entity classes (19 entities)
│   ├── User.java                            # Implements UserDetails for Spring Security
│   ├── UserStatus.java                      # Enum: ACTIVE, INACTIVE, SUSPENDED
│   ├── Role.java                            # User roles
│   ├── Permission.java                      # Granular permissions
│   ├── Order.java                           # Purchase orders
│   ├── Payment.java                         # Payment records
│   ├── PaymentMethod.java                   # Enum: VNPAY, STRIPE, METAMASK
│   ├── PaymentStatus.java                   # Enum: PENDING, COMPLETED, FAILED, REFUNDED
│   ├── ServicePackage.java                  # Service subscription packages
│   ├── Subscription.java                    # Farm package subscriptions
│   ├── SubscriptionStatus.java              # Enum: ACTIVE, EXPIRED, CANCELLED
│   ├── Farm.java                            # (planned/partial)
│   ├── FarmingSeason.java                   # (planned/partial)
│   ├── FarmingProcess.java                  # (planned/partial)
│   ├── Product.java                         # (planned/partial)
│   ├── Shipment.java                        # (planned/partial)
│   ├── ShipmentTracking.java                # (planned/partial)
│   ├── Driver.java                          # (planned/partial)
│   ├── Vehicle.java                         # (planned/partial)
│   ├── QRCode.java                          # (planned/partial)
│   ├── BlockchainTransaction.java           # (planned/partial)
│   ├── Notification.java                    # (planned/partial)
│   ├── Report.java                          # (planned/partial)
│   └── IoTData.java                         # (planned/partial)
│
├── repository/                              # Spring Data JPA Repositories
│   ├── UserRepository.java
│   ├── RoleRepository.java
│   ├── PermissionRepository.java
│   ├── OrderRepository.java
│   ├── PaymentRepository.java
│   ├── ServicePackageRepository.java
│   └── SubscriptionRepository.java
│
├── dto/                                     # Data Transfer Objects
│   ├── LoginRequest.java                    # email/phone + password
│   ├── RegisterRequest.java                 # fullName + email + phone + password
│   ├── AuthResponse.java                    # accessToken + refreshToken + user info
│   ├── AdminCreateRequest.java              # Admin account creation fields
│   ├── AdminUpdateRequest.java              # Admin account update fields
│   ├── AdminResponse.java                   # Admin account detail output
│   ├── CreateDepositRequest.java            # Deposit payment request
│   ├── DepositResponse.java                 # Deposit result
│   ├── PurchasePackageRequest.java          # Service package purchase
│   ├── PurchasePackageResponse.java         # Purchase result with payment URL
│   ├── ServicePackageResponse.java          # Service package info
│   ├── SubscriptionResponse.java            # Subscription info
│   ├── PaymentStatusResponse.java           # Payment status check result
│   └── SepayWebhookRequest.java             # Sepay callback payload
│
├── controller/                              # REST Controllers
│   ├── AuthController.java                  # /api/auth/*
│   ├── AdminController.java                 # /api/admin/*
│   ├── OrderController.java                 # /api/orders/*
│   ├── ServicePackageController.java        # /api/service-packages/*
│   ├── SubscriptionController.java          # /api/subscriptions/*
│   └── SepayWebhookController.java          # /api/webhooks/sepay/*
│
├── service/                                 # Business Logic Services
│   ├── AuthService.java                     # Login, Registration, Token management
│   ├── AdminService.java                    # Admin CRUD, role assignment
│   ├── OrderService.java                    # Order CRUD, status transitions
│   ├── ServicePackageService.java           # Package listing and management
│   ├── SubscriptionService.java             # Subscription lifecycle
│   └── SepayService.java                    # Payment processing, webhook handling
│
└── exception/                               # Global Error Handling
    ├── GlobalExceptionHandler.java          # @ControllerAdvice
    ├── ErrorResponse.java                   # Standardized error response format
    ├── BadRequestException.java             # HTTP 400
    ├── UnauthorizedException.java           # HTTP 401
    ├── ForbiddenException.java              # HTTP 403
    ├── ResourceNotFoundException.java       # HTTP 404
    └── ConflictException.java               # HTTP 409
```

#### 2.1.2. Class Diagram — Tổng quan Entity

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              ENTITY CLASS HIERARCHY                                    │
│                                                                                       │
│  ┌───────────────────┐     ┌───────────────────┐     ┌─────────────────────────────┐ │
│  │      User         │────►│      Role         │────►│       Permission            │ │
│  │ (UserDetails)     │ M:N │                   │ M:N │                              │ │
│  │───────────────────│     │───────────────────│     │──────────────────────────────│ │
│  │ - id: Long        │     │ - id: Long        │     │ - id: Long                   │ │
│  │ - email: String   │     │ - name: String    │     │ - code: String (UNIQUE)      │ │
│  │ - password: String│     │ - description: Str│     │ - description: String        │ │
│  │ - fullName: String│     │ - permissions:Set │     │ - createdAt: Timestamp       │ │
│  │ - phone: String   │     │ - createdAt: TS   │     │ - updatedAt: Timestamp       │ │
│  │ - status: UserSts │     │ - updatedAt: TS   │     └──────────────────────────────┘ │
│  │ - avatarUrl: Str  │     └───────────────────┘                                      │
│  │ - roles: Set<Role>│                                                                 │
│  │ - createdAt: TS   │                                                                 │
│  │ - updatedAt: TS   │                                                                 │
│  └────────┬──────────┘                                                                 │
│           │ 1:N                                                                        │
│           ▼                                                                            │
│  ┌───────────────────┐     ┌───────────────────┐     ┌─────────────────────────────┐ │
│  │      Farm         │────►│ FarmCertification │     │      Subscription           │ │
│  │───────────────────│ 1:N │───────────────────│     │──────────────────────────────│ │
│  │ - id: Long        │     │ - id: Long        │     │ - id: Long                   │ │
│  │ - userId: Long(FK)│     │ - farmId: Long(FK)│     │ - farmId: Long (FK)          │ │
│  │ - name: String    │     │ - type: String    │     │ - packageId: Long (FK)       │ │
│  │ - address: String │     │ - fileUrl: String │     │ - startDate: Date            │ │
│  │ - area: Double    │     │ - expiryDate: Date│     │ - endDate: Date              │ │
│  │ - gpsLat: Double  │     │ - createdAt: TS   │     │ - status: SubStatus          │ │
│  │ - gpsLng: Double  │     └───────────────────┘     │ - createdAt: Timestamp       │ │
│  │ - status: String  │                                └──────────────┬───────────────┘ │
│  │ - createdAt: TS   │                                               │                 │
│  └────────┬──────────┘                              ┌────────────────┴──────────────┐ │
│           │ 1:N                                     │      ServicePackage           │ │
│           ▼                                         │───────────────────────────────│ │
│  ┌────────────────────┐    ┌──────────────────┐    │ - id: Long                    │ │
│  │  FarmingSeason     │───►│FarmingProcess    │    │ - name: String                │ │
│  │────────────────────│1:N │───────────────────│    │ - description: Text           │ │
│  │ - id: Long         │    │ - id: Long        │    │ - price: BigDecimal           │ │
│  │ - farmId: Long(FK) │    │ - seasonId: L(FK) │    │ - durationDays: Int           │ │
│  │ - name: String     │    │ - processType: Str│    │ - features: JSON              │ │
│  │ - productType: Str │    │ - executionDate:Dt│    │ - status: String              │ │
│  │ - variety: String  │    │ - materials: JSON │    │ - createdAt: Timestamp        │ │
│  │ - area: Double     │    │ - images: JSON    │    └───────────────────────────────┘ │
│  │ - startDate: Date  │    │ - notes: Text     │                                     │
│  │ - endDate: Date    │    │ - txHash: String  │                                     │
│  │ - status: String   │    │ - createdAt: TS   │                                     │
│  │ - txHash: String   │    └───────────────────┘                                     │
│  │ - createdAt: TS    │                                                               │
│  └────────┬───────────┘                                                               │
│           │ 1:N                                                                        │
│           ▼                                                                            │
│  ┌───────────────────┐     ┌───────────────────┐     ┌─────────────────────────────┐ │
│  │    Product        │────►│      Order        │────►│        Payment              │ │
│  │───────────────────│ 1:N │───────────────────│ 1:N │──────────────────────────────│ │
│  │ - id: Long        │     │ - id: Long        │     │ - id: Long                   │ │
│  │ - seasonId: L(FK) │     │ - productId: L(FK)│     │ - orderId: Long (FK)         │ │
│  │ - categoryId: L   │     │ - retailerId:Long │     │ - amount: BigDecimal         │ │
│  │ - name: String    │     │ - quantity: Double│     │ - method: PaymentMethod      │ │
│  │ - description: Txt│     │ - price: BigDec   │     │ - status: PaymentStatus      │ │
│  │ - price: BigDec   │     │ - status: String  │     │ - txRef: String              │ │
│  │ - quantity: Double│     │ - deliveryAddr:Str│     │ - createdAt: Timestamp       │ │
│  │ - qrCodeId: L(FK) │     │ - depositRate: Dbl│     └──────────────────────────────┘ │
│  │ - status: String  │     │ - createdAt: TS   │                                      │
│  │ - createdAt: TS   │     └────────┬──────────┘                                      │
│  └───────────────────┘              │ 1:1                                              │
│                                     ▼                                                  │
│  ┌───────────────────┐     ┌───────────────────┐     ┌─────────────────────────────┐ │
│  │    QRCode         │     │    Shipment       │────►│   ShipmentTracking          │ │
│  │───────────────────│     │───────────────────│ 1:N │──────────────────────────────│ │
│  │ - id: Long        │     │ - id: Long        │     │ - id: Long                   │ │
│  │ - traceUrl: String│     │ - orderId: L (UQ) │     │ - shipmentId: Long (FK)      │ │
│  │ - qrImage: String │     │ - driverId: L(FK) │     │ - status: String             │ │
│  │ - seasonId: L(FK) │     │ - vehicleId: L(FK)│     │ - gpsLat: Double             │ │
│  │ - createdAt: TS   │     │ - status: String  │     │ - gpsLng: Double             │ │
│  └───────────────────┘     │ - pickupTime: DT  │     │ - images: JSON               │ │
│                            │ - deliveryTime: DT│     │ - notes: Text                │ │
│                            │ - routeSummary:Str│     │ - timestamp: Timestamp       │ │
│                            │ - createdAt: TS   │     └──────────────────────────────┘ │
│                            └───────────────────┘                                      │
│                                                                                       │
│  ┌───────────────────┐     ┌───────────────────┐     ┌─────────────────────────────┐ │
│  │     Driver        │     │     Vehicle       │     │   BlockchainTransaction     │ │
│  │───────────────────│     │───────────────────│     │──────────────────────────────│ │
│  │ - id: Long        │     │ - id: Long        │     │ - id: Long                   │ │
│  │ - userId: L(UN,FK)│     │ - licensePlate:Str│     │ - entityType: String         │ │
│  │ - citizenId: Str  │◄───►│ - type: String    │     │ - entityId: Long             │ │
│  │ - licenseNumber:St│ 1:1 │ - capacity: Double│     │ - txHash: String (UNIQUE)    │ │
│  │ - vehicleId: L(FK)│     │ - status: String  │     │ - contractAddress: String    │ │
│  │ - status: String  │     │ - createdAt: TS   │     │ - status: String             │ │
│  │ - createdAt: TS   │     └───────────────────┘     │ - retryCount: Int            │ │
│  └───────────────────┘                                │ - idempotencyKey: Str(UNIQUE)│ │
│                                                       │ - createdAt: Timestamp       │ │
│  ┌───────────────────┐     ┌───────────────────┐     └──────────────────────────────┘ │
│  │  Notification     │     │     Report        │                                      │
│  │───────────────────│     │───────────────────│     ┌─────────────────────────────┐ │
│  │ - id: Long        │     │ - id: Long        │     │        IoTData              │ │
│  │ - userId: Long(FK)│     │ - userId: Long(FK)│     │──────────────────────────────│ │
│  │ - type: String    │     │ - type: String    │     │ - id: Long                   │ │
│  │ - title: String   │     │ - title: String   │     │ - farmId: Long (FK)          │ │
│  │ - content: Text   │     │ - content: Text   │     │ - temperature: Double        │ │
│  │ - channel: String │     │ - attachments:JSON│     │ - humidity: Double           │ │
│  │ - isRead: Boolean │     │ - status: String  │     │ - ph: Double                 │ │
│  │ - createdAt: TS   │     │ - adminResponse:T│     │ - measuredAt: Timestamp      │ │
│  └───────────────────┘     │ - createdAt: TS   │     └─────────────────────────────┘ │
│                            └───────────────────┘                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

#### 2.1.3. Class Diagram — Service Layer (đã triển khai)

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              SERVICE LAYER — IMPLEMENTED                               │
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │                          <<Service>> AuthService                                  ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - userRepository: UserRepository                                                  ││
│  │ - roleRepository: RoleRepository                                                  ││
│  │ - passwordEncoder: PasswordEncoder                                                ││
│  │ - jwtTokenProvider: JwtTokenProvider                                              ││
│  │ - authenticationManager: AuthenticationManager                                    ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + register(RegisterRequest): AuthResponse                                         ││
│  │ + login(LoginRequest): AuthResponse                                               ││
│  │ + refreshToken(String): AuthResponse                                              ││
│  │ + validateToken(String): boolean                                                  ││
│  │ + getCurrentUser(): User                                                          ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │                          <<Service>> AdminService                                 ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - userRepository: UserRepository                                                  ││
│  │ - roleRepository: RoleRepository                                                  ││
│  │ - passwordEncoder: PasswordEncoder                                                ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + createAdmin(AdminCreateRequest): AdminResponse                                  ││
│  │ + getAllAdmins(Pageable): Page<AdminResponse>                                     ││
│  │ + getAdminById(Long): AdminResponse                                               ││
│  │ + updateAdmin(Long, AdminUpdateRequest): AdminResponse                            ││
│  │ + deleteAdmin(Long): void                                                         ││
│  │ + getRoles(): List<Role>                                                          ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │                          <<Service>> OrderService                                 ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - orderRepository: OrderRepository                                                ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + createOrder(OrderRequest): OrderResponse                                        ││
│  │ + getOrders(Pageable): Page<OrderResponse>                                        ││
│  │ + getOrderById(Long): OrderResponse                                               ││
│  │ + updateOrderStatus(Long, String): OrderResponse                                  ││
│  │ + cancelOrder(Long, String): OrderResponse                                        ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │                        <<Service>> ServicePackageService                          ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - packageRepository: ServicePackageRepository                                     ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + getAllPackages(): List<ServicePackageResponse>                                  ││
│  │ + getPackageById(Long): ServicePackageResponse                                    ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │                        <<Service>> SubscriptionService                            ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - subscriptionRepository: SubscriptionRepository                                  ││
│  │ - packageRepository: ServicePackageRepository                                     ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + purchasePackage(PurchasePackageRequest): PurchasePackageResponse                ││
│  │ + getSubscriptions(Long farmId): List<SubscriptionResponse>                       ││
│  │ + getActiveSubscription(Long farmId): SubscriptionResponse                        ││
│  │ + cancelSubscription(Long): void                                                  ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │                          <<Service>> SepayService                                 ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - sepayConfig: SepayConfig                                                        ││
│  │ - paymentRepository: PaymentRepository                                            ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + createDeposit(CreateDepositRequest): DepositResponse                            ││
│  │ + handleWebhook(SepayWebhookRequest): void                                        ││
│  │ + checkPaymentStatus(String): PaymentStatusResponse                               ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────────┘
```

#### 2.1.4. Class Diagram — Controller Layer (đã triển khai)

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              CONTROLLER LAYER — IMPLEMENTED                            │
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │           @RestController  AuthController  (/api/auth)                           ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - authService: AuthService                                                        ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + POST   /api/auth/register       → register(@Valid RegisterRequest): AuthResp   ││
│  │ + POST   /api/auth/login          → login(@Valid LoginRequest): AuthResp         ││
│  │ + POST   /api/auth/refresh-token  → refreshToken(@RequestBody): AuthResp         ││
│  │ + GET    /api/auth/me             → getCurrentUser(): User                       ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │           @RestController  AdminController  (/api/admin)                         ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - adminService: AdminService                                                      ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + POST   /api/admin              → createAdmin(@Valid AdminCreateReq): AdminResp ││
│  │ + GET    /api/admin              → getAllAdmins(Pageable): Page<AdminResponse>   ││
│  │ + GET    /api/admin/{id}         → getAdminById(@PathVariable Long): AdminResp   ││
│  │ + PUT    /api/admin/{id}         → updateAdmin(@PathVariable, @Valid AdminUpd)   ││
│  │ + DELETE /api/admin/{id}         → deleteAdmin(@PathVariable Long): void         ││
│  │ + GET    /api/admin/roles        → getRoles(): List<Role>                        ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │           @RestController  OrderController  (/api/orders)                        ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - orderService: OrderService                                                      ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + GET    /api/orders             → getOrders(Pageable, @RequestParam filters)    ││
│  │ + GET    /api/orders/{id}        → getOrderById(@PathVariable Long)              ││
│  │ + POST   /api/orders             → createOrder(@Valid CreateOrderReq)             ││
│  │ + PUT    /api/orders/{id}/status → updateStatus(@PathVariable, @RequestBody)     ││
│  │ + PUT    /api/orders/{id}/cancel → cancelOrder(@PathVariable, @RequestBody)      ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │     @RestController  ServicePackageController  (/api/service-packages)           ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - packageService: ServicePackageService                                           ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + GET    /api/service-packages    → getAllPackages(): List<SPResponse>           ││
│  │ + GET    /api/service-packages/{id}→ getPackageById(@PathVariable Long)           ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │     @RestController  SubscriptionController  (/api/subscriptions)                ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - subscriptionService: SubscriptionService                                        ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + POST   /api/subscriptions/purchase → purchase(@Valid PurchasePackageReq)       ││
│  │ + GET    /api/subscriptions          → getSubscriptions(): List<SubResponse>     ││
│  │ + GET    /api/subscriptions/{id}     → getSubscription(@PathVariable Long)       ││
│  │ + PUT    /api/subscriptions/{id}/cancel→ cancel(@PathVariable Long)              ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
│                                                                                       │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐│
│  │     @RestController  SepayWebhookController  (/api/webhooks/sepay)               ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ - sepayService: SepayService                                                      ││
│  │───────────────────────────────────────────────────────────────────────────────────││
│  │ + POST   /api/webhooks/sepay  → handleWebhook(@RequestBody SepayWebhookReq):void ││
│  └──────────────────────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.2. Module Auth (Xác thực & Phân quyền)

#### 2.2.1. Class Diagram chi tiết

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE AUTH — DETAILED CLASS DIAGRAM                     │
│                                                                                       │
│  ┌─────────────────────────┐     ┌─────────────────────────────────────────────────┐ │
│  │     LoginRequest        │     │          RegisterRequest                        │ │
│  │─────────────────────────│     │─────────────────────────────────────────────────│ │
│  │ - emailOrPhone: String  │     │ - fullName: String (@NotBlank)                  │ │
│  │ - password: String      │     │ - email: String (@Email, @NotBlank)             │ │
│  └─────────────────────────┘     │ - phone: String (@Pattern)                      │ │
│                                  │ - password: String (@Size(min=8))                │ │
│  ┌─────────────────────────┐     └─────────────────────────────────────────────────┘ │
│  │     AuthResponse        │                                                         │
│  │─────────────────────────│     ┌─────────────────────────────────────────────────┐ │
│  │ - accessToken: String   │     │        JwtTokenProvider                         │ │
│  │ - refreshToken: String  │     │─────────────────────────────────────────────────│ │
│  │ - tokenType: String     │     │ - jwtSecret: String (@Value)                    │ │
│  │ - userId: Long          │     │ - jwtExpirationMs: int (@Value)                 │ │
│  │ - email: String         │     │ - refreshExpirationMs: int (@Value)             │ │
│  │ - fullName: String      │     │─────────────────────────────────────────────────│ │
│  │ - roles: List<String>   │     │ + generateAccessToken(UserDetails): String      │ │
│  │ - avatarUrl: String     │     │ + generateRefreshToken(UserDetails): String     │ │
│  └─────────────────────────┘     │ + validateToken(String): boolean                │ │
│                                  │ + getUsernameFromToken(String): String          │ │
│  ┌─────────────────────────┐     │ + getRolesFromToken(String): List<String>      │ │
│  │  JwtAuthenticationFilter│     └─────────────────────────────────────────────────┘ │
│  │─────────────────────────│                                                         │
│  │ - jwtTokenProvider      │     ┌─────────────────────────────────────────────────┐ │
│  │ - userDetailsService    │     │     CustomUserDetailsService                    │ │
│  │─────────────────────────│     │─────────────────────────────────────────────────│ │
│  │ + doFilterInternal(     │     │ - userRepository: UserRepository                │ │
│  │    request, response,   │     │─────────────────────────────────────────────────│ │
│  │    filterChain): void   │     │ + loadUserByUsername(String): UserDetails       │ │
│  └─────────────────────────┘     └─────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                         SecurityConfig                                          │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │ + passwordEncoder(): PasswordEncoder        → BCryptPasswordEncoder             │ │
│  │ + authenticationProvider(): DaoAuthProvider  → CustomUserDetailsService + BCrypt│ │
│  │ + authenticationManager(AuthConfig): AuthMgr → Standard auth manager            │ │
│  │ + filterChain(HttpSecurity): SecurityFilter  → STATELESS session, permit auth/* │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

#### 2.2.2. RBAC Authorization Model

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         ROLE-BASED ACCESS CONTROL (RBAC)                              │
│                                                                                       │
│  ┌───────────────┐     ┌───────────────────────────────────────────────────────────┐ │
│  │    Roles      │     │                    Permissions                            │ │
│  ├───────────────┤     ├───────────────────────────────────────────────────────────┤ │
│  │ SUPER_ADMIN   │────►│ ADMIN_CREATE, ADMIN_READ, ADMIN_UPDATE, ADMIN_DELETE      │ │
│  │               │     │ FARM_APPROVE, CONTRACT_DEPLOY, CONTRACT_MANAGE            │ │
│  │               │     │ FULL_SYSTEM_ACCESS                                       │ │
│  │               │     │                                                           │ │
│  │ ADMIN         │────►│ ADMIN_CREATE, ADMIN_READ, ADMIN_UPDATE                    │ │
│  │               │     │ FARM_READ, FARM_APPROVE, PRODUCT_MANAGE                   │ │
│  │               │     │ CONTRACT_READ, REPORT_MANAGE                              │ │
│  │               │     │                                                           │ │
│  │ MODERATOR     │────►│ ADMIN_READ, FARM_READ, PRODUCT_READ, REPORT_READ          │ │
│  │               │     │                                                           │ │
│  │ FARM_MANAGER  │────►│ FARM_READ, FARM_WRITE, SEASON_CREATE, SEASON_UPDATE      │ │
│  │               │     │ PRODUCT_CREATE, ORDER_READ, ORDER_ACCEPT                  │ │
│  │               │     │ SHIPMENT_READ, REPORT_CREATE, SUBSCRIPTION_MANAGE         │ │
│  │               │     │                                                           │ │
│  │ RETAILER      │────►│ PRODUCT_READ, ORDER_CREATE, ORDER_CANCEL                  │ │
│  │               │     │ PAYMENT_CREATE, SHIPMENT_READ, REPORT_CREATE              │ │
│  │               │     │ QR_SCAN                                                  │ │
│  │               │     │                                                           │ │
│  │ SHIPPING_MGR  │────►│ ORDER_READ, SHIPMENT_CREATE, SHIPMENT_CANCEL              │ │
│  │               │     │ VEHICLE_MANAGE, DRIVER_MANAGE, REPORT_CREATE              │ │
│  │               │     │                                                           │ │
│  │ SHIP_DRIVER   │────►│ SHIPMENT_READ, SHIPMENT_UPDATE, QR_SCAN                   │ │
│  │               │     │ REPORT_CREATE                                             │ │
│  │               │     │                                                           │ │
│  │ GUEST         │────►│ PRODUCT_READ, QR_SCAN, ARTICLE_READ                       │ │
│  └───────────────┘     └───────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  Access Control Flow:                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │  1. Client sends JWT in Authorization: Bearer <token>                           │ │
│  │  2. JwtAuthenticationFilter extracts token, validates signature & expiry        │ │
│  │  3. CustomUserDetailsService.loadUserByUsername() loads User + Roles + Permissions│ │
│  │  4. Spring Security sets SecurityContext with GrantedAuthorities                │ │
│  │  5. @PreAuthorize("hasRole('SUPER_ADMIN')") or @PreAuthorize("hasAuthority(...)"│ │
│  │     on controller methods enforces access                                       │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.3. Module Admin (Quản trị hệ thống)

#### 2.3.1. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE ADMIN                                              │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                         AdminController                                          │ │
│  │  /api/admin                                                                      │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  POST   /              → @PreAuthorize("hasRole('SUPER_ADMIN')")                 │ │
│  │  GET    /              → @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN',...)")  │ │
│  │  GET    /{id}          → getAdminById(@PathVariable Long)                        │ │
│  │  PUT    /{id}          → @PreAuthorize("hasRole('SUPER_ADMIN')")                 │ │
│  │  DELETE /{id}          → @PreAuthorize("hasRole('SUPER_ADMIN')")                 │ │
│  │  GET    /roles         → getRoles()                                              │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                                │
│                                      ▼                                                │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                          AdminService                                            │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + createAdmin(AdminCreateRequest): AdminResponse                                │ │
│  │  + getAllAdmins(Pageable): Page<AdminResponse>                                   │ │
│  │  + getAdminById(Long): AdminResponse                                             │ │
│  │  + updateAdmin(Long, AdminUpdateRequest): AdminResponse                          │ │
│  │  + deleteAdmin(Long): void  // soft-delete → status=INACTIVE                     │ │
│  │  + getRoles(): List<Role>                                                        │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Business Rules:                                                                 │ │
│  │  - BR1: Only SUPER_ADMIN can create/delete admin accounts                       │ │
│  │  - BR2: Delete is soft-delete (status=INACTIVE)                                 │ │
│  │  - BR3: Cannot delete own account                                               │ │
│  │  - BR4: Email must be unique across all users                                   │ │
│  │  - BR5: Password must be hashed with BCrypt before storage                      │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                      │                                                │
│                    ┌─────────────────┼─────────────────┐                              │
│                    ▼                 ▼                   ▼                            │
│  ┌──────────────────────┐ ┌──────────────────┐ ┌──────────────────────────────────┐ │
│  │   UserRepository     │ │ RoleRepository   │ │    PasswordEncoder (BCrypt)      │ │
│  │──────────────────────│ │──────────────────│ │──────────────────────────────────│ │
│  │ + findByEmail(String)│ │ + findByName(Str)│ │ + encode(CharSequence): String   │ │
│  │ + existsByEmail(Str) │ │ + findAll()      │ │ + matches(CharSequence,String): │ │
│  │ + findByRoles_Name   │ │                  │ │   boolean                        │ │
│  │   (String): List     │ │                  │ │                                   │ │
│  └──────────────────────┘ └──────────────────┘ └──────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                         │
│  │   AdminCreateRequest    │     │   AdminUpdateRequest    │                         │
│  │─────────────────────────│     │─────────────────────────│                         │
│  │ - fullName: @NotBlank   │     │ - fullName: String      │                         │
│  │ - email: @Email         │     │ - phone: String         │                         │
│  │ - password: @Size(min=8)│     │ - status: UserStatus    │                         │
│  │ - phone: String         │     │ - roleIds: List<Long>   │                         │
│  │ - roleIds: List<Long>   │     │ - avatarUrl: String     │                         │
│  └─────────────────────────┘     └─────────────────────────┘                         │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                          AdminResponse                                           │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  - id: Long        - fullName: String    - email: String                        │ │
│  │  - phone: String   - status: UserStatus  - avatarUrl: String                    │ │
│  │  - roles: List<RoleDto>   - createdAt: LocalDateTime                            │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.4. Module Farm Management (Quản lý nông trại)

#### 2.4.1. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE FARM MANAGEMENT                                    │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                    FarmController (Planned)                                      │ │
│  │  /api/farms                                                                      │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  POST   /api/farms                → registerFarm(@Valid FarmRequest)             │ │
│  │  GET    /api/farms                → getMyFarms()                                 │ │
│  │  GET    /api/farms/{id}           → getFarmDetail(@PathVariable)                 │ │
│  │  PUT    /api/farms/{id}           → updateFarm(@PathVariable, @Valid)            │ │
│  │  POST   /api/farms/{id}/documents → uploadDocuments(@PathVariable, @RequestParam) │ │
│  │  GET    /api/farms/{id}/seasons   → getFarmSeasons(@PathVariable, Pageable)      │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │               FarmingSeasonController (Planned)                                  │ │
│  │  /api/farms/{farmId}/seasons                                                     │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  POST   /                        → createSeason(@Valid SeasonRequest)            │ │
│  │  GET    /                        → getSeasons(Pageable, filters)                 │ │
│  │  GET    /{seasonId}              → getSeasonDetail(@PathVariable)                │ │
│  │  POST   /{seasonId}/processes    → addProcess(@Valid ProcessRequest)             │ │
│  │  GET    /{seasonId}/processes    → getProcesses(@PathVariable)                   │ │
│  │  POST   /{seasonId}/export       → exportSeason(@Valid ExportRequest)            │ │
│  │  POST   /{seasonId}/sell         → pushToTradingFloor(@Valid SellRequest)        │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                         FarmService (Planned)                                    │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + registerFarm(FarmRequest, Long userId): FarmResponse                          │ │
│  │  + getMyFarms(Long userId): List<FarmResponse>                                   │ │
│  │  + getFarmDetail(Long farmId): FarmDetailResponse                                │ │
│  │  + updateFarm(Long farmId, FarmRequest): FarmResponse                            │ │
│  │  + approveFarm(Long farmId): void  // Admin only                                │ │
│  │  + rejectFarm(Long farmId, String reason): void  // Admin only                  │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Business Rules:                                                                 │ │
│  │  - BR1: Farm must be APPROVED before full features available                     │ │
│  │  - BR2: Active subscription required for creating seasons                        │ │
│  │  - BR3: Business license upload is mandatory                                    │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                     FarmingSeasonService (Planned)                               │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + createSeason(Long farmId, SeasonRequest): SeasonResponse                      │ │
│  │  + getSeasons(Long farmId, Pageable, filters): Page<SeasonResponse>              │ │
│  │  + getSeasonDetail(Long seasonId): SeasonDetailResponse                          │ │
│  │  + addProcess(Long seasonId, ProcessRequest): ProcessResponse                    │ │
│  │  + exportSeason(Long seasonId, ExportRequest): ExportResponse                    │ │
│  │  + pushToTradingFloor(Long seasonId, SellRequest): ProductResponse               │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Blockchain Integration:                                                         │ │
│  │  - createSeason() → BlockchainService.writeSeason() → VeChainThor                │ │
│  │  - addProcess()   → BlockchainService.writeProcess() → VeChainThor               │ │
│  │  - exportSeason() → BlockchainService.writeExport() → VeChainThor + Generate QR  │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.5. Module Retailer (Nhà bán lẻ)

#### 2.5.1. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE RETAILER                                           │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                   RetailerController (Planned)                                   │ │
│  │  /api/retailer                                                                   │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  PUT    /api/retailer/profile       → updateProfile(@Valid RetailerProfile)      │ │
│  │  POST   /api/retailer/documents     → uploadBusinessLicense(@RequestParam File)  │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  /api/marketplace (Trading Floor)                                                │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  GET    /api/marketplace/products   → searchProducts(Pageable, SearchFilter)     │ │
│  │  GET    /api/marketplace/products/{id}→ getProductDetail(@PathVariable)          │ │
│  │  POST   /api/marketplace/orders     → createOrder(@Valid OrderRequest)           │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  /api/qr                                                                         │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  GET    /api/qr/scan                → scanQR(@RequestParam String qrData)        │ │
│  │  GET    /api/qr/trace/{hash}        → traceProduct(@PathVariable String hash)    │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                    MarketplaceService (Planned)                                  │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + searchProducts(SearchFilter, Pageable): Page<ProductResponse>                 │ │
│  │  + getProductDetail(Long productId): ProductDetailResponse                       │ │
│  │  + createOrder(Long retailerId, OrderRequest): OrderResponse                     │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Search Filters:                                                                 │ │
│  │  - keyword: String (product name, farm name)                                    │ │
│  │  - categoryId: Long                                                              │ │
│  │  - region: String (NORTH, CENTRAL, SOUTH)                                        │ │
│  │  - certifications: List<String> (VietGAP, GlobalGAP, Organic)                    │ │
│  │  - minPrice: BigDecimal, maxPrice: BigDecimal                                    │ │
│  │  - sortBy: PRICE_ASC, PRICE_DESC, NEWEST, RATING                                 │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.6. Module Order (Đơn hàng & Giao dịch)

#### 2.6.1. Order State Machine

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              ORDER STATE MACHINE                                       │
│                                                                                       │
│                              ┌──────────┐                                            │
│                              │  PENDING │  ← Retailer tạo yêu cầu mua                │
│                              └────┬─────┘                                            │
│                                   │                                                   │
│                    ┌──────────────┼──────────────┐                                   │
│                    │ Farm Accept  │              │ Farm Reject /                     │
│                    ▼              │              │ Retailer Cancel                    │
│              ┌──────────┐        │              ▼                                   │
│              │ ACCEPTED │        │        ┌──────────┐                              │
│              └────┬─────┘        │        │ REJECTED │ / CANCELLED                   │
│                   │              │        └──────────┘                              │
│                   │ Retailer     │                                                    │
│                   │ Deposit (30%)│                                                    │
│                   ▼              │                                                    │
│              ┌──────────┐        │                                                    │
│              │   PAID   │        │                                                    │
│              └────┬─────┘        │                                                    │
│                   │              │                                                    │
│                   │ Shipping     │                                                    │
│                   │ Mgr assigns  │                                                    │
│                   ▼              │                                                    │
│              ┌──────────┐        │                                                    │
│              │ SHIPPING │        │                                                    │
│              └────┬─────┘        │                                                    │
│                   │              │                                                    │
│                   │ Driver       │                                                    │
│                   │ delivers     │                                                    │
│                   ▼              │                                                    │
│              ┌──────────┐        │                                                    │
│              │DELIVERED │        │                                                    │
│              └────┬─────┘        │                                                    │
│                   │              │                                                    │
│                   │ Retailer     │                                                    │
│                   │ confirms     │                                                    │
│                   ▼              │                                                    │
│              ┌──────────┐        │                                                    │
│              │COMPLETED │        │                                                    │
│              └──────────┘        │                                                    │
│                                                                                       │
│  Transition Rules:                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ From       → To         │ Trigger                │ Actor         │ Condition     │ │
│  │─────────────────────────│────────────────────────│───────────────│───────────────│ │
│  │ PENDING    → ACCEPTED   │ Farm accepts           │ Farm Manager  │ Product avail │ │
│  │ PENDING    → REJECTED   │ Farm rejects           │ Farm Manager  │ Reason req.   │ │
│  │ PENDING    → CANCELLED  │ Retailer cancels       │ Retailer      │ Reason req.   │ │
│  │ ACCEPTED   → PAID       │ Retailer pays deposit  │ Retailer      │ Within 24h    │ │
│  │ ACCEPTED   → CANCELLED  │ Timeout (24h no dep.)  │ System        │ Auto-cancel   │ │
│  │ PAID       → SHIPPING   │ SM creates shipment    │ Ship. Manager │               │ │
│  │ SHIPPING   → DELIVERED  │ Driver delivers        │ Ship. Driver  │               │ │
│  │ DELIVERED  → COMPLETED  │ Retailer confirms      │ Retailer      │ Within 48h    │ │
│  │ DELIVERED  → COMPLETED  │ Timeout (48h no conf.) │ System        │ Auto-confirm  │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.7. Module Shipping (Quản lý vận chuyển)

#### 2.7.1. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE SHIPPING & DRIVER                                  │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                ShippingController (Planned)                                      │ │
│  │  /api/shipping                                                                   │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  GET    /api/shipping/orders         → getCompletedOrders()                       │ │
│  │  POST   /api/shipping/shipments      → createShipment(@Valid ShipmentReq)         │ │
│  │  GET    /api/shipping/shipments       → getShipments(Pageable, filters)           │ │
│  │  GET    /api/shipping/shipments/{id}  → getShipmentDetail(@PathVariable)          │ │
│  │  PUT    /api/shipping/shipments/{id}/cancel → cancelShipment(@PathVariable)       │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  /api/shipping/vehicles                                                           │ │
│  │  GET    /      → getVehicles()                                                    │ │
│  │  POST   /      → createVehicle(@Valid VehicleReq)                                 │ │
│  │  PUT    /{id}  → updateVehicle(@PathVariable, @Valid VehicleReq)                  │ │
│  │  DELETE /{id}  → deleteVehicle(@PathVariable)                                     │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  /api/shipping/drivers                                                            │ │
│  │  GET    /      → getDrivers()                                                     │ │
│  │  POST   /      → createDriver(@Valid DriverReq)                                   │ │
│  │  PUT    /{id}  → updateDriver(@PathVariable, @Valid DriverReq)                    │ │
│  │  DELETE /{id}  → deleteDriver(@PathVariable)                                      │ │
│  │  PUT    /{id}/assign → assignVehicle(@PathVariable, @RequestParam vehicleId)      │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │            DriverMobileController (Planned) — for mobile app                     │ │
│  │  /api/driver                                                                      │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  GET    /api/driver/shipments    → getMyShipments(Pageable, status filter)        │ │
│  │  GET    /api/driver/shipments/{id}→ getShipmentDetail(@PathVariable)              │ │
│  │  POST   /api/driver/shipments/{id}/tracking → addTracking(@Valid TrackingReq)     │ │
│  │  POST   /api/driver/shipments/{id}/pickup   → confirmPickup(@Valid PickupReq)     │ │
│  │  POST   /api/driver/shipments/{id}/deliver  → confirmDelivery(@Valid DelivReq)     │ │
│  │  POST   /api/driver/reports                → sendReport(@Valid ReportReq)         │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                         ShipmentService (Planned)                                │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + createShipment(ShipmentRequest): ShipmentResponse                              │ │
│  │  + getShipments(Pageable, filters): Page<ShipmentResponse>                        │ │
│  │  + getShipmentDetail(Long): ShipmentDetailResponse                                │ │
│  │  + cancelShipment(Long, String reason): void                                      │ │
│  │  + assignDriver(Long shipmentId, Long driverId): void                             │ │
│  │  + addTracking(Long shipmentId, TrackingRequest): void                            │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Business Rules:                                                                 │ │
│  │  - BR1: Can only cancel shipment before driver starts (status=PICKING_UP)         │ │
│  │  - BR2: Driver must be in IDLE status to be assigned                              │ │
│  │  - BR3: Vehicle must be AVAILABLE to be assigned                                  │ │
│  │  - BR4: GPS tracking updates must be within reasonable distance                    │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.8. Module Payment (Thanh toán)

#### 2.8.1. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE PAYMENT (SEPAY INTEGRATION)                        │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                         SepayService                                             │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  - sepayConfig: SepayConfig                                                      │ │
│  │  - paymentRepository: PaymentRepository                                          │ │
│  │  - subscriptionRepository: SubscriptionRepository                                │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + createDeposit(CreateDepositRequest): DepositResponse                          │ │
│  │  + handleWebhook(SepayWebhookRequest): void                                      │ │
│  │  + checkPaymentStatus(String txRef): PaymentStatusResponse                       │ │
│  │  + verifySignature(SepayWebhookRequest): boolean                                 │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                   SepayWebhookController                                         │ │
│  │  /api/webhooks/sepay                                                             │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  POST   /          → handleWebhook(@RequestBody SepayWebhookRequest)             │ │
│  │                       // No auth — validated by HMAC signature                   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  Payment Flow (Sepay Redirect):                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                                 │ │
│  │  1. Client POST /api/payments/deposit                                            │ │
│  │       → SepayService.createDeposit()                                            │ │
│  │       → Generates Sepay payment URL                                             │ │
│  │       → Returns { paymentUrl, txRef }                                           │ │
│  │                                                                                 │ │
│  │  2. Client redirects user to paymentUrl (Sepay hosted page)                      │ │
│  │                                                                                 │ │
│  │  3. User completes payment on Sepay                                              │ │
│  │                                                                                 │ │
│  │  4. Sepay sends callback to POST /api/webhooks/sepay                             │ │
│  │       → SepayService.handleWebhook()                                            │ │
│  │       → Verify HMAC signature                                                   │ │
│  │       → Update Payment.status = COMPLETED                                       │ │
│  │       → Activate subscription / confirm order                                   │ │
│  │       → Send notification to user                                               │ │
│  │                                                                                 │ │
│  │  5. Client polls GET /api/payments/{txRef}/status                                │ │
│  │       → SepayService.checkPaymentStatus()                                       │ │
│  │       → Returns { status, amount, updatedAt }                                   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.9. Module Blockchain (Tích hợp VeChainThor)

#### 2.9.1. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE BLOCKCHAIN (VeChainThor)                           │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                    BlockchainService (Planned)                                   │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  - veChainConnection: ThorConnection                                             │ │
│  │  - seasonContract: FarmingSeasonContract                                         │ │
│  │  - processContract: FarmingProcessContract                                       │ │
│  │  - exportContract: ExportContract                                                │ │
│  │  - traceContract: TraceabilityContract                                           │ │
│  │  - txRepository: BlockchainTransactionRepository                                 │ │
│  │  - redisTemplate: RedisTemplate                                                  │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + writeSeason(Long seasonId, SeasonData): String // returns txHash              │ │
│  │  + writeProcess(Long processId, ProcessData): String                             │ │
│  │  + writeExport(Long exportId, ExportData): String                                │ │
│  │  + readTrace(String qrHash): TraceData                                           │ │
│  │  + verifyQR(String qrHash): boolean                                              │ │
│  │  + deployContract(ContractDeployRequest): String // returns contract address      │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  TX Lifecycle:                                                                   │ │
│  │  1. Build TX data (ABI encode params)                                           │ │
│  │  2. Get nonce from Redis (atomic INCR)                                           │ │
│  │  3. Sign TX with system private key                                              │ │
│  │  4. Submit to VeChainThor node                                                    │ │
│  │  5. Wait for confirmation (12 blocks)                                            │ │
│  │  6. Save txHash + status to MySQL                                                │ │
│  │  7. On failure: retry 3 times → queue for async retry                            │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                  Smart Contract Wrappers (Planned)                               │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │                                                                                 │ │
│  │  ┌─────────────────────────┐     ┌─────────────────────────┐                    │ │
│  │  │ FarmingSeasonContract   │     │ FarmingProcessContract  │                    │ │
│  │  │─────────────────────────│     │─────────────────────────│                    │ │
│  │  │ + createSeason(         │     │ + addProcess(           │                    │ │
│  │  │   bytes32 farmId,       │     │   bytes32 seasonId,     │                    │ │
│  │  │   bytes32 seasonId,     │     │   bytes32 processId,     │                    │ │
│  │  │   string seasonName,    │     │   string processType,   │                    │ │
│  │  │   string productType,   │     │   uint256 execDate,     │                    │ │
│  │  │   string variety,       │     │   bytes32 materialsHash,│                    │ │
│  │  │   uint256 area,         │     │   bytes32 imagesHash    │                    │ │
│  │  │   uint256 startDate     │     │  ): uint256             │                    │ │
│  │  │  ): uint256             │     │                         │                    │ │
│  │  └─────────────────────────┘     └─────────────────────────┘                    │ │
│  │                                                                                 │ │
│  │  ┌─────────────────────────┐     ┌─────────────────────────┐                    │ │
│  │  │ ExportContract          │     │ TraceabilityContract    │                    │ │
│  │  │─────────────────────────│     │─────────────────────────│                    │ │
│  │  │ + recordExport(         │     │ + getTrace(bytes32)     │                    │ │
│  │  │   bytes32 seasonId,     │     │   : TraceData           │                    │ │
│  │  │   bytes32 exportId,     │     │                         │                    │ │
│  │  │   uint256 quantity,     │     │ + verify(bytes32 qrHash)│                    │ │
│  │  │   bytes32 qrHash        │     │   : bool                │                    │ │
│  │  │  ): uint256             │     │                         │                    │ │
│  │  └─────────────────────────┘     └─────────────────────────┘                    │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                    Nonce Management (Redis-based)                                │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Key: bc:nonce:{accountAddress}                                                  │ │
│  │  Operation: Redis INCR (atomic increment)                                        │ │
│  │  Fallback: Query on-chain nonce if Redis fails                                   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.10. Module Notification (Thông báo)

#### 2.10.1. Class Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE NOTIFICATION                                       │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                  NotificationService (Planned)                                   │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  - notificationRepository: NotificationRepository                                │ │
│  │  - webSocketHandler: WebSocketHandler                                            │ │
│  │  - fcmService: FCMService                                                        │ │
│  │  - emailService: EmailService                                                    │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + sendNotification(Long userId, NotificationEvent): void                        │ │
│  │  + getNotifications(Long userId, Pageable): Page<NotificationResponse>           │ │
│  │  + markAsRead(Long notificationId): void                                         │ │
│  │  + markAllAsRead(Long userId): void                                              │ │
│  │  + getUnreadCount(Long userId): int                                              │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Channel Resolution:                                                             │ │
│  │  ┌───────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │ Event Type          │ In-App (WS)│ Push (FCM) │ Email (SMTP)              │ │ │
│  │  │─────────────────────│────────────│─────────────│──────────────────────────│ │ │
│  │  │ ORDER_CREATED       │     ✓      │             │      ✓ (Farm Manager)    │ │ │
│  │  │ ORDER_ACCEPTED      │     ✓      │      ✓      │      ✓ (Retailer)        │ │ │
│  │  │ DEPOSIT_PAID        │     ✓      │             │      ✓ (Farm Manager)    │ │ │
│  │  │ SHIPMENT_UPDATE     │     ✓      │      ✓      │                           │ │ │
│  │  │ DELIVERY_DONE       │     ✓      │             │      ✓ (Both)            │ │ │
│  │  │ FARM_APPROVED       │     ✓      │             │      ✓ (Farm Manager)    │ │ │
│  │  │ FARM_REJECTED       │     ✓      │             │      ✓ (Farm Manager)    │ │ │
│  │  │ IOT_ALERT           │     ✓      │      ✓      │                           │ │ │
│  │  │ REPORT_RECEIVED     │     ✓      │             │                           │ │ │
│  │  │ SERVICE_EXPIRING    │     ✓      │             │      ✓ (Farm Manager)    │ │ │
│  │  │ PAYMENT_SUCCESS     │     ✓      │             │      ✓                    │ │ │
│  │  └───────────────────────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.11. Module IoT (Cảm biến)

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE IoT                                                │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                     IoTService (Planned)                                         │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  - iotDataRepository: IoTDataRepository                                          │ │
│  │  - redisTemplate: RedisTemplate                                                  │ │
│  │  - notificationService: NotificationService                                      │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  + processSensorData(IoTSensorData): void                                        │ │
│  │  + getLatestReadings(Long farmId): IoTReadingResponse                            │ │
│  │  + getHistoricalData(Long farmId, DateRange): List<IoTReadingResponse>           │ │
│  │  + checkThresholds(IoTSensorData): List<Alert>                                   │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  Threshold Rules:                                                                │ │
│  │  ┌───────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │ Parameter   │ Min    │ Max    │ Unit  │ Alert Type                         │ │ │
│  │  │─────────────│────────│────────│───────│────────────────────────────────────│ │ │
│  │  │ Temperature │ 15     │ 40     │ °C    │ Below min / Above max              │ │ │
│  │  │ Humidity    │ 30     │ 90     │ %     │ Below min / Above max              │ │ │
│  │  │ pH          │ 5.5    │ 7.5    │ pH    │ Below min / Above max              │ │ │
│  │  └───────────────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                                 │ │
│  │  MQTT Topics:                                                                    │ │
│  │  bicap/iot/{farmId}/temperature    → Temperature readings                        │ │
│  │  bicap/iot/{farmId}/humidity       → Humidity readings                           │ │
│  │  bicap/iot/{farmId}/ph             → pH readings                                 │ │
│  │  bicap/iot/{farmId}/all            → Combined JSON: {temp, humidity, ph, ts}     │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 2.12. Module Content & Report

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              MODULE CONTENT & REPORT                                   │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                  ArticleController (Planned)                                     │ │
│  │  /api/public/articles                                                             │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  GET    /              → getArticles(Pageable, category) // Public, no auth      │ │
│  │  GET    /{id}          → getArticleDetail(@PathVariable) // Public               │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  /api/admin/articles  (Admin CRUD)                                               │ │
│  │  POST   /              → createArticle(@Valid)                                   │ │
│  │  PUT    /{id}          → updateArticle(@PathVariable, @Valid)                    │ │
│  │  DELETE /{id}          → deleteArticle(@PathVariable)                            │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │                  ReportController (Planned)                                      │ │
│  │  /api/reports                                                                     │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  POST   /              → createReport(@Valid ReportRequest) // Any auth user     │ │
│  │  GET    /              → getMyReports(Pageable)                                  │ │
│  │  GET    /{id}          → getReportDetail(@PathVariable)                          │ │
│  │─────────────────────────────────────────────────────────────────────────────────│ │
│  │  /api/admin/reports  (Admin management)                                          │ │
│  │  GET    /              → getAllReports(Pageable, status filter)                  │ │
│  │  PUT    /{id}/respond  → respondToReport(@PathVariable, @RequestBody)            │ │
│  │  PUT    /{id}/status   → updateReportStatus(@PathVariable, @RequestParam)        │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  Report Status Flow:                                                                  │
│  OPEN → INVESTIGATING → RESOLVED → CLOSED                                             │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Thiết kế chi tiết Cơ sở dữ liệu

### 3.1. Entity-Relationship Diagram (ERD) chi tiết

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              COMPLETE ENTITY-RELATIONSHIP DIAGRAM (CROW'S FOOT)                      │
│                                                                                                      │
│  ┌──────────────┐       ┌──────────────────┐       ┌──────────────────┐                             │
│  │ permissions  │◄───M:N───│ role_permissions │───M:N───►│      roles       │                             │
│  └──────────────┘       └──────────────────┘       └────────┬─────────┘                             │
│                                                             │ M:N                                    │
│                                                    ┌────────┴─────────┐                             │
│                                                    │   user_roles     │                             │
│                                                    └────────┬─────────┘                             │
│                                                             │ M:N                                    │
│  ┌──────────────────────┐                          ┌────────┴──────────┐                            │
│  │ service_packages     │                          │      users        │                            │
│  └──────────┬───────────┘                          └───┬──────┬────┬───┘                            │
│             │ 1:N                                       │      │    │                                │
│             ▼                                    ┌──────┘      │    └──────────────┐                │
│  ┌──────────────────────┐                        │ 1:N         │ 1:N              │ 1:1            │
│  │    subscriptions     │                        ▼             ▼                  ▼                │
│  └──────────┬───────────┘              ┌──────────────┐ ┌───────────┐    ┌──────────────┐          │
│             │ 1:N                      │    farms     │ │  orders   │    │   drivers    │          │
│             ▼                          └───┬────┬─────┘ └─────┬─────┘    └──────┬───────┘          │
│  ┌──────────────────────┐                  │    │ 1:N         │ 1:1             │ 1:1              │
│  │  farming_seasons     │◄──────1:N────────┘    │             ▼                 │                  │
│  └──┬──────┬──────┬─────┘                 ┌─────┴──────┐ ┌───────────┐   ┌─────┴──────┐          │
│     │ 1:N  │ 1:N  │ 1:N                   │  farm_cert │ │ shipments │   │  vehicles  │          │
│     ▼      ▼      ▼                       └────────────┘ └─────┬─────┘   └────────────┘          │
│  ┌────┐ ┌──────┐ ┌──────────┐                                  │ 1:N                              │
│  │proc│ │qrCodes│ │ products │                                  ▼                                  │
│  │ess │ └──────┘ └────┬─────┘                         ┌──────────────────┐                         │
│  └────┘               │ 1:N                           │shipment_tracking │                         │
│                       ▼                               └──────────────────┘                         │
│               ┌───────────┐                                                                         │
│               │  orders   │ (via product)                                                           │
│               └─────┬─────┘                                                                         │
│                     │ 1:N                                                                           │
│                     ▼                                                                               │
│               ┌───────────┐     ┌──────────────────────────┐                                       │
│               │ payments  │     │ blockchain_transactions  │                                       │
│               └───────────┘     └──────────────────────────┘                                       │
│                                                                                                      │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐                                         │
│  │ notifications│     │   reports    │     │   iot_data   │                                         │
│  └──────┬───────┘     └──────┬───────┘     └──────┬───────┘                                         │
│         │ FK                 │ FK                 │ FK                                               │
│         ▼                    ▼                    ▼                                                 │
│     ┌──────────┐        ┌──────────┐        ┌──────────┐                                           │
│     │  users   │        │  users   │        │  farms   │                                           │
│     └──────────┘        └──────────┘        └──────────┘                                           │
└─────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2. Mô tả chi tiết từng bảng

#### Bảng 1: `permissions`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID permission |
| `code` | VARCHAR(100) | NOT NULL, UNIQUE | Mã permission (vd: ADMIN_CREATE) |
| `description` | VARCHAR(255) | NULL | Mô tả quyền |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | Ngày cập nhật |

#### Bảng 2: `roles`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID role |
| `name` | VARCHAR(50) | NOT NULL, UNIQUE | Tên role: SUPER_ADMIN, ADMIN, MODERATOR, FARM_MANAGER, RETAILER, SHIPPING_MGR, SHIP_DRIVER, GUEST |
| `description` | VARCHAR(255) | NULL | Mô tả vai trò |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | Ngày cập nhật |

#### Bảng 3: `role_permissions`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `role_id` | BIGINT | PK, FK → roles(id) ON DELETE CASCADE | ID role |
| `permission_id` | BIGINT | PK, FK → permissions(id) ON DELETE CASCADE | ID permission |

#### Bảng 4: `users`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID user |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | Email đăng nhập |
| `password` | VARCHAR(128) | NOT NULL | Mật khẩu (BCrypt hash) |
| `full_name` | VARCHAR(255) | NOT NULL | Họ và tên |
| `phone` | VARCHAR(15) | NULL | Số điện thoại |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, INACTIVE, SUSPENDED |
| `avatar_url` | VARCHAR(500) | NULL | URL ảnh đại diện |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP ON UPDATE | Ngày cập nhật |

#### Bảng 5: `user_roles`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `user_id` | BIGINT | PK, FK → users(id) ON DELETE CASCADE | ID user |
| `role_id` | BIGINT | PK, FK → roles(id) ON DELETE CASCADE | ID role |

#### Bảng 6: `service_packages`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID gói dịch vụ |
| `name` | VARCHAR(100) | NOT NULL | Tên gói (vd: Basic, Premium, Enterprise) |
| `description` | TEXT | NULL | Mô tả chi tiết |
| `price` | DECIMAL(12,2) | NOT NULL | Giá gói (VND) |
| `duration_days` | INT | NOT NULL | Thời hạn (ngày) |
| `features` | JSON | NULL | Danh sách tính năng (JSON array) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, INACTIVE |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 7: `vehicles`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID phương tiện |
| `license_plate` | VARCHAR(20) | NOT NULL, UNIQUE | Biển số xe |
| `type` | VARCHAR(50) | NOT NULL | Loại xe (Truck, Van, Motorcycle) |
| `capacity` | DOUBLE | NOT NULL | Tải trọng (tấn) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'AVAILABLE' | AVAILABLE, IN_USE, MAINTENANCE |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 8: `drivers`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID tài xế |
| `user_id` | BIGINT | NOT NULL, UNIQUE, FK → users(id) | ID user (role = SHIP_DRIVER) |
| `citizen_id` | VARCHAR(20) | NOT NULL, UNIQUE | Số CCCD |
| `license_number` | VARCHAR(30) | NOT NULL, UNIQUE | Số bằng lái |
| `vehicle_id` | BIGINT | NULL, FK → vehicles(id) ON DELETE SET NULL | ID phương tiện được phân công |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'IDLE' | IDLE, ON_TRIP, OFFLINE |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 9: `farms`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID nông trại |
| `user_id` | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE | ID chủ trang trại |
| `name` | VARCHAR(255) | NOT NULL | Tên nông trại |
| `address` | VARCHAR(500) | NOT NULL | Địa chỉ |
| `area` | DOUBLE | NOT NULL | Diện tích (ha) |
| `gps_lat` | DOUBLE | NULL | Tọa độ GPS vĩ độ |
| `gps_lng` | DOUBLE | NULL | Tọa độ GPS kinh độ |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING, APPROVED, REJECTED, SUSPENDED |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 10: `farm_certifications`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID chứng nhận |
| `farm_id` | BIGINT | NOT NULL, FK → farms(id) ON DELETE CASCADE | ID nông trại |
| `type` | VARCHAR(100) | NOT NULL | Loại chứng nhận (VietGAP, GlobalGAP, Organic...) |
| `file_url` | VARCHAR(500) | NOT NULL | URL file chứng nhận |
| `expiry_date` | DATE | NOT NULL | Ngày hết hạn |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 11: `subscriptions`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID đăng ký |
| `farm_id` | BIGINT | NOT NULL, FK → farms(id) ON DELETE CASCADE | ID nông trại |
| `package_id` | BIGINT | NOT NULL, FK → service_packages(id) | ID gói dịch vụ |
| `start_date` | DATE | NOT NULL | Ngày bắt đầu |
| `end_date` | DATE | NOT NULL | Ngày hết hạn |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, EXPIRED, CANCELLED |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 12: `farming_seasons`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID mùa vụ |
| `farm_id` | BIGINT | NOT NULL, FK → farms(id) ON DELETE CASCADE | ID nông trại |
| `name` | VARCHAR(255) | NOT NULL | Tên mùa vụ |
| `product_type` | VARCHAR(100) | NOT NULL | Loại sản phẩm |
| `variety` | VARCHAR(100) | NOT NULL | Giống cây/vật nuôi |
| `area` | DOUBLE | NOT NULL | Diện tích canh tác (ha) |
| `start_date` | DATE | NOT NULL | Ngày bắt đầu |
| `end_date` | DATE | NULL | Ngày kết thúc |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'IN_PROGRESS' | IN_PROGRESS, HARVESTED, CANCELLED |
| `tx_hash` | VARCHAR(66) | NULL | VeChainThor transaction hash |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 13: `farming_processes`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID quy trình |
| `season_id` | BIGINT | NOT NULL, FK → farming_seasons(id) ON DELETE CASCADE | ID mùa vụ |
| `process_type` | VARCHAR(100) | NOT NULL | SOIL_PREP, SEEDING, FERTILIZATION, PEST_CONTROL, HARVESTING |
| `execution_date` | DATE | NOT NULL | Ngày thực hiện |
| `materials` | JSON | NULL | Vật tư sử dụng (JSON) |
| `images` | JSON | NULL | Danh sách URL ảnh minh chứng |
| `notes` | TEXT | NULL | Ghi chú |
| `tx_hash` | VARCHAR(66) | NULL | VeChainThor transaction hash |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 14: `qrcodes`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID QR Code |
| `trace_url` | VARCHAR(500) | NOT NULL | URL truy xuất nguồn gốc |
| `qr_image` | VARCHAR(500) | NOT NULL | URL ảnh QR Code |
| `season_id` | BIGINT | NOT NULL, FK → farming_seasons(id) ON DELETE CASCADE | ID mùa vụ |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 15: `products`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID sản phẩm |
| `season_id` | BIGINT | NOT NULL, FK → farming_seasons(id) ON DELETE CASCADE | ID mùa vụ |
| `category_id` | BIGINT | NOT NULL | ID danh mục |
| `name` | VARCHAR(255) | NOT NULL | Tên sản phẩm |
| `description` | TEXT | NULL | Mô tả |
| `price` | DECIMAL(12,2) | NOT NULL | Đơn giá (VND) |
| `quantity` | DOUBLE | NOT NULL | Số lượng |
| `qr_code_id` | BIGINT | NULL, FK → qrcodes(id) ON DELETE SET NULL | ID QR Code |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'AVAILABLE' | AVAILABLE, OUT_OF_STOCK, INACTIVE |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 16: `orders`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID đơn hàng |
| `product_id` | BIGINT | NOT NULL, FK → products(id) | ID sản phẩm |
| `retailer_id` | BIGINT | NOT NULL, FK → users(id) | ID nhà bán lẻ |
| `quantity` | DOUBLE | NOT NULL | Số lượng đặt mua |
| `price` | DECIMAL(12,2) | NOT NULL | Đơn giá tại thời điểm đặt (snapshot) |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | Xem Order State Machine |
| `delivery_addr` | VARCHAR(500) | NOT NULL | Địa chỉ giao hàng |
| `deposit_rate` | DOUBLE | NOT NULL, DEFAULT 0.3 | Tỷ lệ đặt cọc (30%) |
| `reject_reason` | VARCHAR(1000) | NULL | Lý do từ chối khi Farm Manager từ chối (BICAP-20 / SRS-FM-014) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 17: `payments`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID thanh toán |
| `order_id` | BIGINT | NOT NULL, FK → orders(id) ON DELETE CASCADE | ID đơn hàng |
| `amount` | DECIMAL(12,2) | NOT NULL | Số tiền |
| `method` | VARCHAR(50) | NOT NULL, DEFAULT 'VNPAY' | Phương thức: VNPAY, STRIPE, METAMASK |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING, COMPLETED, FAILED, REFUNDED |
| `tx_ref` | VARCHAR(100) | NULL | Mã tham chiếu cổng thanh toán |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 18: `shipments`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID lô vận chuyển |
| `order_id` | BIGINT | NOT NULL, UNIQUE, FK → orders(id) ON DELETE CASCADE | ID đơn hàng |
| `driver_id` | BIGINT | NULL, FK → drivers(id) ON DELETE SET NULL | ID tài xế |
| `vehicle_id` | BIGINT | NULL, FK → vehicles(id) ON DELETE SET NULL | ID phương tiện |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PICKING_UP' | PICKING_UP, IN_TRANSIT, DELIVERED, RETURNED |
| `pickup_time` | DATETIME | NULL | Thời gian lấy hàng |
| `delivery_time` | DATETIME | NULL | Thời gian giao hàng |
| `route_summary` | VARCHAR(500) | NULL | Tóm tắt lộ trình |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 19: `shipment_tracking`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID tracking |
| `shipment_id` | BIGINT | NOT NULL, FK → shipments(id) ON DELETE CASCADE | ID lô vận chuyển |
| `status` | VARCHAR(50) | NOT NULL | Trạng thái tại thời điểm |
| `gps_lat` | DOUBLE | NOT NULL | Tọa độ GPS vĩ độ |
| `gps_lng` | DOUBLE | NOT NULL | Tọa độ GPS kinh độ |
| `images` | JSON | NULL | Ảnh chụp tại thời điểm |
| `notes` | TEXT | NULL | Ghi chú |
| `timestamp` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian cập nhật |

#### Bảng 20: `blockchain_transactions`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID bản ghi |
| `entity_type` | VARCHAR(50) | NOT NULL | SEASON, PROCESS, QR, EXPORT |
| `entity_id` | BIGINT | NOT NULL | ID của entity liên quan |
| `tx_hash` | VARCHAR(66) | NOT NULL, UNIQUE | Transaction hash trên VeChainThor |
| `contract_address` | VARCHAR(42) | NULL | Địa chỉ Smart Contract |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING, CONFIRMED, FAILED |
| `retry_count` | INT | NOT NULL, DEFAULT 0 | Số lần retry |
| `idempotency_key` | VARCHAR(100) | NOT NULL, UNIQUE | Key chống trùng lặp giao dịch |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 21: `notifications`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID thông báo |
| `user_id` | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE | ID người nhận |
| `type` | VARCHAR(50) | NOT NULL | INFO, SUCCESS, WARNING, ALARM |
| `title` | VARCHAR(255) | NOT NULL | Tiêu đề |
| `content` | TEXT | NOT NULL | Nội dung |
| `channel` | VARCHAR(20) | NOT NULL, DEFAULT 'IN_APP' | IN_APP, PUSH, EMAIL |
| `is_read` | BOOLEAN | NOT NULL, DEFAULT FALSE | Đã đọc |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 22: `reports`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID báo cáo |
| `user_id` | BIGINT | NOT NULL, FK → users(id) ON DELETE CASCADE | ID người gửi |
| `type` | VARCHAR(50) | NOT NULL | INCIDENT, SYSTEM, INQUIRY |
| `title` | VARCHAR(255) | NOT NULL | Tiêu đề |
| `content` | TEXT | NOT NULL | Nội dung |
| `attachments` | JSON | NULL | File đính kèm |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'OPEN' | OPEN, INVESTIGATING, RESOLVED, CLOSED |
| `admin_response` | TEXT | NULL | Phản hồi Admin |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |

#### Bảng 23: `iot_data`

| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|-----|-------------|-----------|-------|
| `id` | BIGINT | PK, AUTO_INCREMENT | ID bản ghi |
| `farm_id` | BIGINT | NOT NULL, FK → farms(id) ON DELETE CASCADE | ID nông trại |
| `temperature` | DOUBLE | NOT NULL | Nhiệt độ (°C) |
| `humidity` | DOUBLE | NOT NULL | Độ ẩm (%) |
| `ph` | DOUBLE | NOT NULL | Độ pH |
| `measured_at` | TIMESTAMP | NOT NULL | Thời điểm đo |

### 3.3. Chiến lược Indexing

```sql
-- ============================================================
-- INDEXING STRATEGY FOR BICAP DATABASE (MySQL 5.7.41)
-- ============================================================

-- AUTH & USER LOOKUP
-- email is UNIQUE → auto-indexed by MySQL
CREATE INDEX `idx_users_phone` ON `users` (`phone`);
CREATE INDEX `idx_users_status` ON `users` (`status`);

-- FARM FILTERING
CREATE INDEX `idx_farms_status` ON `farms` (`status`);
CREATE INDEX `idx_farms_user` ON `farms` (`user_id`);

-- SEASON SEARCH
CREATE INDEX `idx_seasons_farm_status` ON `farming_seasons` (`farm_id`, `status`);
CREATE INDEX `idx_seasons_product_type` ON `farming_seasons` (`product_type`);

-- PROCESS LOOKUP
CREATE INDEX `idx_processes_season` ON `farming_processes` (`season_id`);

-- PRODUCT MARKETPLACE
CREATE INDEX `idx_products_category_status` ON `products` (`category_id`, `status`);
CREATE INDEX `idx_products_price` ON `products` (`price`);
CREATE INDEX `idx_products_season` ON `products` (`season_id`);

-- ORDER HISTORY
CREATE INDEX `idx_orders_retailer_status` ON `orders` (`retailer_id`, `status`);
CREATE INDEX `idx_orders_product` ON `orders` (`product_id`);
CREATE INDEX `idx_orders_status` ON `orders` (`status`);

-- PAYMENT LOOKUP
CREATE INDEX `idx_payments_order` ON `payments` (`order_id`);
CREATE INDEX `idx_payments_txref` ON `payments` (`tx_ref`);

-- SHIPMENT MANAGEMENT
CREATE INDEX `idx_shipments_driver_status` ON `shipments` (`driver_id`, `status`);
CREATE INDEX `idx_shipments_status` ON `shipments` (`status`);

-- SHIPMENT TRACKING
CREATE INDEX `idx_tracking_shipment` ON `shipment_tracking` (`shipment_id`);
CREATE INDEX `idx_tracking_timestamp` ON `shipment_tracking` (`timestamp`);

-- NOTIFICATIONS
CREATE INDEX `idx_notif_user_read` ON `notifications` (`user_id`, `is_read`);
CREATE INDEX `idx_notif_user_created` ON `notifications` (`user_id`, `created_at`);

-- REPORTS
CREATE INDEX `idx_reports_user` ON `reports` (`user_id`);
CREATE INDEX `idx_reports_status` ON `reports` (`status`);

-- IoT TIME-SERIES
CREATE INDEX `idx_iot_farm_time` ON `iot_data` (`farm_id`, `measured_at`);

-- BLOCKCHAIN TX
CREATE INDEX `idx_bctx_entity` ON `blockchain_transactions` (`entity_type`, `entity_id`);
CREATE INDEX `idx_bctx_status` ON `blockchain_transactions` (`status`);

-- SUBSCRIPTIONS
CREATE INDEX `idx_subs_farm_status` ON `subscriptions` (`farm_id`, `status`);
CREATE INDEX `idx_subs_end_date` ON `subscriptions` (`end_date`);

-- DRIVER
CREATE INDEX `idx_drivers_status` ON `drivers` (`status`);
CREATE INDEX `idx_drivers_vehicle` ON `drivers` (`vehicle_id`);

-- VEHICLE
CREATE INDEX `idx_vehicles_status` ON `vehicles` (`status`);

-- FARM CERTIFICATIONS
CREATE INDEX `idx_cert_farm` ON `farm_certifications` (`farm_id`);

-- QR CODES
CREATE INDEX `idx_qr_season` ON `qrcodes` (`season_id`);
```

### 3.4. Redis Cache Schema

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              REDIS 8.6 CACHE SCHEMA                                    │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ Key Pattern                         │ Type    │ TTL      │ Purpose               │ │
│  │─────────────────────────────────────│─────────│──────────│───────────────────────│ │
│  │ session:{token}                     │ String  │ 15 min   │ JWT session cache     │ │
│  │ user:{id}                           │ String  │ 30 min   │ User profile          │ │
│  │ farm:{id}                           │ String  │ 1 hour   │ Farm details          │ │
│  │ product:list:{page}:{filterHash}    │ String  │ 5 min    │ Marketplace search    │ │
│  │ product:{id}                        │ String  │ 30 min   │ Product detail        │ │
│  │ notification:{userId}:unread        │ Integer │ 1 min    │ Unread badge count    │ │
│  │ iot:{farmId}:latest                 │ String  │ 5 min    │ Latest IoT dashboard  │ │
│  │ rate_limit:{ip}                     │ String  │ 1 min    │ Rate limit counter    │ │
│  │ rate_limit:{userId}                 │ String  │ 1 min    │ User rate limit       │ │
│  │ bc:tx:{hash}                        │ String  │ 24 hours │ BC TX result cache    │ │
│  │ bc:nonce:{accountAddress}           │ String  │ —        │ TX nonce counter      │ │
│  │ season:{id}:timeline                │ String  │ 15 min   │ Season timeline       │ │
│  │ shipment:{id}:tracking              │ List    │ 5 min    │ Latest tracking       │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                       │
│  Cache Invalidation Strategy:                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ 1. Write-through: Update MySQL → DEL cache key simultaneously                    │ │
│  │ 2. TTL-based expiry: All keys auto-expire per TTL                                │ │
│  │ 3. Event-driven: Order status change → publish event → DEL related cache keys    │ │
│  │ 4. Pattern-based: DEL product:list:* when new product added                       │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Thiết kế chi tiết Frontend Web Apps

### 4.1. Cấu trúc Component tổng quan

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND MONOREPO STRUCTURE                                    │
│                                                                                       │
│  bicap-frontend/                                                                      │
│  ├── packages/                                                                        │
│  │   ├── shared/                          # Shared components, hooks, utils           │
│  │   │   ├── components/                                                              │
│  │   │   │   ├── Layout/                  # AppLayout, Sidebar, Header, Footer        │
│  │   │   │   ├── Form/                    # FormInput, FormSelect, FormUpload         │
│  │   │   │   ├── DataDisplay/             # DataTable, StatCard, StatusBadge          │
│  │   │   │   ├── Feedback/                # Toast, Modal, ConfirmDialog, Loading     │
│  │   │   │   └── Auth/                    # LoginForm, RegisterForm, ProtectedRoute   │
│  │   │   ├── hooks/                                                                   │
│  │   │   │   ├── useAuth.ts               # Auth state, login/logout                  │
│  │   │   │   ├── useApi.ts                # Axios instance, interceptors              │
│  │   │   │   ├── usePagination.ts         # Pagination logic                          │
│  │   │   │   ├── useWebSocket.ts          # STOMP WebSocket client                    │
│  │   │   │   └── useNotification.ts       # Notification polling/badge                │
│  │   │   ├── utils/                                                                   │
│  │   │   │   ├── auth.ts                  # Token storage, JWT decode                 │
│  │   │   │   ├── format.ts               # Date, currency, number formatters         │
│  │   │   │   └── validators.ts           # Form validation rules                     │
│  │   │   └── types/                                                                   │
│  │   │       ├── api.ts                   # API response types                        │
│  │   │       ├── models.ts                # Domain model types                        │
│  │   │       └── enums.ts                 # Enum types                                │
│  │   │                                                                                │
│  │   ├── admin-web/                       # Admin Web App (port 3001)                 │
│  │   │   ├── pages/                                                                   │
│  │   │   │   ├── Dashboard/               # System overview & stats                   │
│  │   │   │   ├── AccountManagement/       # Admin CRUD (BICAP-1)                      │
│  │   │   │   ├── FarmApproval/            # Farm registration approval (BICAP-3)     │
│  │   │   │   ├── FarmManagement/          # Farm details management (BICAP-4)        │
│  │   │   │   ├── ProductMonitoring/       # Product monitoring (BICAP-5)              │
│  │   │   │   ├── SmartContractManagement/ # Smart contract deploy/manage (BICAP-6)   │
│  │   │   │   └── ReportManagement/        # Admin report center                       │
│  │   │   └── App.tsx                                                                  │
│  │   │                                                                                │
│  │   ├── farm-web/                        # Farm Management Web App (port 3002)       │
│  │   │   ├── pages/                                                                   │
│  │   │   │   ├── Dashboard/               # Farm overview                             │
│  │   │   │   ├── Profile/                 # Profile & business docs (BICAP-8,9)      │
│  │   │   │   ├── ServicePackages/         # Buy service packages (BICAP-10,11)       │
│  │   │   │   ├── Seasons/                 # Season list & detail (BICAP-12,13)       │
│  │   │   │   ├── SeasonCreate/            # Create season + blockchain (BICAP-14)    │
│  │   │   │   ├── SeasonProcess/           # Update processes + blockchain (BICAP-15) │
│  │   │   │   ├── Export/                  # Export + QR Code (BICAP-16,17)           │
│  │   │   │   ├── TradingFloor/            # Push to marketplace (BICAP-18,19)        │
│  │   │   │   ├── Orders/                  # Order processing (BICAP-20)              │
│  │   │   │   ├── Retailers/               # Retailer info (BICAP-21)                 │
│  │   │   │   ├── Shipping/                # Track shipments (BICAP-22,23)            │
│  │   │   │   └── Notifications/           # Notifications (BICAP-24,25,26)           │
│  │   │   └── App.tsx                                                                  │
│  │   │                                                                                │
│  │   ├── retailer-web/                    # Retailer Web App (port 3003)               │
│  │   │   ├── pages/                                                                   │
│  │   │   │   ├── Dashboard/               # Retailer dashboard                         │
│  │   │   │   ├── Profile/                 # Profile & business docs (BICAP-37,38)    │
│  │   │   │   ├── Marketplace/             # Product search & browse (BICAP-39,40)    │
│  │   │   │   ├── ProductDetail/           # Product detail + QR scan (BICAP-41)      │
│  │   │   │   ├── OrderCreate/             # Create purchase order (BICAP-42)         │
│  │   │   │   ├── Payments/                # Deposit payment (BICAP-43)               │
│  │   │   │   ├── Orders/                  # Order history (BICAP-45,46)              │
│  │   │   │   ├── Shipping/                # Track deliveries (BICAP-49,50)           │
│  │   │   │   └── Notifications/           # Notifications (BICAP-47,48)              │
│  │   │   └── App.tsx                                                                  │
│  │   │                                                                                │
│  │   └── shipping-web/                    # Shipping Management Web App (port 3004)    │
│  │       ├── pages/                                                                   │
│  │       │   ├── Dashboard/               # Shipping overview                         │
│  │       │   ├── Orders/                  # Completed orders (BICAP-54)               │
│  │       │   ├── Shipments/               # Create & manage shipments (BICAP-55,56)  │
│  │       │   ├── Tracking/                # Track shipments (BICAP-57)                │
│  │       │   ├── Vehicles/                # Vehicle CRUD (BICAP-58)                  │
│  │       │   ├── Drivers/                 # Driver CRUD (BICAP-59)                   │
│  │       │   └── Reports/                 # Reports & notifications (BICAP-60,61,62) │
│  │       └── App.tsx                                                                  │
│  │                                                                                    │
│  └── apps/                                                                             │
│      ├── driver-mobile/                   # Shipping Driver Mobile App (React Native)  │
│      │   ├── screens/                                                                 │
│      │   │   ├── LoginScreen.tsx           # Driver login                              │
│      │   │   ├── ShipmentListScreen.tsx    # My shipments (BICAP-63)                   │
│      │   │   ├── ShipmentDetailScreen.tsx  # Shipment detail                           │
│      │   │   ├── QRScanScreen.tsx          # QR scan at farm (BICAP-65)                │
│      │   │   ├── PickupConfirmScreen.tsx   # Confirm pickup (BICAP-66)                 │
│      │   │   ├── DeliveryConfirmScreen.tsx # Confirm delivery (BICAP-67)               │
│      │   │   ├── TrackingUpdateScreen.tsx  # Update status + GPS (BICAP-64)            │
│      │   │   └── ReportScreen.tsx          # Send report (BICAP-68)                    │
│      │   └── App.tsx                                                                   │
│      │                                                                                 │
│      └── guest-app/                       # Guest App (React Native / Next.js)         │
│          ├── screens/                                                                  │
│          │   ├── HomeScreen.tsx            # Home page with featured products           │
│          │   ├── ProductSearchScreen.tsx   # Search & filter (BICAP-70)                │
│          │   ├── ProductDetailScreen.tsx   # Product detail                            │
│          │   ├── QRScanScreen.tsx          # Scan QR for traceability                  │
│          │   ├── TraceResultScreen.tsx     # Full traceability display                 │
│          │   ├── ArticleListScreen.tsx     # Educational articles (BICAP-71)           │
│          │   └── ArticleDetailScreen.tsx   # Article detail                            │
│          └── App.tsx                                                                   │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2. Admin Web App — Component Tree

```
App (AdminWeb)
├── AuthProvider (Context)
│   ├── LoginPage
│   │   └── LoginForm
│   │       ├── FormInput (email)
│   │       ├── FormInput (password)
│   │       └── SubmitButton
│   │
│   └── ProtectedRoute (role: SUPER_ADMIN | ADMIN | MODERATOR)
│       └── AppLayout
│           ├── Sidebar
│           │   ├── NavItem: Dashboard
│           │   ├── NavItem: Account Management
│           │   ├── NavItem: Farm Approval
│           │   ├── NavItem: Farm Management
│           │   ├── NavItem: Product Monitoring
│           │   ├── NavItem: Smart Contracts
│           │   └── NavItem: Reports
│           ├── Header
│           │   ├── Breadcrumb
│           │   ├── NotificationBell
│           │   └── UserMenu (avatar, logout)
│           └── Content (React Router Outlet)
│               ├── DashboardPage
│               │   ├── StatCard (x4: Users, Farms, Products, Orders)
│               │   ├── RecentActivityList
│               │   └── PendingApprovalList
│               │
│               ├── AccountManagementPage
│               │   ├── SearchBar + FilterBar
│               │   ├── DataTable (admins)
│               │   │   └── columns: Name, Email, Role, Status, Actions
│               │   ├── CreateAdminModal
│               │   │   ├── FormInput (fullName, email, phone)
│               │   │   ├── FormSelect (roles)
│               │   │   └── FormInput (password)
│               │   └── EditAdminModal
│               │
│               ├── FarmApprovalPage
│               │   ├── Tabs: Pending | Approved | Rejected
│               │   ├── DataTable (farm registrations)
│               │   ├── FarmDetailModal
│               │   │   ├── DocumentViewer (business license, certs)
│               │   │   ├── FarmInfo (name, address, area, GPS)
│               │   │   └── ApprovalActions (Approve / Reject with reason)
│               │   └── ConfirmDialog
│               │
│               ├── FarmManagementPage
│               │   ├── SearchBar + StatusFilter
│               │   ├── DataTable (all farms)
│               │   └── FarmDetailPanel
│               │       ├── Certifications
│               │       ├── SeasonHistory
│               │       └── AdminNotes
│               │
│               ├── ProductMonitoringPage
│               │   ├── CategoryFilter + SearchBar
│               │   ├── DataTable (all products)
│               │   └── ProductDetailPanel
│               │
│               ├── SmartContractPage
│               │   ├── ContractList
│               │   │   └── ContractCard (name, address, version, status)
│               │   ├── DeployContractModal
│               │   │   ├── FormInput (contract name)
│               │   │   ├── FileUpload (bytecode)
│               │   │   ├── FileUpload (ABI)
│               │   │   └── FormSelect (environment: TESTNET | MAINNET)
│               │   └── ContractDetailPanel
│               │       ├── TransactionHistory
│               │       └── StatusBadge
│               │
│               └── ReportManagementPage
│                   ├── StatusFilter
│                   ├── DataTable (all reports)
│                   └── ReportDetailModal
│                       ├── ReportContent
│                       ├── AttachmentViewer
│                       └── AdminResponseForm
```

### 4.3. Farm Management Web App — Component Tree

```
App (FarmWeb)
├── AuthProvider
│   ├── LoginPage / RegisterPage
│   │   ├── LoginForm
│   │   └── RegisterForm
│   │       ├── FormInput (fullName, email, phone)
│   │       ├── PasswordStrengthMeter
│   │       └── FormInput (password, confirmPassword)
│   │
│   └── ProtectedRoute (role: FARM_MANAGER)
│       └── AppLayout
│           ├── Sidebar
│           │   ├── Dashboard
│           │   ├── Profile & Farm Info
│           │   ├── Service Packages
│           │   ├── Farming Seasons
│           │   ├── Trading Floor
│           │   ├── Orders
│           │   ├── Shipping
│           │   └── Notifications
│           ├── Header (NotificationBell, SubscriptionBadge)
│           └── Content
│               ├── DashboardPage
│               │   ├── FarmStatusCard
│               │   ├── ActiveSeasonSummary
│               │   ├── IoTReadingWidget (temp, humidity, pH)
│               │   ├── PendingOrdersWidget
│               │   └── ActiveShipmentsWidget
│               │
│               ├── ProfilePage
│               │   ├── PersonalInfoForm
│               │   ├── FarmInfoForm
│               │   │   ├── FormInput (name, address, area)
│               │   │   ├── GPSPicker (map)
│               │   │   └── FormSelect (product types)
│               │   ├── BusinessLicenseUpload (file upload + preview)
│               │   └── CertificationsManager (add/remove cert files)
│               │
│               ├── ServicePackagesPage
│               │   ├── CurrentSubscriptionCard
│               │   ├── PackageComparisonTable
│               │   │   └── PackageCard (name, price, features, CTA)
│               │   └── PaymentModal
│               │       ├── PaymentMethodSelect
│               │       ├── PaymentSummary
│               │       └── LoadingOverlay (redirecting to Sepay)
│               │
│               ├── SeasonsPage
│               │   ├── SeasonFilterTabs (All | In Progress | Harvested | Exported)
│               │   ├── SeasonGrid / SeasonList
│               │   │   └── SeasonCard (name, crop, status, progress, txHash)
│               │   └── FloatingActionButton (Create Season)
│               │
│               ├── SeasonCreatePage
│               │   ├── StepWizard (Stepper)
│               │   │   ├── Step1: BasicInfo (name, productType, variety)
│               │   │   ├── Step2: FarmDetails (area, startDate, endDate)
│               │   │   ├── Step3: PlannedProcesses (drag-drop steps)
│               │   │   └── Step4: Review (summary, submit, blockchain confirm)
│               │   └── BlockchainTxStatus (txHash, confirmations, VeChain link)
│               │
│               ├── SeasonDetailPage
│               │   ├── SeasonInfoHeader (status badge, dates, area)
│               │   ├── ProcessTimeline
│               │   │   └── TimelineItem (date, type, materials, images, txHash)
│               │   ├── IoTDataChart (temperature, humidity, pH charts)
│               │   ├── AddProcessModal
│               │   │   ├── FormSelect (processType)
│               │   │   ├── DatePicker (executionDate)
│               │   │   ├── MaterialsEditor (add rows: name, quantity, unit)
│               │   │   ├── ImageUploader (multiple, with preview)
│               │   │   └── FormTextArea (notes)
│               │   └── ExportSection
│               │       ├── ExportForm (quantity, date, warehouse)
│               │       └── QRCodeDisplay (image, download, print)
│               │
│               ├── TradingFloorPage
│               │   ├── MyProductsList
│               │   │   └── ProductCard (name, price, quantity, status, qrCode)
│               │   └── PushToMarketModal
│               │       ├── FormInput (productName, description)
│               │       ├── FormInput (price, quantity)
│               │       ├── ImageUploader (product images)
│               │       └── QRCodePreview
│               │
│               ├── OrdersPage
│               │   ├── OrderFilterTabs (Pending | Accepted | Paid | Completed)
│               │   └── OrderList
│               │       └── OrderCard
│               │           ├── RetailerInfo
│               │           ├── ProductInfo
│               │           └── ActionButtons (Accept / Reject / View Detail)
│               │
│               ├── ShippingPage
│               │   ├── ShipmentList
│               │   │   └── ShipmentCard (status, driver, vehicle, ETA)
│               │   └── ShipmentTrackingMap (Leaflet/Google Maps with route)
│               │
│               └── NotificationsPage
│                   ├── NotificationList
│                   │   └── NotificationItem (icon, title, time, read/unread)
│                   └── MarkAllReadButton
```

### 4.4. Retailer Web App — Component Tree

```
App (RetailerWeb)
├── ... Auth, Layout (same pattern) ...
│   └── Content
│       ├── MarketplacePage
│       │   ├── SearchBar (keyword)
│       │   ├── FilterPanel
│       │   │   ├── CategoryFilter (checkboxes)
│       │   │   ├── RegionFilter (dropdown)
│       │   │   ├── CertificationFilter (multi-select)
│       │   │   ├── PriceRangeSlider (min - max)
│       │   │   └── SortSelect (PRICE_ASC, PRICE_DESC, NEWEST)
│       │   ├── ProductGrid
│       │   │   └── ProductCard (image, name, farm, price, certifications, qrBadge)
│       │   └── Pagination
│       │
│       ├── ProductDetailPage
│       │   ├── ImageGallery
│       │   ├── ProductInfo (name, description, price, quantity)
│       │   ├── FarmInfoCard (name, address, certifications)
│       │   ├── QRScanButton → QRCodeScanner (camera or upload)
│       │   ├── TraceabilityPanel (expandable)
│       │   │   ├── SeasonTimeline
│       │   │   ├── ProcessHistory
│       │   │   ├── Certifications
│       │   │   └── BlockchainVerificationBadge
│       │   └── OrderForm
│       │       ├── QuantityInput
│       │       ├── PriceInput
│       │       ├── DeliveryDatePicker
│       │       ├── AddressInput
│       │       ├── NotesTextArea
│       │       └── SubmitOrderButton
│       │
│       ├── OrdersPage
│       │   ├── OrderFilterTabs
│       │   └── OrderList
│       │       └── OrderCard
│       │           ├── OrderStatusBadge
│       │           ├── ProductSummary
│       │           ├── PaymentStatus
│       │           └── ActionButtons (Pay Deposit / Cancel / View)
│       │
│       ├── PaymentPage
│       │   ├── OrderSummary
│       │   ├── DepositAmount (30% calculation display)
│       │   ├── PaymentMethodSelect
│       │   └── PayButton → Redirect to Sepay
│       │
│       ├── ShippingTrackingPage
│       │   ├── ShipmentCard
│       │   ├── TrackingTimeline
│       │   └── LiveMap
│       │
│       └── DeliveryConfirmPage
│           ├── DeliveryInfo
│           ├── ImageUploader (received goods photos)
│           ├── RatingStars (1-5)
│           ├── FeedbackTextArea
│           └── ConfirmButton
```

### 4.5. Shipping Management Web App — Component Tree

```
App (ShippingWeb)
├── ... Auth, Layout ...
│   └── Content
│       ├── CompletedOrdersPage
│       │   └── OrderList → OrderCard (info, product, addresses)
│       │
│       ├── ShipmentCreatePage
│       │   ├── OrderInfoCard
│       │   ├── RoutePlanner (pickup → delivery)
│       │   ├── DriverSelect (dropdown: IDLE drivers)
│       │   ├── VehicleSelect (dropdown: AVAILABLE vehicles)
│       │   ├── SchedulePicker (pickup time, delivery ETA)
│       │   └── CreateButton
│       │
│       ├── ShipmentsPage
│       │   ├── StatusFilter
│       │   └── ShipmentList → ShipmentCard
│       │       ├── StatusBadge
│       │       ├── DriverInfo
│       │       ├── VehicleInfo
│       │       └── TrackingPreview
│       │
│       ├── VehiclesPage
│       │   ├── VehicleTable (license plate, type, capacity, status)
│       │   └── VehicleFormModal (CRUD)
│       │
│       ├── DriversPage
│       │   ├── DriverTable (name, citizen ID, license, assigned vehicle, status)
│       │   └── DriverFormModal (CRUD + assign vehicle)
│       │
│       └── ReportsPage
│           ├── ReportList
│           └── ReportDetail (from drivers, resolve actions)
```

### 4.6. Shipping Driver Mobile App — Component Tree

```
App (DriverMobile - React Native)
├── AuthProvider
│   └── LoginScreen
│       └── LoginForm (email + password)
│
├── Navigation (Bottom Tabs)
│   ├── Tab 1: Shipments
│   │   ├── ShipmentListScreen
│   │   │   ├── StatusFilter (PICKING_UP | IN_TRANSIT | DELIVERED)
│   │   │   └── ShipmentCard (order info, addresses, status badge)
│   │   │
│   │   └── ShipmentDetailScreen
│   │       ├── OrderInfoSection
│   │       ├── RouteMap (Google Maps / MapBox)
│   │       ├── ActionButtons
│   │       │   ├── Scan QR at Farm (BICAP-65)
│   │       │   ├── Confirm Pickup (BICAP-66)
│   │       │   ├── Update Status (BICAP-64)
│   │       │   └── Confirm Delivery (BICAP-67)
│   │       └── TrackingHistoryTimeline
│   │
│   ├── Tab 2: QR Scanner
│   │   └── QRScanScreen
│   │       ├── CameraView (react-native-camera)
│   │       ├── ScanResultOverlay
│   │       │   ├── ProductInfo
│   │       │   ├── FarmInfo
│   │       │   └── BlockchainVerificationBadge
│   │       └── ConfirmPickupButton
│   │
│   ├── Tab 3: Profile
│   │   ├── DriverInfo (name, license, vehicle)
│   │   └── LogoutButton
│   │
│   └── Tab 4: Reports
│       ├── MyReportsList
│       └── CreateReportScreen
│           ├── FormSelect (report type)
│           ├── FormTextArea (description)
│           ├── ImageCapture (camera)
│           └── SubmitButton
│
└── NotificationHandler (push notification listener)
```

### 4.7. Guest App — Component Tree

```
App (GuestApp - Next.js / React Native Web)
├── PublicLayout
│   ├── Header (logo, search, navigation)
│   ├── Content
│   │   ├── HomePage
│   │   │   ├── HeroBanner
│   │   │   ├── FeaturedProducts (scrollable cards)
│   │   │   ├── EducationalArticlesPreview
│   │   │   └── StatsSection (farmers, products, transactions)
│   │   │
│   │   ├── ProductSearchPage
│   │   │   ├── SearchBar
│   │   │   ├── FilterPanel (same as marketplace)
│   │   │   └── ProductGrid → ProductCard
│   │   │
│   │   ├── ProductDetailPage
│   │   │   ├── ProductInfo
│   │   │   └── QRCodeScanner (to trace product origin)
│   │   │
│   │   ├── TraceResultPage
│   │   │   ├── ProductOriginInfo
│   │   │   ├── FarmInfo
│   │   │   ├── SeasonTimeline (visual journey)
│   │   │   ├── Certifications
│   │   │   ├── IoTDataSummary
│   │   │   └── BlockchainVerificationBadge "Verified on VeChainThor ✓"
│   │   │
│   │   ├── ArticleListPage
│   │   │   ├── CategoryFilter
│   │   │   └── ArticleGrid → ArticleCard
│   │   │
│   │   └── ArticleDetailPage
│   │       ├── ArticleContent
│   │       ├── RelatedArticles
│   │       └── ShareButtons
│   │
│   └── Footer (about, contact, links)
└── NotificationPrompt (opt-in to push notifications)
```

---

## 5. Thiết kế chi tiết Smart Contract

### 5.1. FarmingSeasonContract

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         FarmingSeasonContract — Solidity Design                       │
│──────────────────────────────────────────────────────────────────────────────────────│
│                                                                                      │
│  // SPDX-License-Identifier: MIT                                                     │
│  pragma solidity ^0.8.24;                                                            │
│                                                                                      │
│  import "@openzeppelin/contracts-upgradeable/access/AccessControlUpgradeable.sol";   │
│  import "@openzeppelin/contracts-upgradeable/security/ReentrancyGuardUpgradeable.sol";│
│  import "@openzeppelin/contracts-upgradeable/security/PausableUpgradeable.sol";      │
│  import "@openzeppelin/contracts-upgradeable/proxy/utils/UUPSUpgradeable.sol";       │
│                                                                                      │
│  contract FarmingSeasonContract is                                                   │
│      Initializable, AccessControlUpgradeable, ReentrancyGuardUpgradeable,            │
│      PausableUpgradeable, UUPSUpgradeable {                                          │
│                                                                                      │
│      // ── ROLES ──────────────────────────────────────────────────────────────      │
│      bytes32 public constant SYSTEM_WRITER_ROLE = keccak256("SYSTEM_WRITER_ROLE");   │
│      bytes32 public constant UPGRADER_ROLE = keccak256("UPGRADER_ROLE");             │
│                                                                                      │
│      // ── STRUCTS ────────────────────────────────────────────────────────────      │
│      struct SeasonData {                                                             │
│          bytes32 farmId;                                                             │
│          bytes32 seasonId;                                                           │
│          string  seasonName;                                                         │
│          string  productType;                                                        │
│          string  variety;                                                            │
│          uint256 area;          // in square meters (scaled by 100)                   │
│          uint256 startDate;     // Unix timestamp                                     │
│          uint256 endDate;       // Unix timestamp (0 if ongoing)                      │
│          uint8   status;        // 0=IN_PROGRESS, 1=HARVESTED, 2=CANCELLED           │
│          uint256 createdAt;     // Block timestamp                                     │
│      }                                                                               │
│                                                                                      │
│      // ── STORAGE ────────────────────────────────────────────────────────────      │
│      mapping(bytes32 => SeasonData) public seasons;                                  │
│      mapping(bytes32 => bytes32[]) public farmSeasons; // farmId → seasonId[]        │
│      bytes32[] public allSeasonIds;                                                  │
│                                                                                      │
│      // ── EVENTS ─────────────────────────────────────────────────────────────      │
│      event SeasonCreated(bytes32 indexed farmId, bytes32 indexed seasonId,           │
│                         string seasonName, uint256 startDate);                       │
│      event SeasonStatusUpdated(bytes32 indexed seasonId, uint8 oldStatus,            │
│                               uint8 newStatus);                                      │
│                                                                                      │
│      // ── FUNCTIONS ──────────────────────────────────────────────────────────      │
│      function createSeason(bytes32 farmId, bytes32 seasonId, string calldata name,   │
│          string calldata pType, string calldata variety,                             │
│          uint256 area, uint256 startDate) external                                   │
│          onlyRole(SYSTEM_WRITER_ROLE) nonReentrant whenNotPaused                     │
│          returns (uint256)                                                           │
│      {                                                                               │
│          require(seasons[seasonId].createdAt == 0, "Season already exists");         │
│          seasons[seasonId] = SeasonData(farmId, seasonId, name, pType, variety,      │
│                                         area, startDate, 0, 0, 0, block.timestamp); │
│          farmSeasons[farmId].push(seasonId);                                         │
│          allSeasonIds.push(seasonId);                                                │
│          emit SeasonCreated(farmId, seasonId, name, startDate);                      │
│          return block.number;                                                        │
│      }                                                                               │
│                                                                                      │
│      function updateSeasonStatus(bytes32 seasonId, uint8 newStatus) external         │
│          onlyRole(SYSTEM_WRITER_ROLE) whenNotPaused                                  │
│      { ... }                                                                         │
│                                                                                      │
│      function getSeason(bytes32 seasonId) external view returns (SeasonData memory)  │
│      { ... }                                                                         │
│                                                                                      │
│      function getFarmSeasons(bytes32 farmId) external view returns (bytes32[] memory)│
│      { ... }                                                                         │
│  }                                                                                   │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2. FarmingProcessContract

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         FarmingProcessContract — Solidity Design                      │
│──────────────────────────────────────────────────────────────────────────────────────│
│  struct ProcessData {                                                                │
│      bytes32 seasonId;                                                               │
│      bytes32 processId;                                                              │
│      string  processType;   // SOIL_PREP|SEEDING|FERTILIZATION|PEST_CONTROL|HARVEST  │
│      uint256 executionDate;  // Unix timestamp                                        │
│      bytes32 materialsHash;  // keccak256(abi.encode(materials JSON))                 │
│      bytes32 imagesHash;     // keccak256(abi.encode(image URLs))                     │
│      uint256 createdAt;                                                              │
│  }                                                                                   │
│                                                                                      │
│  mapping(bytes32 => ProcessData) public processes;                                    │
│  mapping(bytes32 => bytes32[]) public seasonProcesses;                               │
│                                                                                      │
│  event ProcessAdded(bytes32 indexed seasonId, bytes32 indexed processId,             │
│                    string processType, uint256 executionDate);                        │
│                                                                                      │
│  function addProcess(bytes32 seasonId, bytes32 processId, string calldata pType,     │
│      uint256 execDate, bytes32 matHash, bytes32 imgHash) external                   │
│      onlyRole(SYSTEM_WRITER_ROLE) nonReentrant whenNotPaused returns (uint256)       │
│  { ... }                                                                             │
│                                                                                      │
│  function getProcess(bytes32 processId) external view returns (ProcessData memory)   │
│  { ... }                                                                             │
│                                                                                      │
│  function getSeasonProcesses(bytes32 seasonId)                                       │
│      external view returns (bytes32[] memory)                                        │
│  { ... }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.3. ExportContract

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         ExportContract — Solidity Design                              │
│──────────────────────────────────────────────────────────────────────────────────────│
│  struct ExportData {                                                                 │
│      bytes32 seasonId;                                                               │
│      bytes32 exportId;                                                               │
│      uint256 quantity;      // scaled by 100                                          │
│      string  unit;          // "kg", "ton"                                            │
│      string  warehouse;                                                              │
│      bytes32 qrHash;        // keccak256(seasonId + exportId + quantity + timestamp)  │
│      uint256 exportDate;                                                             │
│      uint256 createdAt;                                                              │
│  }                                                                                   │
│                                                                                      │
│  event ExportRecorded(bytes32 indexed seasonId, bytes32 indexed exportId,            │
│                      uint256 quantity, bytes32 qrHash);                              │
│                                                                                      │
│  function recordExport(bytes32 seasonId, bytes32 exportId, uint256 quantity,         │
│      string calldata unit, string calldata warehouse, bytes32 qrHash)               │
│      external onlyRole(SYSTEM_WRITER_ROLE) nonReentrant whenNotPaused               │
│      returns (uint256)                                                               │
│  { ... }                                                                             │
│                                                                                      │
│  function verifyQR(bytes32 qrHash) external view returns (bool, ExportData memory)   │
│  { ... }                                                                             │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.4. TraceabilityContract

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         TraceabilityContract — Solidity Design                        │
│──────────────────────────────────────────────────────────────────────────────────────│
│  struct TraceData {                                                                  │
│      bytes32 traceId;                                                                │
│      bytes32 seasonId;                                                               │
│      bytes32[] processIds;                                                           │
│      bytes32 exportId;                                                               │
│      bytes32 farmId;                                                                 │
│      string  farmName;                                                               │
│      uint256 createdAt;                                                              │
│  }                                                                                   │
│                                                                                      │
│  event TraceVerified(bytes32 indexed traceId, address indexed verifier,              │
│                     uint256 timestamp);                                              │
│                                                                                      │
│  function createTrace(bytes32 traceId, bytes32 seasonId, bytes32[] calldata pIds,   │
│      bytes32 exportId, bytes32 farmId, string calldata farmName)                    │
│      external onlyRole(SYSTEM_WRITER_ROLE) whenNotPaused                             │
│  { ... }                                                                             │
│                                                                                      │
│  function getTrace(bytes32 traceId) external view returns (TraceData memory)         │
│  { ... }                                                                             │
│                                                                                      │
│  function verify(bytes32 qrHash) external returns (bool)                             │
│  {                                                                                   │
│      // Verify QR hash exists in ExportContract                                      │
│      // Record verification event                                                     │
│      emit TraceVerified(qrHash, msg.sender, block.timestamp);                        │
│      return true;                                                                    │
│  }                                                                                   │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Đặc tả chi tiết REST API

### 6.1. API Naming Convention

```
Base URL:     https://api.bicap.vn/api

Versioning:   URL path-based (e.g., /api/v1/...)
              Current version: v1 (implied, no prefix)

Format:       Request:  JSON (Content-Type: application/json)
              Response: JSON (Content-Type: application/json)

Auth:         Bearer <JWT Token> (Authorization header)

Pagination:   ?page=0&size=20&sort=createdAt,desc

Standard Response Envelope:
{
  "success": true/false,
  "data": { ... },
  "message": "Human-readable message",
  "errors": [ { "field": "email", "message": "Email is required" } ],
  "timestamp": "2026-08-02T12:00:00Z"
}

HTTP Status Codes:
  200 OK             — Success
  201 Created        — Resource created
  204 No Content     — Success (no body)
  400 Bad Request    — Validation error
  401 Unauthorized   — Missing/invalid token
  403 Forbidden      — Insufficient permissions
  404 Not Found      — Resource not found
  409 Conflict       — Duplicate/resource conflict
  422 Unprocessable  — Business rule violation
  500 Internal Error — Server error
```

### 6.2. Auth API

| Method | Endpoint | Auth | Description | Request Body | Response |
|--------|----------|------|-------------|-------------|----------|
| POST | `/api/auth/register` | No | Đăng ký tài khoản mới | `RegisterRequest` | `AuthResponse` |
| POST | `/api/auth/login` | No | Đăng nhập | `LoginRequest` | `AuthResponse` |
| POST | `/api/auth/refresh-token` | No | Làm mới access token | `{ refreshToken }` | `AuthResponse` |
| GET | `/api/auth/me` | Yes | Lấy thông tin user hiện tại | — | `UserResponse` |
| POST | `/api/auth/logout` | Yes | Đăng xuất (invalidate token) | — | `{ message }` |
| POST | `/api/auth/forgot-password` | No | Quên mật khẩu | `{ email }` | `{ message }` |
| POST | `/api/auth/reset-password` | No | Đặt lại mật khẩu | `{ token, newPassword }` | `{ message }` |

### 6.3. Admin API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| POST | `/api/admin` | Yes | SUPER_ADMIN | Tạo tài khoản Admin mới |
| GET | `/api/admin` | Yes | SUPER_ADMIN, ADMIN, MODERATOR | Danh sách Admin (phân trang) |
| GET | `/api/admin/{id}` | Yes | SUPER_ADMIN, ADMIN, MODERATOR | Chi tiết Admin |
| PUT | `/api/admin/{id}` | Yes | SUPER_ADMIN | Cập nhật Admin |
| DELETE | `/api/admin/{id}` | Yes | SUPER_ADMIN | Xóa Admin (soft-delete) |
| GET | `/api/admin/roles` | Yes | Any authenticated | Danh sách roles |
| GET | `/api/admin/farms/pending` | Yes | SUPER_ADMIN, ADMIN | Danh sách nông trại chờ duyệt |
| PUT | `/api/admin/farms/{id}/approve` | Yes | SUPER_ADMIN, ADMIN | Phê duyệt nông trại |
| PUT | `/api/admin/farms/{id}/reject` | Yes | SUPER_ADMIN, ADMIN | Từ chối nông trại (kèm lý do) |
| GET | `/api/admin/farms` | Yes | SUPER_ADMIN, ADMIN | Danh sách tất cả nông trại |
| GET | `/api/admin/products` | Yes | SUPER_ADMIN, ADMIN | Giám sát sản phẩm |
| POST | `/api/admin/contracts/deploy` | Yes | SUPER_ADMIN | Triển khai Smart Contract |
| GET | `/api/admin/contracts` | Yes | SUPER_ADMIN, ADMIN | Danh sách Smart Contract |
| GET | `/api/admin/reports` | Yes | SUPER_ADMIN, ADMIN | Danh sách báo cáo từ users |

### 6.4. Farm API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| POST | `/api/farms` | Yes | FARM_MANAGER | Đăng ký nông trại mới |
| GET | `/api/farms` | Yes | FARM_MANAGER | Danh sách nông trại của tôi |
| GET | `/api/farms/{id}` | Yes | FARM_MANAGER | Chi tiết nông trại |
| PUT | `/api/farms/{id}` | Yes | FARM_MANAGER | Cập nhật thông tin nông trại |
| POST | `/api/farms/{id}/documents` | Yes | FARM_MANAGER | Upload giấy phép/chứng nhận |
| GET | `/api/farms/{id}/certifications` | Yes | FARM_MANAGER | Danh sách chứng nhận |
| DELETE | `/api/farms/{id}/certifications/{certId}` | Yes | FARM_MANAGER | Xóa chứng nhận |
| GET | `/api/farms/{id}/seasons` | Yes | FARM_MANAGER | Danh sách mùa vụ của nông trại |
| POST | `/api/farms/{id}/seasons` | Yes | FARM_MANAGER | Tạo mùa vụ mới (+Blockchain) |
| GET | `/api/farms/{id}/seasons/{seasonId}` | Yes | FARM_MANAGER | Chi tiết mùa vụ |
| POST | `/api/farms/{id}/seasons/{seasonId}/processes` | Yes | FARM_MANAGER | Thêm bước quy trình (+Blockchain) |
| GET | `/api/farms/{id}/seasons/{seasonId}/processes` | Yes | FARM_MANAGER | Timeline quy trình |
| POST | `/api/farms/{id}/seasons/{seasonId}/export` | Yes | FARM_MANAGER | Xuất kho + QR Code (+Blockchain) |
| GET | `/api/farms/{id}/seasons/{seasonId}/qrcode` | Yes | FARM_MANAGER | Lấy QR Code đã tạo |
| GET | `/api/farms/{id}/subscriptions` | Yes | FARM_MANAGER | Danh sách gói đã mua |
| GET | `/api/farms/{id}/iot` | Yes | FARM_MANAGER | Dữ liệu IoT mới nhất |

### 6.5. Retailer API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/retailer/profile` | Yes | RETAILER | Lấy thông tin cá nhân |
| PUT | `/api/retailer/profile` | Yes | RETAILER | Cập nhật thông tin |
| POST | `/api/retailer/documents` | Yes | RETAILER | Upload giấy phép kinh doanh |
| GET | `/api/marketplace/products` | Yes | RETAILER, GUEST | Tìm kiếm sản phẩm (có bộ lọc) |
| GET | `/api/marketplace/products/{id}` | Yes | RETAILER, GUEST | Chi tiết sản phẩm |
| POST | `/api/marketplace/orders` | Yes | RETAILER | Tạo yêu cầu mua hàng |
| GET | `/api/marketplace/orders` | Yes | RETAILER | Lịch sử đơn hàng của tôi |
| GET | `/api/marketplace/orders/{id}` | Yes | RETAILER | Chi tiết đơn hàng |
| PUT | `/api/marketplace/orders/{id}/cancel` | Yes | RETAILER | Hủy yêu cầu mua |
| GET | `/api/qr/scan?data={qrData}` | Yes | RETAILER, GUEST | Quét QR Code |
| GET | `/api/qr/trace/{hash}` | No | PUBLIC | Truy xuất nguồn gốc từ hash |

### 6.6. Order API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/orders` | Yes | FARM_MANAGER, RETAILER, SHIPPING_MGR | Danh sách đơn hàng |
| GET | `/api/orders/{id}` | Yes | Authenticated | Chi tiết đơn hàng |
| PUT | `/api/orders/{id}/accept` | Yes | FARM_MANAGER | Chấp nhận đơn hàng |
| PUT | `/api/orders/{id}/reject` | Yes | FARM_MANAGER | Từ chối đơn hàng |
| PUT | `/api/orders/{id}/cancel` | Yes | RETAILER, FARM_MANAGER | Hủy đơn hàng |
| GET | `/api/orders/{id}/payments` | Yes | Authenticated | Lịch sử thanh toán của đơn |
| POST | `/api/orders/{id}/confirm-delivery` | Yes | RETAILER | Xác nhận đã nhận hàng (BICAP-51) |
| POST | `/api/orders/{id}/delivery-images` | Yes | RETAILER | Upload ảnh nhận hàng (BICAP-52) |

### 6.7. Shipping API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/shipping/orders/completed` | Yes | SHIPPING_MGR | Đơn hàng đã thanh toán chờ vận chuyển |
| POST | `/api/shipping/shipments` | Yes | SHIPPING_MGR | Tạo lô vận chuyển |
| GET | `/api/shipping/shipments` | Yes | SHIPPING_MGR | Danh sách lô vận chuyển |
| GET | `/api/shipping/shipments/{id}` | Yes | SHIPPING_MGR, SHIP_DRIVER | Chi tiết lô |
| PUT | `/api/shipping/shipments/{id}/cancel` | Yes | SHIPPING_MGR | Hủy lô vận chuyển |
| GET | `/api/shipping/vehicles` | Yes | SHIPPING_MGR | Danh sách phương tiện |
| POST | `/api/shipping/vehicles` | Yes | SHIPPING_MGR | Thêm phương tiện |
| PUT | `/api/shipping/vehicles/{id}` | Yes | SHIPPING_MGR | Cập nhật phương tiện |
| DELETE | `/api/shipping/vehicles/{id}` | Yes | SHIPPING_MGR | Xóa phương tiện |
| GET | `/api/shipping/drivers` | Yes | SHIPPING_MGR | Danh sách tài xế |
| POST | `/api/shipping/drivers` | Yes | SHIPPING_MGR | Thêm tài xế |
| PUT | `/api/shipping/drivers/{id}` | Yes | SHIPPING_MGR | Cập nhật tài xế |
| DELETE | `/api/shipping/drivers/{id}` | Yes | SHIPPING_MGR | Xóa tài xế |
| GET | `/api/driver/shipments` | Yes | SHIP_DRIVER | Chuyến hàng của tôi |
| POST | `/api/driver/shipments/{id}/tracking` | Yes | SHIP_DRIVER | Cập nhật tracking |
| POST | `/api/driver/shipments/{id}/pickup` | Yes | SHIP_DRIVER | Xác nhận lấy hàng |
| POST | `/api/driver/shipments/{id}/deliver` | Yes | SHIP_DRIVER | Xác nhận giao hàng |

### 6.8. Payment API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| POST | `/api/payments/deposit` | Yes | RETAILER | Tạo thanh toán đặt cọc |
| GET | `/api/payments/{txRef}/status` | Yes | Authenticated | Kiểm tra trạng thái thanh toán |
| POST | `/api/webhooks/sepay` | No* | SYSTEM | Webhook callback từ Sepay (*HMAC verified) |
| GET | `/api/payments/history` | Yes | Authenticated | Lịch sử thanh toán |

### 6.9. Blockchain API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/blockchain/transactions` | Yes | SUPER_ADMIN, ADMIN | Danh sách giao dịch blockchain |
| GET | `/api/blockchain/transactions/{hash}` | Yes | Authenticated | Chi tiết giao dịch |
| POST| `/api/blockchain/transactions/{id}/retry` | Yes | SUPER_ADMIN | Thử lại giao dịch thất bại |
| GET | `/api/blockchain/contracts` | Yes | Authenticated | Danh sách Smart Contract |
| POST | `/api/blockchain/contracts/deploy` | Yes | SUPER_ADMIN | Triển khai Smart Contract mới |

### 6.10. Notification API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/notifications` | Yes | All | Danh sách thông báo |
| GET | `/api/notifications/unread-count` | Yes | All | Số thông báo chưa đọc |
| PUT | `/api/notifications/{id}/read` | Yes | All | Đánh dấu đã đọc |
| PUT | `/api/notifications/read-all` | Yes | All | Đánh dấu tất cả đã đọc |
| WS | `/ws/notifications` | Yes | All | WebSocket endpoint (STOMP) |

### 6.11. IoT API

| Method | Endpoint | Auth | Roles | Description |
|--------|----------|------|-------|-------------|
| GET | `/api/iot/farms/{farmId}/latest` | Yes | FARM_MANAGER, ADMIN | Dữ liệu IoT mới nhất |
| GET | `/api/iot/farms/{farmId}/history` | Yes | FARM_MANAGER | Dữ liệu IoT lịch sử |
| GET | `/api/iot/farms/{farmId}/alerts` | Yes | FARM_MANAGER | Cảnh báo IoT |

---

## 7. Sequence Diagram cho các luồng nghiệp vụ chính

### 7.1. Luồng đăng ký và xác thực

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         SEQUENCE DIAGRAM: USER REGISTRATION & LOGIN                    │
│                                                                                       │
│  User        Browser      AuthController    AuthService    UserRepo    DB/Redis       │
│   │             │               │               │             │           │           │
│   │  Register   │               │               │             │           │           │
│   │────────────►│               │               │             │           │           │
│   │             │  POST /api/auth/register      │             │           │           │
│   │             │──────────────►│               │             │           │           │
│   │             │               │  register()   │             │           │           │
│   │             │               │──────────────►│             │           │           │
│   │             │               │               │ validate    │           │           │
│   │             │               │               │ input       │           │           │
│   │             │               │               │ email exists?│           │           │
│   │             │               │               │────────────►│           │           │
│   │             │               │               │◄─── false ──│           │           │
│   │             │               │               │             │           │           │
│   │             │               │               │ hash pwd    │           │           │
│   │             │               │               │ BCrypt      │           │           │
│   │             │               │               │             │           │           │
│   │             │               │               │ save user   │           │           │
│   │             │               │               │─────────────────────────►│         │
│   │             │               │               │◄──────── OK ─────────────│         │
│   │             │               │               │             │           │           │
│   │             │               │               │ assign      │           │           │
│   │             │               │               │ default role│           │           │
│   │             │               │               │             │           │           │
│   │             │               │◄── AuthResp ──│             │           │           │
│   │             │◄── 201 OK ────│               │             │           │           │
│   │             │  {accessToken,│               │             │           │           │
│   │             │   refreshToken│               │             │           │           │
│   │             │   user}       │               │             │           │           │
│   │             │               │               │             │           │           │
│   │  Login      │               │               │             │           │           │
│   │────────────►│               │               │             │           │           │
│   │             │  POST /api/auth/login         │             │           │           │
│   │             │──────────────►│               │             │           │           │
│   │             │               │  login()      │             │           │           │
│   │             │               │──────────────►│             │           │           │
│   │             │               │               │ authenticate│           │           │
│   │             │               │               │────────────►│           │           │
│   │             │               │               │◄─User+Roles─│           │           │
│   │             │               │               │             │           │           │
│   │             │               │               │ check pwd   │           │           │
│   │             │               │               │ BCrypt.match│           │           │
│   │             │               │               │             │           │           │
│   │             │               │               │ gen JWT     │           │           │
│   │             │               │               │ accessToken │           │           │
│   │             │               │               │ refreshToken│           │           │
│   │             │               │               │             │           │           │
│   │             │               │               │ cache session│          │           │
│   │             │               │               │─────────────┼──────────►│         │
│   │             │               │               │◄──── OK ────┼──────────│         │
│   │             │               │               │             │           │           │
│   │             │               │◄── AuthResp ──│             │           │           │
│   │             │◄── 200 OK ────│               │             │           │           │
│   │◄─── JWT ────│               │               │             │           │           │
│   │             │               │               │             │           │           │
│   │  API Call   │               │               │             │           │           │
│   │────────────►│               │               │             │           │           │
│   │             │  GET /api/farms (Bearer token)│             │           │           │
│   │             │──────────────►│               │             │           │           │
│   │             │               │  JwtFilter:   │             │           │           │
│   │             │               │  validate JWT │             │           │           │
│   │             │               │  set Security │             │           │           │
│   │             │               │  Context      │             │           │           │
│   │             │               │               │             │           │           │
│   │             │               │  @PreAuthorize│             │           │           │
│   │             │               │  check role   │             │           │           │
│   │             │               │               │             │           │           │
│   │             │◄── Data ──────│               │             │           │           │
│   │◄── Data ────│               │               │             │           │           │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.2. Luồng tạo mùa vụ và ghi Blockchain

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                SEQUENCE DIAGRAM: CREATE FARMING SEASON + BLOCKCHAIN                    │
│                                                                                       │
│  FM       FarmController  SeasonService  BlockchainSvc  MySQL    Redis    VeChainThor │
│   │             │               │              │          │        │           │      │
│   │  POST /api/farms/{id}/seasons              │          │        │           │      │
│   │────────────►│               │              │          │        │           │      │
│   │             │ createSeason()│              │          │        │           │      │
│   │             │──────────────►│              │          │        │           │      │
│   │             │               │ ① validate   │          │        │           │      │
│   │             │               │   - Farm approved?     │        │           │      │
│   │             │               │   - Subscription active?│       │           │      │
│   │             │               │   - Input valid?       │        │           │      │
│   │             │               │              │          │        │           │      │
│   │             │               │ ② save season│          │        │           │      │
│   │             │               │─────────────────────────►│        │           │      │
│   │             │               │◄──── entity ─────────────│        │           │      │
│   │             │               │              │          │        │           │      │
│   │             │               │ ③ write to   │          │        │           │      │
│   │             │               │   blockchain │          │        │           │      │
│   │             │               │─────────────►│          │        │           │      │
│   │             │               │              │ get nonce│        │           │      │
│   │             │               │              │─────────────────►│           │      │
│   │             │               │              │◄── nonce ────────│           │      │
│   │             │               │              │          │        │           │      │
│   │             │               │              │ build TX │        │           │      │
│   │             │               │              │ ABI encode       │           │      │
│   │             │               │              │ sign TX  │        │           │      │
│   │             │               │              │          │        │           │      │
│   │             │               │              │ Submit TX│        │           │      │
│   │             │               │              │─────────────────────────────────►│  │
│   │             │               │              │          │        │  Mining...│      │
│   │             │               │              │          │        │  12 blocks│      │
│   │             │               │              │◄── txHash────────────────────────│  │
│   │             │               │              │          │        │           │      │
│   │             │               │              │ save TX  │        │           │      │
│   │             │               │              │ record   │        │           │      │
│   │             │               │              │─────────────────────────►│      │
│   │             │               │              │◄──── OK ──────────────────│      │
│   │             │               │              │          │        │           │      │
│   │             │               │              │ update   │        │           │      │
│   │             │               │              │ season   │        │           │      │
│   │             │               │              │ status=  │        │           │      │
│   │             │               │              │ CONFIRMED│        │           │      │
│   │             │               │              │─────────────────────────►│      │
│   │             │               │              │◄──── OK ──────────────────│      │
│   │             │               │              │          │        │           │      │
│   │             │               │◄── SeasonResp│          │        │           │      │
│   │             │◄── 201 Created│              │          │        │           │      │
│   │◄─── JSON ───│  {season,     │              │          │        │           │      │
│   │    txHash    │   txHash,     │              │          │        │           │      │
│   │    explorer  │   explorerUrl}│              │          │        │           │      │
│   │    link      │               │              │          │        │           │      │
│   │             │               │              │          │        │           │      │
│   │  [ALT: TX Failure]          │              │          │        │           │      │
│   │             │               │              │ retry 3x │        │           │      │
│   │             │               │              │──fail──► │        │           │      │
│   │             │               │              │ queue for│        │           │      │
│   │             │               │              │ async    │        │           │      │
│   │             │               │              │ retry    │        │           │      │
│   │             │               │◄── Partial ──│          │        │           │      │
│   │◄── 200 OK ──│  {season,      │   success   │          │        │           │      │
│   │  (warning)   │   status:     │              │          │        │           │      │
│   │              │   PENDING_BC} │              │          │        │           │      │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.3. Luồng đặt mua và thanh toán

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                  SEQUENCE DIAGRAM: ORDER CREATION & DEPOSIT PAYMENT                    │
│                                                                                       │
│  Retailer  RetailerCtrl  OrderSvc  ProductRepo  OrderRepo  PaymentSvc  SepayGateway  │
│     │           │           │           │           │           │           │         │
│     │ POST /api/marketplace/orders     │           │           │           │         │
│     │──────────►│           │           │           │           │           │         │
│     │           │ createOrder()         │           │           │           │         │
│     │           │──────────►│           │           │           │           │         │
│     │           │           │ getProduct│           │           │           │         │
│     │           │           │──────────►│           │           │           │         │
│     │           │           │◄──product─│           │           │           │         │
│     │           │           │           │           │           │           │         │
│     │           │           │ validate: │           │           │           │         │
│     │           │           │ - product AVAILABLE?  │           │           │         │
│     │           │           │ - qty <= available?   │           │           │         │
│     │           │           │ - price > 0?          │           │           │         │
│     │           │           │           │           │           │           │         │
│     │           │           │ create order          │           │           │         │
│     │           │           │ status=PENDING        │           │           │         │
│     │           │           │──────────────────────►│           │           │         │
│     │           │           │◄────── order ─────────│           │           │         │
│     │           │           │           │           │           │           │         │
│     │           │           │ send notif│           │           │           │         │
│     │           │           │ to Farm Mgr           │           │           │         │
│     │           │           │           │           │           │           │         │
│     │           │◄── OrderResp         │           │           │           │         │
│     │◄── 201 ───│           │           │           │           │           │         │
│     │           │           │           │           │           │           │         │
│     │  ... Farm Manager accepts order (SRS-FM-014) ...          │           │         │
│     │  ... Order status → ACCEPTED ...                          │           │         │
│     │           │           │           │           │           │           │         │
│     │ POST /api/payments/deposit        │           │           │           │         │
│     │──────────►│           │           │           │           │           │         │
│     │           │           │           │           │ createDeposit()       │         │
│     │           │           │           │           │──────────────────────►│         │
│     │           │           │           │           │           │           │         │
│     │           │           │           │           │ verify order ACCEPTED │         │
│     │           │           │           │           │ check 24h window      │         │
│     │           │           │           │           │ amount = price*qty*0.3│         │
│     │           │           │           │           │           │           │         │
│     │           │           │           │           │ create payment record │         │
│     │           │           │           │           │ status=PENDING        │         │
│     │           │           │           │           │           │           │         │
│     │           │           │           │           │ call Sepay│           │         │
│     │           │           │           │           │──────────────────────►│         │
│     │           │           │           │           │◄── paymentUrl ────────│         │
│     │           │           │           │           │           │           │         │
│     │           │           │           │           │◄── DepositResp───────│         │
│     │           │◄────────────────────────────────────── {paymentUrl, txRef}│         │
│     │◄── 200 ───│  { paymentUrl, txRef } │           │           │           │         │
│     │           │           │           │           │           │           │         │
│     │  Redirect to Sepay payment page    │           │           │           │         │
│     │──────────────────────────────────────────────────────────────────────►│         │
│     │  ... User pays on Sepay website ...│           │           │           │         │
│     │           │           │           │           │           │           │         │
│     │           │           │           │           │  Sepay callback       │         │
│     │           │           │           │           │◄──────────────────────│         │
│     │           │           │           │           │ verify HMAC signature │         │
│     │           │           │           │           │ update payment=PAID   │         │
│     │           │           │           │           │ update order=PAID     │         │
│     │           │           │           │           │ activate subscription │         │
│     │           │           │           │           │ send notification     │         │
│     │           │           │           │           │           │           │         │
│     │  GET /api/payments/{txRef}/status  │           │           │           │         │
│     │──────────►│           │           │           │──────────►│           │         │
│     │◄── 200 ───│  { status: "COMPLETED" }         │           │           │         │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.4. Luồng vận chuyển và giao nhận

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                SEQUENCE DIAGRAM: SHIPMENT CREATION & DELIVERY                          │
│                                                                                       │
│  SM      ShippingCtrl  ShipSvc  DriverRepo  VehicleRepo  ShipRepo  TrackingRepo      │
│   │           │           │          │           │           │           │            │
│   │ POST /api/shipping/shipments     │           │           │           │            │
│   │──────────►│           │          │           │           │           │            │
│   │           │ createShipment()     │           │           │           │            │
│   │           │──────────►│          │           │           │           │            │
│   │           │           │ get avail│           │           │           │            │
│   │           │           │ drivers  │           │           │           │            │
│   │           │           │─────────►│           │           │           │            │
│   │           │           │◄─drivers─│           │           │           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ get avail│           │           │           │            │
│   │           │           │ vehicles │           │           │           │            │
│   │           │           │────────────────────►│           │           │            │
│   │           │           │◄──── vehicles ──────│           │           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ create shipment    │           │           │            │
│   │           │           │──────────────────────────────────►│           │            │
│   │           │           │◄──────── shipment ────────────────│           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ update   │           │           │           │            │
│   │           │           │ driver   │           │           │           │            │
│   │           │           │ status=  │           │           │           │            │
│   │           │           │ ON_TRIP  │           │           │           │            │
│   │           │           │─────────►│           │           │           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ update vehicle        │           │           │            │
│   │           │           │ status=IN_USE         │           │           │            │
│   │           │           │──────────────────────►│           │           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ notify   │           │           │           │            │
│   │           │           │ driver   │           │           │           │            │
│   │           │           │ (push)   │           │           │           │            │
│   │           │           │          │           │           │           │            │
│   │           │◄── ShipResp          │           │           │           │            │
│   │◄── 201 ───│           │          │           │           │           │            │
│   │           │           │          │           │           │           │            │
│   │  ... Driver picks up at farm (BICAP-66) ...              │           │            │
│   │           │           │          │           │           │           │            │
│   │           │ POST /api/driver/shipments/{id}/pickup       │           │            │
│   │           │◄──────────────────── (from Driver Mobile App) │           │            │
│   │           │ confirmPickup()      │           │           │           │            │
│   │           │──────────►│          │           │           │           │            │
│   │           │           │ add      │           │           │           │            │
│   │           │           │ tracking │           │           │           │            │
│   │           │           │─────────────────────────────────────────────►│            │
│   │           │           │◄──── OK ─────────────────────────────────────│            │
│   │           │           │          │           │           │           │            │
│   │           │           │ update   │           │           │           │            │
│   │           │           │ shipment │           │           │           │            │
│   │           │           │ status=  │           │           │           │            │
│   │           │           │ IN_TRANSIT│          │           │           │            │
│   │           │           │──────────────────────────────────►│           │            │
│   │           │           │◄──── OK ──────────────────────────│           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ notify   │           │           │           │            │
│   │           │           │ FM + RT  │           │           │           │            │
│   │           │◄── 200 ───│          │           │           │           │            │
│   │           │           │          │           │           │           │            │
│   │  ... Driver delivers to retailer (BICAP-67) ...          │           │            │
│   │           │           │          │           │           │           │            │
│   │           │ POST /api/driver/shipments/{id}/deliver      │           │            │
│   │           │◄─────────────────────────────────────────────────────────│            │
│   │           │ confirmDelivery()     │           │           │           │            │
│   │           │──────────►│          │           │           │           │            │
│   │           │           │ add      │           │           │           │            │
│   │           │           │ tracking │           │           │           │            │
│   │           │           │ (final)  │           │           │           │            │
│   │           │           │─────────────────────────────────────────────►│            │
│   │           │           │          │           │           │           │            │
│   │           │           │ update   │           │           │           │            │
│   │           │           │ shipment │           │           │           │            │
│   │           │           │ status=  │           │           │           │            │
│   │           │           │ DELIVERED│           │           │           │            │
│   │           │           │──────────────────────────────────►│           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ free     │           │           │           │            │
│   │           │           │ driver   │           │           │           │            │
│   │           │           │─────────►│           │           │           │            │
│   │           │           │          │           │           │           │            │
│   │           │           │ notify   │           │           │           │            │
│   │           │           │ retailer │           │           │           │            │
│   │           │           │ to confirm (48h window)         │           │            │
│   │           │◄── 200 ───│          │           │           │           │            │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 7.5. Luồng truy xuất nguồn gốc QR Code

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                     SEQUENCE DIAGRAM: QR CODE TRACEABILITY                             │
│                                                                                       │
│  Guest     GuestApp     QRController   BlockchainSvc   VeChainThor   MySQL            │
│   │           │              │               │             │           │              │
│   │ Scan QR   │              │               │             │           │              │
│   │──────────►│              │               │             │           │              │
│   │           │ Decode QR    │               │             │           │              │
│   │           │ extract URL  │               │             │           │              │
│   │           │ /trace/{hash}│               │             │           │              │
│   │           │              │               │             │           │              │
│   │           │ GET /api/qr/trace/{hash}     │             │           │              │
│   │           │─────────────►│               │             │           │              │
│   │           │              │ getTrace(hash)│             │           │              │
│   │           │              │──────────────►│             │           │              │
│   │           │              │               │             │           │              │
│   │           │              │               │ ① Query     │           │              │
│   │           │              │               │ Traceability│           │              │
│   │           │              │               │ Contract    │           │              │
│   │           │              │               │────────────►│           │              │
│   │           │              │               │◄── Trace ───│           │              │
│   │           │              │               │  {seasonId, │           │              │
│   │           │              │               │   processIds│           │              │
│   │           │              │               │   exportId, │           │              │
│   │           │              │               │   farmId}   │           │              │
│   │           │              │               │             │           │              │
│   │           │              │               │ ② Get season│           │              │
│   │           │              │               │ data from   │           │              │
│   │           │              │               │ SeasonContract│          │              │
│   │           │              │               │────────────►│           │              │
│   │           │              │               │◄── Season ──│           │              │
│   │           │              │               │             │           │              │
│   │           │              │               │ ③ Get all   │           │              │
│   │           │              │               │ processes   │           │              │
│   │           │              │               │────────────►│           │              │
│   │           │              │               │◄─Process[]──│           │              │
│   │           │              │               │             │           │              │
│   │           │              │               │ ④ Get farm  │           │              │
│   │           │              │               │ info (off-  │           │              │
│   │           │              │               │ chain)      │           │              │
│   │           │              │               │─────────────────────────►│              │
│   │           │              │               │◄── FarmInfo ──────────────│              │
│   │           │              │               │   (name, certs, address)  │              │
│   │           │              │               │             │           │              │
│   │           │              │               │ ⑤ Get IoT   │           │              │
│   │           │              │               │ summary     │           │              │
│   │           │              │               │─────────────────────────►│              │
│   │           │              │               │◄── IoT avg ──────────────│              │
│   │           │              │               │             │           │              │
│   │           │              │               │ ⑥ Verify    │           │              │
│   │           │              │               │ on-chain    │           │              │
│   │           │              │               │────────────►│           │              │
│   │           │              │               │◄── verified─│           │              │
│   │           │              │               │             │           │              │
│   │           │              │◄── TraceResp──│             │           │              │
│   │           │◄── 200 OK ───│               │             │           │              │
│   │◄── UI ────│              │               │             │           │              │
│   │  Shows:   │              │               │             │           │              │
│   │  - Farm   │              │               │             │           │              │
│   │  - Season │              │               │             │           │              │
│   │  - Process│              │               │             │           │              │
│   │    timeline              │               │             │           │              │
│   │  - Certs  │              │               │             │           │              │
│   │  - IoT    │              │               │             │           │              │
│   │  - Verified│             │               │             │           │              │
│   │    on BC ✓│              │               │             │           │              │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Thiết kế bảo mật chi tiết

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              SECURITY DESIGN DETAILS                                   │
│                                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ LAYER               │ MECHANISM                    │ IMPLEMENTATION              │ │
│  │─────────────────────│──────────────────────────────│─────────────────────────────│ │
│  │ Authentication      │ JWT (HS512)                  │ JwtTokenProvider            │ │
│  │                     │ Access Token: 15 min TTL     │ JwtAuthenticationFilter     │ │
│  │                     │ Refresh Token: 7 days TTL    │ /api/auth/refresh-token     │ │
│  │                     │                              │                             │ │
│  │ Authorization       │ RBAC (5 standard roles)      │ @PreAuthorize on controllers│ │
│  │                     │ + granular permissions       │ hasRole() / hasAuthority()  │ │
│  │                     │                              │                             │ │
│  │ Password Storage    │ BCrypt (strength=12)         │ BCryptPasswordEncoder       │ │
│  │                     │                              │                             │ │
│  │ Transport Security  │ HTTPS/TLS 1.2+               │ Nginx SSL termination       │ │
│  │                     │ HSTS header                  │ Spring Security headers     │ │
│  │                     │                              │                             │ │
│  │ API Security        │ Rate limiting (Redis)        │ 60 req/min per IP           │ │
│  │                     │ CORS (whitelisted origins)   │ 120 req/min per user        │ │
│  │                     │ CSRF disabled (stateless API)│ CorsConfig                  │ │
│  │                     │ Idempotency keys             │ X-Idempotency-Key header    │ │
│  │                     │                              │                             │ │
│  │ Blockchain Security │ Smart contract access control│ OpenZeppelin AccessControl  │ │
│  │                     │ Reentrancy guard             │ OpenZeppelin ReentrancyGuard│ │
│  │                     │ Pausable (emergency stop)    │ OpenZeppelin Pausable       │ │
│  │                     │ UUPS proxy pattern           │ OpenZeppelin UUPSUpgradeable│ │
│  │                     │ TX signing (off-chain)       │ System private key (env var)│ │
│  │                     │                              │                             │ │
│  │ Payment Security    │ HMAC-SHA256 webhook verify   │ SepayService.verifySignature│ │
│  │                     │ Idempotency key per payment  │ X-Idempotency-Key header    │ │
│  │                     │ PCI DSS SAQ A (redirect flow)│ Hosted payment page         │ │
│  │                     │                              │                             │ │
│  │ IoT Security        │ MQTTS (TLS on port 8883)     │ MQTT broker config          │ │
│  │                     │ Device auth (user/pass or    │ Per-device credentials      │ │
│  │                     │   X.509 client certificate)  │                             │ │
│  │                     │ ACL per farm topic           │ bicap/iot/{farmId}/* only   │ │
│  │                     │ Sensor data validation       │ Threshold sanity checks     │ │
│  │                     │                              │                             │ │
│  │ Data Protection     │ PII encryption at rest       │ AES-256 (future)            │ │
│  │                     │ Input sanitization           │ Spring @Valid + custom      │ │
│  │                     │ SQL injection prevention     │ Spring Data JPA (prepared)  │ │
│  │                     │ XSS prevention               │ Output encoding (frontend)  │ │
│  │                     │ File upload validation       │ Type, size, extension check │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Phụ lục

### 9.1. Ma trận truy xuất thiết kế (Design Traceability Matrix)

| Module | Class Diagram | Sequence Diagram | Database Tables | API Spec | Smart Contract | Frontend Components |
|--------|--------------|------------------|-----------------|----------|----------------|---------------------|
| Admin | §2.3 | §7.1 | users, roles, permissions, farms, products | §6.2, §6.3 | — | §4.2 |
| Farm Management | §2.4 | §7.2 | farms, seasons, processes, products, qrcodes | §6.4 | §5.1, §5.2, §5.3 | §4.3 |
| Retailer | §2.5 | §7.3 | orders, payments, products | §6.5, §6.6 | — | §4.4 |
| Shipping | §2.7 | §7.4 | shipments, tracking, drivers, vehicles | §6.7 | — | §4.5, §4.6 |
| Payment | §2.8 | §7.3 | payments, subscriptions | §6.8 | — | — |
| Blockchain | §2.9 | §7.2, §7.5 | blockchain_transactions | §6.9 | §5.1-§5.4 | — |
| Notification | §2.10 | — | notifications | §6.10 | — | — |
| IoT | §2.11 | — | iot_data | §6.11 | — | — |
| Guest | — | §7.5 | — | §6.5, §6.11 | — | §4.7 |
| Content & Report | §2.12 | — | reports | — | — | — |

### 9.2. Bảng chú giải thuật ngữ UML

| Ký hiệu | Ý nghĩa | Ví dụ trong tài liệu |
|---------|---------|---------------------|
| `────►` (solid arrow) | Association (quan hệ) | User → Role |
| `────►` (hollow triangle) | Inheritance (kế thừa) | User implements UserDetails |
| `◇───►` (hollow diamond) | Aggregation (tập hợp yếu) | Farm ◇→ FarmCertification |
| `◆───►` (filled diamond) | Composition (tập hợp mạnh) | Order ◆→ Payment |
| `1`, `1:N`, `M:N` | Cardinality | User 1:N Farm |
| `<<Service>>` | Stereotype (định danh loại) | AuthService |
| `+`, `-` | Visibility: public, private | + createAdmin(...) |
| `{PK}`, `{FK}` | Constraint: Primary/Foreign Key | id (PK) |
| `──►` (lifeline) | Sequence diagram participant | User ──► |
| `────►►` (solid arrowhead) | Synchronous message | POST /api/auth/login |
| `◄────` (dashed arrow) | Return message | ◄── AuthResp |

### 9.3. Danh sách các file liên quan

| Layer | File Path | Module | Status |
|-------|-----------|--------|--------|
| Entity | `entity/User.java` | Auth | ✅ Implemented |
| Entity | `entity/Role.java` | Auth | ✅ Implemented |
| Entity | `entity/Permission.java` | Auth | ✅ Implemented |
| Entity | `entity/Order.java` | Order | ✅ Implemented |
| Entity | `entity/Payment.java` | Payment | ✅ Implemented |
| Entity | `entity/ServicePackage.java` | Subscription | ✅ Implemented |
| Entity | `entity/Subscription.java` | Subscription | ✅ Implemented |
| Repository | `repository/UserRepository.java` | Auth/Admin | ✅ Implemented |
| Repository | `repository/RoleRepository.java` | Auth | ✅ Implemented |
| Repository | `repository/OrderRepository.java` | Order | ✅ Implemented |
| Repository | `repository/PaymentRepository.java` | Payment | ✅ Implemented |
| Service | `service/AuthService.java` | Auth | ✅ Implemented |
| Service | `service/AdminService.java` | Admin | ✅ Implemented |
| Service | `service/OrderService.java` | Order | ✅ Implemented |
| Service | `service/SepayService.java` | Payment | ✅ Implemented |
| Controller | `controller/AuthController.java` | Auth | ✅ Implemented |
| Controller | `controller/AdminController.java` | Admin | ✅ Implemented |
| Controller | `controller/OrderController.java` | Order | ✅ Implemented |
| Security | `common/security/JwtTokenProvider.java` | Auth | ✅ Implemented |
| Security | `common/security/JwtAuthenticationFilter.java` | Auth | ✅ Implemented |
| Config | `config/SecurityConfig.java` | Auth | ✅ Implemented |
| Config | `config/DatabaseSeeder.java` | DB | ✅ Implemented |
| Frontend | `frontend/src/components/Auth/LoginForm.tsx` | Auth UI | ✅ Implemented |
| Frontend | `frontend/src/components/Auth/RegisterForm.tsx` | Auth UI | ✅ Implemented |
| Frontend | `frontend/src/pages/Auth/AuthPage.tsx` | Auth UI | ✅ Implemented |
| Frontend | `frontend/src/pages/FarmManager/ServicePackages.tsx` | Farm UI | ✅ Implemented |

### 9.4. Trạng thái triển khai

| Module | Backend | Frontend | Smart Contract | Trạng thái |
|--------|---------|----------|----------------|------------|
| Auth (BICAP-72) | ✅ Entity, Repository, Service, Controller, Security | ✅ Login, Register | — | Hoàn thành |
| Admin (BICAP-1) | ✅ Entity, Repository, Service, Controller | ⏳ Admin Dashboard | — | Đang triển khai |
| Farm Management (BICAP-7..27) | ⏳ Entity một phần | ⏳ Service Packages | ⏳ | Đang triển khai |
| Retailer (BICAP-36..53) | ⏳ | ⏳ | — | Chưa bắt đầu |
| Shipping (BICAP-54..62) | ⏳ | ⏳ | — | Chưa bắt đầu |
| Shipping Driver (BICAP-63..68) | ⏳ | ⏳ | — | Chưa bắt đầu |
| Guest (BICAP-69..71) | ⏳ | ⏳ | — | Chưa bắt đầu |
| Payment (BICAP-78) | ✅ Sepay integration | ⏳ Payment UI | — | Đang triển khai |
| Blockchain (BICAP-73,74) | ⏳ | ⏳ | ⏳ | Chưa bắt đầu |
| Notification (BICAP-77) | ✅ Entity, Repository, Service, Controller, SSE, Email | ⏳ Notification UI | — | Đang triển khai |
| IoT (BICAP-26) | ⏳ | ⏳ | — | Chưa bắt đầu |
| Database (BICAP-79) | ✅ DDL đầy đủ | — | — | Hoàn thành |

---

> **Tài liệu được biên soạn bởi:** Nhóm phát triển BICAP  
> **Ngày hoàn thành:** 02/08/2026  
> **Mã Jira:** BICAP-94 — Detail Design  
> **Trạng thái:** Bản nháp (Draft) — Cần review và cập nhật theo tiến độ triển khai
