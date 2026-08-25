# BÁO CÁO TỔNG KẾT DỰ ÁN

## TÍCH HỢP BLOCKCHAIN TRONG SẢN XUẤT NÔNG SẢN SẠCH (BICAP)

| Thông tin | Chi tiết |
|---|---|
| **Tên dự án (EN)** | Blockchain Integration in Clean Agricultural Production |
| **Tên dự án (VN)** | Tích hợp Blockchain trong sản xuất nông sản sạch |
| **Viết tắt** | BICAP |
| **Loại tài liệu** | Báo cáo tổng kết toàn bộ quá trình phát triển |
| **Thời gian phát triển** | 22/07/2026 → 12/08/2026 |
| **Số commit** | 70 commits trên nhánh `main` |

---

## Mục lục

1. [Giới thiệu chung](#1-giới-thiệu-chung)
2. [Công nghệ & Kiến trúc hệ thống](#2-công-nghệ--kiến-trúc-hệ-thống)
3. [Tổng quan các giai đoạn phát triển](#3-tổng-quan-các-giai-đoạn-phát-triển)
4. [Chi tiết từng module đã triển khai](#4-chi-tiết-từng-module-đã-triển-khai)
5. [Cơ sở dữ liệu & Cache](#5-cơ-sở-dữ-liệu--cache)
6. [Blockchain & Smart Contract](#6-blockchain--smart-contract)
7. [Frontend Web](#7-frontend-web)
8. [CI/CD & Triển khai](#8-cicd--triển-khai)
9. [Kiểm thử](#9-kiểm-thử)
10. [Hướng dẫn chạy dự án](#10-hướng-dẫn-chạy-dự-án)
11. [Tổng kết: đã làm được gì & còn thiếu gì](#11-tổng-kết)

---

## 1. Giới thiệu chung

### 1.1. Bối cảnh

Dự án **BICAP** ra đời nhằm giải quyết nhu cầu ngày càng tăng về **nông sản sạch, có thể truy xuất nguồn gốc** tại Việt Nam. Nhiều nông trại và hợp tác xã quy mô vừa và nhỏ gặp khó khăn trong việc giám sát, quản lý quy trình sản xuất và đáp ứng tiêu chuẩn an toàn thực phẩm. Người tiêu dùng ngày nay cũng mong muốn biết rõ quy trình sản xuất của sản phẩm mình dùng.

### 1.2. Giải pháp

Dự án ứng dụng công nghệ **Blockchain** để truy xuất nguồn gốc nông sản **từ trang trại đến bàn ăn** (farm to table):

- Tối ưu giám sát quy trình sản xuất nông nghiệp.
- Triển khai truy xuất nguồn gốc sản phẩm bằng blockchain.
- Cung cấp **QR code** giúp người tiêu dùng kiểm tra nguồn gốc nông sản.
- Tối ưu chi phí, hiệu quả qua phân tích và dự báo.
- Kết nối nông trại với nhà phân phối bán lẻ.

### 1.3. Các nhóm người dùng (Actor)

| Actor | Ứng dụng | Vai trò |
|---|---|---|
| **Admin** | Web App | Quản trị hệ thống, phê duyệt nông trại, quản lý sản phẩm, triển khai smart contract |
| **Farm Manager** | Web App | Quản lý nông trại, mùa vụ, xuất bán, nhận thông báo IoT |
| **Retailer** | Web App | Tìm kiếm sản phẩm, đặt hàng, đặt cọc, truy xuất QR |
| **Shipping Manager** | Web App | Quản lý vận chuyển, phương tiện, tài xế |
| **Ship Driver** | Mobile App | Cập nhật quy trình giao hàng, quét QR |
| **Guest** | Web/Mobile | Xem sản phẩm, nội dung giáo dục |

---

## 2. Công nghệ & Kiến trúc hệ thống

### 2.1. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.0, Spring Security, Spring Data JPA, Spring Validation, Spring Mail, Actuator |
| **Xác thực** | JWT (jjwt 0.12.5), HS256, BCrypt, RBAC (`@EnableMethodSecurity` + `@PreAuthorize`) |
| **Database** | MySQL 5.7.41 (production), H2 in-memory (dev/test) |
| **Cache** | Redis 8.6 (cấu hình đã chuẩn bị) |
| **Frontend** | React 19 + Vite 8 + TypeScript 6 (2 ứng dụng: farm portal & admin) |
| **Blockchain** | VeChainThor (EVM-compatible), Solidity ^0.8.24, OpenZeppelin Upgradeable |
| **Thanh toán** | Cổng Sepay (webhook bank transfer) |
| **CI/CD** | GitHub Actions, Docker, Docker Compose, Nginx |

### 2.2. Kiến trúc tổng thể

Hệ thống theo **kiến trúc 3 tầng** (Presentation / Application / Data), với mô hình **Monolith** cho backend (phù hợp đội nhỏ / MVP — ADR-001).

```
┌─────────────────────────────────────────────────────────────────┐
│  Presentation Layer                                             │
│   ├── Admin Web App   (admin-web/, React Vite)                  │
│   ├── Farm Portal     (frontend/, React Vite)                   │
│   └── (Retailer / Shipping / Mobile - tương lai)                │
├─────────────────────────────────────────────────────────────────┤
│  Application Layer  →  Spring Boot Backend (:8080)              │
│   ├── Security (JWT filter, RBAC, rate limit)                   │
│   ├── Controllers (17) → Services (19) → Repositories (17)     │
│   ├── BlockchainService (mock/live VeChainThor)                 │
│   ├── NotificationService (SSE real-time)                       │
│   └── SepayService (webhook thanh toán)                         │
├─────────────────────────────────────────────────────────────────┤
│  Data Layer                                                     │
│   ├── MySQL (dữ liệu vận hành - operational data)               │
│   ├── Redis (cache, optional)                                   │
│   └── VeChainThor (dữ liệu truy xuất bất biến - on-chain)       │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3. Các quyết định kiến trúc quan trọng (ADR)

| ADR | Quyết định | Lý do |
|---|---|---|
| ADR-001 | Backend Monolith (thay vì microservices) | Đội nhỏ, MVP, đơn giản hóa triển khai |
| ADR-002 | Java Spring Boot 3.x / JDK 21 | Hệ sinh thái mạnh, bảo mật tích hợp |
| ADR-003 | React/Next.js TypeScript cho web | Đồng bộ type giữa front/back |
| ADR-004 | React Native cho mobile | (Kế hoạch) |
| ADR-005 | **VeChainThor** làm blockchain | Dual-token VET/VTHO giá rẻ, PoA, hướng supply-chain doanh nghiệp |
| ADR-006 | **Dual Storage**: MySQL + VeChainThor | Dữ liệu vận hành lưu MySQL, dữ liệu truy xuất (mùa vụ, quy trình, xuất bán) băm lên chain, liên kết qua `txHash` — tiết kiệm gas |
| ADR-007 | Redis 8.6 làm cache layer | Hiệu năng đọc |

### 2.4. Quy mô codebase

- **Backend**: 142 file Java — 17 Controllers, 19 Services, 17 Repositories, 23 Entities, 43 DTOs, 19 file test.
- **Frontend**: 2 ứng dụng React TS.
- **Blockchain**: 4 smart contract trong 1 file Solidity (`Traceability.sol`, ~19 KB).
- **Docs**: 10 tài liệu (SRS, Architecture Design, Detail Design, User Requirements, …).

---

## 3. Tổng quan các giai đoạn phát triển

Dự án phát triển qua **70 commits** theo các ticket Jira, chia thành 12 giai đoạn:

| # | Giai đoạn | Ticket | Thời gian | Kết quả chính |
|---|---|---|---|---|
| 1 | Khởi tạo & tài liệu | BICAP-91/92/93, EPIC-8/9 | 22–24/07 | User Requirements, SRS, Architecture Design, CI/CD setup |
| 2 | Core Security & RBAC | BICAP-72 | 25/07 | Spring Security + JWT + RBAC cốt lõi |
| 3 | Quản trị Admin | BICAP-1 | 25–26/07 | Admin CRUD + phân quyền, UI admin |
| 4 | Xác thực Farm Manager | BICAP-7 | 26/07–06/08 | Đăng ký/đăng nhập farm manager |
| 5 | Xác thực Retailer | BICAP-36/37/38 | 28/07–05/08 | Đăng ký + xác thực email, hồ sơ KYC |
| 6 | Thanh toán & gói dịch vụ | BICAP-78 | 29–30/07 | Service Package, Subscription, Sepay webhook |
| 7 | Phê duyệt đăng ký nông trại | BICAP-3 | 02/08 | Admin duyệt/từ chối nông trại |
| 8 | Quản lý chi tiết nông trại | BICAP-4 | 02/08 | Chứng nhận, liên hệ, vị trí, ghi chú admin |
| 9 | Tài liệu Detail Design | BICAP-94 | 02/08 | Detail design toàn hệ thống |
| 10 | Thông báo IoT | BICAP-26 | 05/08 | Cảm biến nhiệt độ/độ ẩm/pH, cảnh báo |
| 11 | Quản lý Smart Contract | BICAP-6 | 06/08 | 4 hợp đồng Solidity + BlockchainService |
| 12 | Thông báo real-time & Mùa vụ | BICAP-77, BICAP-73 | 10–12/08 | SSE notifications, Farming Season API |

---

## 4. Chi tiết từng module đã triển khai

### 4.1. Core Security & RBAC (BICAP-72) — "Làm như thế nào"

**Mục tiêu:** Nền tảng xác thực & phân quyền dùng chung cho toàn hệ thống.

**Cách triển khai:**

- **Entity đa vai trò**: `User` implements `UserDetails`, giữ `Set<Role>` (N:N). `Role` giữ `Set<Permission>` (N:N). Cho phép 1 user mang nhiều vai trò theo SRS.
- **Vai trò hệ thống**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR` (nhóm admin) + `FARM_MANAGER`, `RETAILER`, `SHIPPING_MGR`, `SHIP_DRIVER`, `GUEST` (nhóm chức năng).
- **Quyền chi tiết**: `ADMIN_CREATE/READ/UPDATE/DELETE` — phân quyền mịn theo từng thao tác.
- **JWT Token Provider** (`JwtTokenProvider`): thuật toán HS256, secret nạp từ env, token **có kiểu** (`type` claim):
  - `access` — mặc định 24 giờ.
  - `access` của retailer — 15 phút (chặt hơn).
  - `refresh` — 7 ngày (rotation).
  - `email_verification` — 24 giờ.
- **`JwtAuthenticationFilter`** (OncePerRequest): quét header `Authorization: Bearer`, xác thực chữ ký, đưa thông tin user + role vào `SecurityContextHolder`. Riêng endpoint SSE đọc JWT qua query `?token=` (EventSource không gửi được header). **Chống giả mạo header `X-Actor-Email`**: nếu header tồn tại phải khớp với user đã xác thực.
- **`SecurityConfig`**: stateless, tắt CSRF (thuần REST), mở công khai (`permitAll`): `/api/auth/**`, `/api/public/**`, `GET /api/trace/**`, `GET /uploads/**`, `GET /api/service-packages/**`, `/actuator/health`. Phần còn lại yêu cầu token.
- **`ActorAuthorizer`**: nhóm quyền admin theo header `X-Actor-Email` — `requireSuperAdmin()`, `requireAdminWrite()` (SUPER_ADMIN|ADMIN), `requireAdminView()` (+MODERATOR).
- **`RateLimitFilter`**: giới hạn 30 request/phút/IP với `/api/auth/**` (chống brute-force), trả 429.
- **`SecretConfigValidator`**: **fail-fast** lúc khởi động — từ chối boot nếu thiếu `JWT_SECRET` hoặc `SEPAY_API_KEY`.
- **`LoginAttemptService`**: 5 lần sai → khóa 30 phút (`REQUIRES_NEW` để khóa tồn tại ngay cả khi transaction login rollback).
- **Mật khẩu**: BCrypt.

> ⚠️ **Điểm đáng chú ý**: codebase dùng **hai phong cách phân quyền song song** — portal endpoints lấy user qua JWT principal (`CurrentUser.get()`), còn admin endpoints đọc header `X-Actor-Email` kết hợp `ActorAuthorizer`. Đây là lựa chọn có chủ đích.

---

### 4.2. Quản trị Admin (BICAP-1)

**Chức năng:** Tạo, xem, sửa, xóa tài khoản admin; gán vai trò & quyền.

**Cách triển khai:**

- **Backend**: `AdminController` (`/api/admins`) — danh sách phân trang + lọc (status/role/search), chi tiết, tạo admin (chỉ `SUPER_ADMIN`, validate mật khẩu mạnh bằng regex), cập nhật từng phần, **xóa mềm** (status → INACTIVE, không cho tự xóa chính mình).
- **UI**: `AdminTable`, `AdminModal` (form với checkbox RBAC từng quyền), `StatsCards` — trên admin-web.
- **Phân quyền UI**: nút "Tạo Admin"/"Xóa" chỉ hiện khi `SUPER_ADMIN`.

---

### 4.3. Xác thực Farm Manager (BICAP-7, EPIC-2)

**Chức năng:** Đăng ký & đăng nhập tài khoản quản lý nông trại.

**Cách triển khai:**

- **API**: `POST /api/auth/farm/register`, `POST /api/auth/farm/login`.
- **Validate đầu vào**: email, số điện thoại Việt Nam `^0[35789]\d{8}$`, mật khẩu mạnh, xác nhận mật khẩu.
- **`AuthService`**: ngăn **nâng cấp vai trò ngầm** (user không thể tự ý thêm role), kiểm tra khóa/treo tài khoản.
- **UI**: `AuthPage` với tab vai trò (Farm Manager/Retailer), `LoginForm`, `RegisterForm`, `PasswordStrengthMeter` (5 luật + thanh tiến độ, chuẩn WAI-ARIA).
- **Session**: lưu `accessToken`/`refreshToken`/`currentUser` trong `localStorage` (`utils/auth.ts`), kiểm tra `isLoggedIn()`.

---

### 4.4. Xác thực Retailer (BICAP-36/37/38)

**Chức năng:** Đăng ký nhà bán lẻ kèm **xác thực email**, hồ sơ cá nhân, hồ sơ kinh doanh (giấy phép).

**Cách triển khai:**

- **Xác thực email**: khi đăng ký, user được tạo với status `PENDING_VERIFICATION`; `VerificationEmailService` gửi email chứa link token. Frontend nhận `?verifyToken=` → gọi `POST /api/auth/retailer/verify-email` → kích hoạt.
- **Refresh token rotation**: `POST /api/auth/retailer/refresh` — đổi refresh token sau khi dùng (bảo mật tốt hơn).
- **Khóa tài khoản**: trường `failed_login_attempts`, `locked_until` (thêm vào `users` qua script `docs/sql/bicap-36-38-schema.sql`).
- **Hồ sơ KYC**: entity `RetailerBusinessProfile` (OneToOne với User) — tên doanh nghiệp, địa chỉ, loại hình (`RETAIL_STORE/WHOLESALE/SUPERMARKET/OTHER`), file giấy phép kinh doanh.
- **Upload file an toàn**: `LocalFileStorageService` — avatar (JPG/PNG ≤ 5MB), giấy phép (PDF/JPG/PNG ≤ 10MB), **whitelist content-type**, **chống path traversal**, lưu dưới `uploads/retailers/{userId}/`.
- **UI**: `RetailerProfilePage` (avatar upload multipart), `RetailerBusinessPage` (upload giấy phép).

---

### 4.5. Thanh toán & Gói dịch vụ (BICAP-78)

**Chức năng:** Farm manager mua gói dịch vụ bằng chuyển khoản; hệ thống tự xác nhận qua webhook ngân hàng.

**Cách triển khai:**

- **Entity**: `ServicePackage` (tên, giá, thời hạn ngày, features JSON), `Subscription` (farm, package, `paymentCode`, ngày bắt đầu/kết thúc, status), `Payment` (method, status, `txRef` unique).
- **Luồng mua gói**:
  1. `POST /api/subscriptions/purchase` → tạo Subscription `PENDING_PAYMENT` + sinh mã `BICAP{id}{6 chữ số}` + thông tin chuyển khoản (tên ngân hàng, số tài khoản, nội dung chuyển tiền).
  2. **Chống mua trùng/đồng thời**: ràng buộc `UNIQUE (farm_id, status)` + bắt `DataIntegrityViolationException`.
  3. Frontend mở `PaymentModal` hiển thị hướng dẫn chuyển khoản, **polling trạng thái mỗi 5 giây** tới `/api/subscriptions/payment-status/{code}`.
- **Webhook Sepay** (`/api/public/sepay/webhook`):
  - Xác thực bằng `Bearer <apiKey>` so khớp tuyệt đối (fail-closed).
  - Kiểm tra **số tài khoản thụ hưởng** đúng.
  - **Idempotent**: khử trùng theo `txRef` (ràng buộc unique trên `payments.tx_ref`).
  - **Định tuyến theo memo**: nội dung `BICAP...` → kích hoạt Subscription (`activateSubscription`, kiểm tra đúng số tiền); nội dung `DEP...` → đánh dấu đơn hàng đã đặt cọc.
  - Giao dịch trùng/lỗi phụ trả về `success:true` + message (gateway không retry vô ích); lỗi thực sự throw → trả non-2xx để gateway retry.
- **Bảo mật**: `SepayConfig` đọc `sepay.*` từ env.

---

### 4.6. Phê duyệt đăng ký nông trại (BICAP-3)

**Chức năng:** Admin xem, phê duyệt hoặc từ chối nông trại đăng ký mới.

**Cách triển khai:**

- **Luồng đăng ký**: Farm Manager đăng ký nông trại (`POST /api/farms/register`) → tạo `Farm` status `PENDING`.
- **API admin**: `FarmApprovalController` (`/api/admin/farms`):
  - Danh sách phân trang (filter theo status/search), kèm thông tin chủ sở hữu + số chứng nhận (**batch-load tránh N+1**).
  - `GET /stats` — đếm PENDING/APPROVED/REJECTED.
  - `PUT /{id}/approve` — phê duyệt.
  - `PUT /{id}/reject` — từ chối (**bắt buộc nhập lý do**), tự gửi **thông báo in-app tiếng Việt** cho chủ nông trại.
- **UI**: `FarmApprovalPage` — 3 tab Pending/Approved/Rejected với số lượng live, modal chi tiết (xem chứng nhận), cảnh báo thiếu giấy tờ, **chống double-click**, hủy request khi đóng modal.

---

### 4.7. Quản lý chi tiết nông trại (BICAP-4)

**Chức năng:** Admin quản lý thông tin chi tiết nông trại: chứng nhận, liên hệ, vị trí.

**Cách triển khai:**

- **API**: `FarmApprovalController` mở rộng:
  - `PUT /{id}/status` — chuyển trạng thái quản lý (APPROVED/SUSPENDED/INACTIVE), hỗ trợ REJECTED→PENDING (gửi lại).
  - `PUT /{id}/notes` — ghi chú admin (tối đa 2000 ký tự).
- **Entity `Farm`** lưu: name (unique), address, area, GPS (lat/lng), description, productTypes (CSV), adminNotes, status.
- **Entity `FarmCertification`**: type (VietGAP/GlobalGAP/Organic…), fileUrl, expiryDate.
- **UI**: `FarmManagementPage` — modal đổi trạng thái, modal ghi chú, **link GPS tới Google Maps**.

---

### 4.8. Thông báo IoT — Nhiệt độ/Độ ẩm/pH (BICAP-26)

**Chức năng:** Nhận dữ liệu cảm biến; cảnh báo khi vượt ngưỡng an toàn; tổng hợp cuối ngày.

**Cách triển khai:**

- **API**: `POST /api/iot/sensors` (`IotDataController`, `@CrossOrigin("*")`) — nhận `{farmId, temperature, humidity, ph}`.
- **Ngưỡng an toàn**: nhiệt độ 15–40°C, độ ẩm 30–90%, pH 5.5–7.5. Vượt ngưỡng → tạo thông báo **URGENT** + **push SSE real-time** + **email** cảnh báo.
- **Job định kỳ**: `@Scheduled` tổng hợp cảnh báo **23:59:59 hàng ngày** (dùng `findByFarmIdAndMeasuredAtBetween`).
- **UI**: `IotDashboard` — 3 thẻ số liệu với dải an toàn, nút **"Simulate IoT data"** (POST dữ liệu ngẫu nhiên để demo), lịch sử cảnh báo đọc từ **SSE**.

---

### 4.9. Quản lý Smart Contract (BICAP-6) — chi tiết ở mục 6

### 4.10. Thông báo real-time (BICAP-77)

**Chức năng:** Thông báo in-app real-time qua SSE; email cho sự kiện quan trọng.

**Cách triển khai:**

- **Entity `Notification`**: user, type (INFO/SUCCESS/WARNING/ALARM), title, content, channel (IN_APP/PUSH/EMAIL), isRead.
- **SSE fan-out theo user**: `GET /api/notifications/stream` (`text/event-stream`) — JWT qua `?token=`, **heartbeat mỗi 25 giây** giữ kết nối sống, hỗ trợ **nhiều tab/thiết bị** cho cùng user (theo dõi nhiều emitter).
- **API**: danh sách thông báo + đếm chưa đọc (`/unread-count`), đánh dấu đã đọc 1 cái hoặc tất cả (`/read-all`), **kiểm tra quyền sở hữu** khi đánh dấu đã đọc.
- **Email** cho sự kiện critical (cảnh báo IoT).
- **UI**: `NotificationBell` — badge chưa đọc, dropdown danh sách, nhận real-time qua SSE, đánh dấu đã đọc.

---

### 4.11. API Quản lý Mùa vụ (BICAP-73)

**Chức năng:** CRUD mùa vụ, cập nhật quy trình canh tác, xuất bán; **ghi dữ liệu lên blockchain**.

**Cách triển khai:**

- **3 Entity mới**: `FarmingSeason` (mùa vụ), `FarmingProcess` (nhật ký canh tác), `Export` (xuất bán) — mỗi entity có cột `txHash` lưu giao dịch VeChain.
- **10 DTOs** với Jakarta Validation (`@NotBlank`, `@NotNull`, `@Size`, `@DecimalMin`), **manual Builder pattern** (không dùng Lombok).
- **3 Service với ràng buộc nghiệp vụ khắt khe**:

  | Service | Ràng buộc |
  |---|---|
  | `SeasonService` | Chỉ tạo mùa vụ khi Farm đã **APPROVED** và có Subscription **ACTIVE**; luồng trạng thái nghiêm ngặt: chỉ `IN_PROGRESS → HARVESTED/CANCELLED` |
  | `ProcessService` | Chỉ thêm quy trình khi mùa vụ đang **IN_PROGRESS** |
  | `ExportService` | Chỉ tạo phiếu xuất khi mùa vụ đã **HARVESTED** |

  Tất cả đều **kiểm tra quyền sở hữu** (ownership) của user hiện tại đối với nông trại.

- **Tích hợp blockchain**: gọi `BlockchainService.recordSeason/recordProcess/recordExport` → trả `txHash` → lưu lại vào bản ghi MySQL (mô hình **Dual Storage**).
- **API endpoints**:

  | Method | Path | Mục đích |
  |---|---|---|
  | POST | `/api/farms/{farmId}/seasons` | Tạo mùa vụ |
  | GET | `/api/farms/{farmId}/seasons` | Danh sách mùa vụ (phân trang, filter status) |
  | GET | `/api/farms/{farmId}/seasons/{seasonId}` | Chi tiết mùa vụ (kèm processes + exports + farmName) |
  | PUT | `/api/farms/{farmId}/seasons/{seasonId}` | Cập nhật thông tin |
  | PATCH | `/api/farms/{farmId}/seasons/{seasonId}/status` | Đổi trạng thái HARVESTED/CANCELLED |
  | POST | `/api/seasons/{seasonId}/processes` | Thêm quy trình canh tác |
  | GET | `/api/seasons/{seasonId}/processes` | Danh sách quy trình |
  | PUT | `/api/seasons/{seasonId}/processes/{processId}` | Cập nhật quy trình |
  | POST | `/api/seasons/{seasonId}/exports` | Tạo phiếu xuất bán |
  | GET | `/api/seasons/{seasonId}/exports` | Danh sách phiếu xuất |

- **Seeder nâng cấp**: tự tạo `ServicePackage` + cấp `Subscription` ACTIVE cho farm mẫu để dev/test vượt được validate.
- **Tài liệu**: `docs/BICAP-73-Farming-Season-Report.md` + Postman collection.

---

## 5. Cơ sở dữ liệu & Cache

### 5.1. Thiết kế database (BICAP-79)

Thiết kế **23 bảng** đầy đủ DDL, kèm chiến lược index và Redis cache trong `docs/bicap-79-database-setup.md`. Các bảng chính:

| Nhóm | Bảng |
|---|---|
| **Bảo mật** | `permissions`, `roles`, `role_permissions`, `users`, `user_roles` |
| **Nông trại** | `farms`, `farm_certifications`, `farming_seasons`, `farming_processes` |
| **Sản phẩm/Đơn hàng** | `products`, `qrcodes`, `orders`, `payments` |
| **Gói dịch vụ** | `service_packages`, `subscriptions` |
| **Vận chuyển** | `vehicles`, `drivers`, `shipments`, `shipment_tracking` |
| **Blockchain** | `blockchain_transactions` |
| **Khác** | `notifications`, `reports`, `iot_data` |

### 5.2. Chiến lược Indexing

Index cho các truy vấn tải cao: `users.phone`, `farms.status`, `farming_seasons(farm_id, status)`, `products(category_id, status)`, `orders(retailer_id, status)`, `shipments(driver_id, status)`, `notifications(user_id, is_read)`, `iot_data(farm_id, measured_at)`.

### 5.3. Redis 8.6 Cache

- Cấu hình: `requirepass`, `maxmemory 512mb` + eviction `allkeys-lru`, AOF.
- **Bảng key pattern**: `session:{token}` (15'), `user:{id}` (30'), `farm:{id}` (1h), `product:list:*` (5'), `notification:{userId}:unread` (1'), `iot:{farmId}:latest` (5'), `rate_limit:*` (1'), `bc:tx:{hash}` (24h).
- **Invalidation**: write-through/DEL key, TTL self-heal, event-driven khi đơn hàng đổi trạng thái.

> ⚠️ Lưu ý: Redis đã được cấu hình trong `application.properties` và tài liệu, **nhưng hiện chưa có CacheManager tiêu thụ trong code** — là phần chuẩn bị cho giai đoạn tối ưu hiệu năng.

### 5.4. Seeder dữ liệu (`DatabaseSeeder`)

`CommandLineRunner` idempotent khởi tạo: 4 quyền, 8 vai trò + mapping, 7 tài khoản demo (`superadmin@`, `admin@`, `moderator@`, `farm@`, `retailer@`…), 4 nông trại mẫu (APPROVED/PENDING/REJECTED) kèm chứng nhận, 1 gói dịch vụ + subscription ACTIVE. **Không bao giờ ghi đè mật khẩu/vai trò đã tồn tại.**

---

## 6. Blockchain & Smart Contract

### 6.1. Nền tảng: VeChainThor

- EVM-compatible, đồng thuận **PoA**, **dual-token VET + VTHO** (gas), chi phí giao dịch thấp — phù hợp doanh nghiệp supply-chain.
- Testnet node: `https://node-testnet.vechain.dev`.

### 6.2. 4 Smart Contract (`blockchain/contracts/Traceability.sol`)

Một file chứa **4 hợp đồng** (Solidity `^0.8.24`, MIT license, OpenZeppelin Upgradeable), tuân thủ chuẩn bảo mật: `AccessControlUpgradeable` + `ReentrancyGuardUpgradeable` + `PausableUpgradeable` + `UUPSUpgradeable`.

**Vai trò on-chain** (lặp lại ở mỗi hợp đồng):

| Role | Quyền |
|---|---|
| `SYSTEM_WRITER_ROLE` | Người duy nhất ghi dữ liệu (backend dùng) |
| `UPGRADER_ROLE` | Cho phép nâng cấp proxy |
| `DEFAULT_ADMIN_ROLE` | Cấp/thu hồi vai trò, `pause()`/`unpause()` |

**1. `FarmingSeasonContract`** — Ghi mùa vụ:
- `SeasonData {farmId, seasonId, seasonName, productType, variety, area (×100), startDate, endDate, status, createdAt}`.
- `createSeason()`, `updateSeasonStatus()` (tự đặt `endDate` khi HARVESTED/CANCELLED), `getSeason()`, `getFarmSeasons()`, `getSeasonCount()`.
- Sự kiện: `SeasonCreated`, `SeasonStatusUpdated`.

**2. `FarmingProcessContract`** — Ghi quy trình canh tác:
- `ProcessData {seasonId, processId, processType, executionDate, materialsHash, imagesHash, createdAt}`.
- **Quan trọng**: dữ liệu lớn (vật tư, hình ảnh JSON) lưu **off-chain**, chỉ đưa **hash** lên chain (`keccak256`) — tiết kiệm gas.
- `addProcess()`, `getProcess()`, `getSeasonProcesses()`, `getProcessCount()`.

**3. `ExportContract`** — Ghi xuất bán + **xác minh QR**:
- `ExportData {seasonId, exportId, quantity (×100), unit, warehouse, qrHash, exportDate, createdAt}`.
- `recordExport()` — **chống trùng `qrHash`** (mỗi QR chỉ dùng 1 lần).
- `verifyQR(qrHash)` — hàm `view` trả về dữ liệu xuất bán (được `TraceabilityContract` gọi).

**4. `TraceabilityContract`** — Hợp đồng truy xuất tổng hợp:
- `TraceData {traceId, seasonId, processIds[], exportId, farmId, farmName, createdAt}` — gom mùa vụ + quy trình + xuất bán + nông trại thành 1 vết truy xuất.
- Giữ tham chiếu tới `ExportContract` (có thể đổi địa chỉ bởi admin).
- `createTrace()`, `verify(qrHash)` — gọi `exportContract.verifyQR()` và phát sự kiện `TraceVerified`.

### 6.3. Tích hợp Backend (`BlockchainService`)

| Thành phần | Vai trò |
|---|---|
| `BlockchainService` | Trừu tượng hóa tương tác chain: `deployContract`, `recordSeason/Process/Export`, `retryTransaction` |
| `BlockchainTransaction` (entity) | Nhật ký mọi ghi lên chain: `entityType` (SEASON/PROCESS/QR/EXPORT/CONTRACT), `txHash` unique, `status`, `retryCount`, **`idempotencyKey` unique** |
| `SmartContract` (entity) | Metadata hợp đồng: name, address, bytecode, abi, environment (TESTNET/MAINNET), status, version, txHash |

**Cơ chế hoạt động — 3 lớp bảo vệ:**

1. **Idempotency**: mỗi ghi chép có `idempotencyKey` (`SEASON_{id}`, `PROCESS_{id}`, `EXPORT_{id}`, `CONTRACT_{id}`) + ràng buộc unique → **chống submit trùng** khi gọi lại.
2. **Retry**: tối đa 3 lần; thành công → `CONFIRMED` + cập nhật `txHash` lên entity; quá 3 lần → `FAILED`.
3. **Chế độ hoạt động** (cấu hình `blockchain.mode`):
   - `mock` (mặc định): sinh `txHash`/`address` giả định (UUID-based), đánh `CONFIRMED` — thuận tiện dev/test.
   - `live`: gọi `{nodeUrl}/blocks/best` kiểm tra node VeChainThor phản hồi (không thành công → `FAILED`).

**Endpoints blockchain:**

| Method | Path | Quyền |
|---|---|---|
| GET | `/api/blockchain/transactions` | Admin view |
| POST | `/api/blockchain/transactions/{id}/retry` | Admin write |
| GET | `/api/admin/contracts` | Admin view |
| POST | `/api/admin/contracts/deploy` | **SUPER_ADMIN only** |

**UI `SmartContractPage`**: 2 tab (danh sách hợp đồng + nhật ký giao dịch blockchain), modal triển khai (name/version/TESTNET-MAINNET/ABI/bytecode), nút retry, link tới `explore.vechain.org`.

---

## 7. Frontend Web

Dự án có **2 ứng dụng React** (cả hai đều là SPA tối giản, **không dùng thư viện router/state/HTTP** — tự viết bằng `fetch` + conditional rendering / `history.pushState`):

### 7.1. Farm Manager / Retailer Portal (`frontend/`, port 5174)

- **Auth** với tab vai trò: Farm Manager (`/api/auth/farm/*`) & Retailer (`/api/auth/retailer/*`), hỗ trợ verify email qua `?verifyToken=`.
- **ServicePackages**: danh mục gói dịch vụ, mua gói (`POST /subscriptions/purchase`), `PaymentModal` (hướng dẫn chuyển khoản, copy clipboard, **poll trạng thái mỗi 5s**).
- **Khóa VIP theo subscription**: menu `products`, `iot`, `certificates` 🔒 chỉ mở khi có subscription ACTIVE (`GET /subscriptions/my`).
- **IotDashboard**: 3 thẻ số liệu + nút simulate + lịch sử cảnh báo real-time (SSE).
- **NotificationBell**: thông báo real-time (SSE), badge chưa đọc.
- **Profile**: `ProfilePage` (Farm Manager), `RetailerProfilePage` (avatar upload), `RetailerBusinessPage` (upload giấy phép).
- **`farms/my`**: lấy nông trại của chính user (không hardcode farm id).

### 7.2. Admin Portal (`admin-web/`, port 5173 / deploy 3001)

- **Landing page** 3 cổng vào (Admin / Farm / Retail), định tuyến thủ công qua `history.pushState`.
- **Admin CRUD**: `AdminTable` + `AdminModal` (phân trang, debounced search, filter role/status, RBAC từng quyền, xóa mềm).
- **FarmApprovalPage** (BICAP-3): 3 tab trạng thái, phê duyệt/từ chối kèm lý do bắt buộc, xem chứng nhận.
- **FarmManagementPage** (BICAP-4): đổi trạng thái, ghi chú admin, GPS link Google Maps.
- **SmartContractPage** (BICAP-6): triển khai & theo dõi smart contract trên VeChainThor.
- **Session/role mapping**: `buildSession()` map roles API → permissions theo cổng (`SUPER_ADMIN` → 4 quyền admin, …). 401 → tự logout; 403 → "Access Denied". Gửi header `X-Actor-Email` cho mọi API admin.

### 7.3. Thiết kế UI chung

- **Dark "glass-panel" design system** qua CSS custom properties (`--primary`, `--glass-blur`, …), font Inter, giao diện **tiếng Việt**.
- Đồng bộ design system giữa 2 app (`index.css`).

---

## 8. CI/CD & Triển khai

### 8.1. GitHub Actions (`.github/workflows/ci.yml`)

Pipeline "Java & Node.js Multi-Service CI/CD" — chạy khi push `main`/`feature/*` và PR vào `main`, có `concurrency` (cancel-in-progress):

| Job | Nội dung |
|---|---|
| `frontend-ci` | admin-web: Node 20, `npm ci`, `npm run lint` (oxlint), `npm run build` |
| `farm-portal-ci` | frontend: Node 20, `npm ci`, `npm run build` |
| `backend-ci` | JDK 21 (Corretto), `mvn clean test`, `mvn package`, upload artifact JAR |
| `docker-build-push` | Chờ 2 job trên; Buildx + QEMU; **push Docker Hub** backend image + admin-web image (tag `latest` trên main, `sha`, branch) |

### 8.2. Docker

- **Backend Dockerfile**: `eclipse-temurin:21-jre`, chạy user **không root** `bicap`, `HEALTHCHECK` qua `/actuator/health`, curl.
- **`docker-compose.db.yml`**: 3 service — `api` (8080), `admin-web` (3001→80, Nginx SPA fallback), `farm-portal` (3002→80). MySQL/Redis local bị comment (dùng cloud DB). Volume `bicap-uploads` cho file upload.

### 8.3. Cấu hình môi trường

- `.env.example` liệt kê toàn bộ biến: DB, JWT (bắt buộc), SMTP (verify email), Sepay API key (bắt buộc), Redis, `VITE_API_BASE_URL`.
- `SecretConfigValidator` **fail-fast**: thiếu `JWT_SECRET`/`SEPAY_API_KEY` → app không khởi động.
- `run-backend.bat` / `run-frontend.bat` tiện ích chạy local (load `.env`, fallback H2).

---

## 9. Kiểm thử

**19 file test** (JUnit 5 + MockMvc + Spring Security Test, H2 in-memory `create-drop`):

| File test | Phạm vi |
|---|---|
| `AuthControllerTest` / `AuthServiceTest` | Đăng ký, đăng nhập, validation, email verification |
| `JwtAuthenticationFilterTest` / `ActorAuthorizerTest` | Filter JWT, chống giả mạo header, RBAC |
| `AdminServiceTest` | CRUD admin, xóa mềm, phân quyền |
| `FarmApprovalServiceTest` | Duyệt/từ chối nông trại + thông báo |
| `BlockchainServiceTest` / `SmartContractControllerTest` | Deploy contract, ghi season/process/export, retry |
| `NotificationControllerTest` / `NotificationServiceTest` | Thông báo, đếm chưa đọc, quyền sở hữu |
| `OrderServiceTest` | Luồng đặt cọc (30%), kiểm tra số tiền |
| `SubscriptionServiceTest` | Mua gói, chống trùng |
| `SepayServiceTest` | Webhook: kích hoạt subscription, khử trùng txRef |
| `RetailerProfileServiceTest` | Hồ sơ KYC, upload, unique phone |
| `UserProfileServiceTest` / `LocalFileStorageServiceTest` | Hồ sơ user, upload an toàn (path traversal) |
| `DatabaseSeederTest` / `SearchUtilsTest` | Seeder, escape LIKE |

---

## 10. Hướng dẫn chạy dự án

### Backend (Spring Boot, port 8080)

```bash
# Tạo .env từ template rồi điền credentials
cp .env.example .env

# Chạy qua IntelliJ IDEA (EnvFile plugin) hoặc:
mvn spring-boot:run
```

> Mặc định backend chạy với **H2 in-memory** (MODE=MySQL) nên không cần DB ngoài. Set `SPRING_DATASOURCE_*` để dùng MySQL cloud.

### Frontend

```bash
# Farm/Retailer portal (port 5174)
cd frontend && npm install && npm run dev

# Admin portal (port 5173, hoặc 3001 để khớp CORS)
cd admin-web && npm install && npm run dev
```

### Docker (production)

```bash
docker-compose -f docker-compose.db.yml up -d --build
# → api (8080), admin-web (3001), farm-portal (3002)
```

### Tài khoản demo (từ seeder)

`superadmin@bicap.com`, `admin@bicap.com`, `moderator@bicap.com`, `farm@bicap.com`, `retailer@bicap.com`…

---

## 11. Tổng kết

### 11.1. Đã hoàn thành ✅

| Nhóm | Tính năng |
|---|---|
| **Xác thực & phân quyền** | JWT + RBAC (8 vai trò, 4 quyền admin), đăng ký/đăng nhập Farm Manager & Retailer, xác thực email retailer, refresh token rotation, khóa tài khoản sau 5 lần sai, rate limiting, fail-fast secret |
| **Quản trị** | CRUD admin, xóa mềm, quản lý nông trại (duyệt/từ chối, chứng nhận, ghi chú, GPS, trạng thái) |
| **Nghiệp vụ nông nghiệp** | Mùa vụ (tạo/cập nhật/trạng thái), quy trình canh tác, xuất bán — đều được băm ghi lên blockchain |
| **Thanh toán** | Gói dịch vụ, subscription, đặt cọc đơn hàng 30%, webhook Sepay an toàn (apiKey, idempotent, định tuyến theo memo) |
| **Blockchain** | 4 smart contract UUPS chuẩn bảo mật OZ; BlockchainService mock/live + idempotency + retry; quản lý & triển khai contract trên admin UI |
| **Thông báo** | In-app real-time **SSE** (heartbeat, multi-tab), email, cảnh báo IoT theo ngưỡng, tổng hợp cuối ngày |
| **IoT** | API nhận dữ liệu cảm biến, dashboard simulate |
| **Hạ tầng** | CI/CD 4 job, Docker + Compose, 19 test, 10 tài liệu kỹ thuật |

### 11.2. Còn thiếu / đang phát triển 🔜

- **Vận chuyển (Shipping)**: entities `vehicles`/`drivers`/`shipments`/`shipment_tracking` đã thiết kế nhưng **chưa có API/UI**.
- **Sàn giao dịch (Trading floor)**: bảng `products`/`qrcodes` đã thiết kế, chưa có CRUD sản phẩm & đặt hàng đầy đủ (hiện mới có luồng đặt cọc).
- **Truy xuất QR cho khách hàng**: `TraceabilityContract.verify()` đã có trên chain, **nhưng chưa có REST endpoint** (`/api/trace/**` đã mở permitAll trong security, chưa có controller) và chưa sinh QR thật.
- **Mobile app** (Ship Driver / Guest) và **Guest web app** — chưa phát triển.
- **Live VeChainThor**: hiện `blockchain.mode=mock` (mặc định); chế độ `live` mới chỉ health-check node, chưa ký & gửi giao dịch thật (chưa có SDK web3j/VeChain trong `pom.xml`, chưa có script deploy, ABI/bytecode phải dán tay vào form admin).
- **Redis**: cấu hình sẵn sàng nhưng chưa có CacheManager tiêu thụ trong code.
- **File export (CSV/Excel)** và **QR generation** từ backend.

### 11.3. Đánh giá chất lượng code

- **Không dùng Lombok** → POJO tường minh, builder pattern thủ công (dễ đọc, dễ debug, nhưng verbose hơn).
- **An toàn**: BCrypt, fail-fast secret, chống path traversal upload, chống giả mạo `X-Actor-Email`, idempotency chống trùng giao dịch thanh toán/blockchain, rate limit chống brute-force, escape LIKE chống search injection.
- **Hiệu năng**: batch-load chống N+1, repository query động, index thiết kế cho truy vấn tải cao.
- **Sạch sẽ**: exception handler toàn cục chuẩn hóa, 19 test bao phủ các service/controller then chốt.

---

*Báo cáo được tổng hợp từ toàn bộ source code, lịch sử 70 commits, và 10 tài liệu kỹ thuật trong `docs/`. Thời điểm tổng kết: 12/08/2026.*
