# BÁO CÁO TỔNG KẾT DỰ ÁN

## TÍCH HỢP BLOCKCHAIN TRONG SẢN XUẤT NÔNG SẢN SẠCH (BICAP)

| Thông tin | Chi tiết |
|---|---|
| **Tên dự án (EN)** | Blockchain Integration in Clean Agricultural Production |
| **Tên dự án (VN)** | Tích hợp Blockchain trong sản xuất nông sản sạch |
| **Viết tắt** | BICAP |
| **Loại tài liệu** | Báo cáo tổng kết toàn bộ quá trình phát triển |
| **Thời gian phát triển** | 22/07/2026 → 12/08/2026 |
| **Số commit** | 144 commits trên nhánh `main` (5 thành viên) |

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
| **Ship Driver** | Mobile App (đã gỡ khỏi repo; backend vẫn giữ API `/api/driver/**`) | Cập nhật quy trình giao hàng, quét QR |
| **Guest** | Web/Mobile | Xem sản phẩm, nội dung giáo dục |

---

## 2. Công nghệ & Kiến trúc hệ thống

### 2.1. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| **Backend** | Java 21, Spring Boot 3.3.0, Spring Security, Spring Data JPA, Spring Validation, Spring Data Redis/Cache, BouncyCastle, Actuator |
| **Xác thực** | JWT (jjwt 0.12.5), HS256, BCrypt, RBAC (`@EnableMethodSecurity` + `@PreAuthorize`) |
| **Database** | MySQL 5.7.41 remote (dev + production); H2 in-memory chỉ trong test/CI (`ALLOW_SIMULATION=true`) |
| **Cache** | Redis 8.6 — **bắt buộc** (không kết nối được ⇒ backend dừng khởi động; không còn fallback in-memory, chỉ tắt bằng `APP_CACHE_ENABLED=false` cho test/CI) |
| **Frontend** | React 19 + Vite 8 + TypeScript 6 (**1 ứng dụng web duy nhất** `web/`: portal ở `/` + admin ở `/admin`) |
| **Blockchain** | VeChainThor (EVM-compatible), Solidity ^0.8.24, OpenZeppelin Upgradeable |
| **Thanh toán** | Cổng Sepay (webhook bank transfer) |
| **CI/CD** | GitHub Actions (2 job: `web-ci`, `backend-ci`) — không dùng Docker/Nginx |

### 2.2. Kiến trúc tổng thể

Hệ thống theo **kiến trúc 3 tầng** (Presentation / Application / Data), với mô hình **Monolith** cho backend (phù hợp đội nhỏ / MVP — ADR-001).

```
┌─────────────────────────────────────────────────────────────────┐
│  Presentation Layer                                             │
│   ├── Web App         (web/, React Vite: / portal + /admin)     │
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
│   ├── MySQL (dữ liệu vận hành - operational data, remote)       │
│   ├── Redis (cache, bắt buộc)                                   │
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

- **Backend**: 247 file Java (main) + 45 file Java (test), ~18.800 dòng Java (main); 45 test class, `mvn test` = **345 test pass** (chạy không cần MySQL/Redis nhờ H2 `create-drop` + `app.cache.enabled=false` + `ALLOW_SIMULATION=true` trong test resources).
- **Web**: **1 ứng dụng React TS duy nhất** (70 file `.ts`/`.tsx`, ~16.300 dòng) — 45 file trong `web/src/portal`, 21 file trong `web/src/admin`; 22 file test, `npm test` = **84 test**.
- **Blockchain**: 4 smart contract trong 1 file Solidity (`dev/blockchain/contracts/Traceability.sol`, ~19 KB).
- **Docs**: 15 tài liệu `.md` + 6 script SQL trong `docs/sql/`.
- **Tổng**: 144 commit, 5 thành viên.

---

## 3. Tổng quan các giai đoạn phát triển

Dự án phát triển qua **144 commits** theo các ticket Jira, chia thành 12 giai đoạn:

| # | Giai đoạn | Ticket | Thời gian | Kết quả chính |
|---|---|---|---|---|
| 1 | Khởi tạo & tài liệu | BICAP-91/92/93, EPIC-8/9 | 22–24/07 | User Requirements, SRS, Architecture Design, CI/CD setup |
| 2 | Core Security & RBAC | BICAP-72 | 25/07 | Spring Security + JWT + RBAC cốt lõi |
| 3 | Quản trị Admin | BICAP-1 | 25–26/07 | Admin CRUD + phân quyền, UI admin |
| 4 | Xác thực Farm Manager | BICAP-7 | 26/07–06/08 | Đăng ký/đăng nhập farm manager |
| 5 | Xác thực Retailer | BICAP-36/37/38 | 28/07–05/08 | Đăng ký được **kích hoạt ngay** (không cần xác thực email), hồ sơ KYC |
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
- **JWT Token Provider** (`JwtTokenProvider`): thuật toán HS256, secret **bắt buộc** nạp từ env ở mọi profile (`SecureSecretInitializer` từ chối secret thiếu/placeholder, không còn secret sinh tạm), token **có kiểu** (`type` claim):
  - `access` — mặc định 24 giờ.
  - `access` của retailer — 15 phút (chặt hơn).
  - `refresh` — 7 ngày (rotation).
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
- **UI**: `AdminTable`, `AdminModal` (form với checkbox RBAC từng quyền), `StatsCards` — trong `web/src/admin`.
- **Phân quyền UI**: nút "Tạo Admin"/"Xóa" chỉ hiện khi `SUPER_ADMIN`.

---

### 4.3. Xác thực Farm Manager (BICAP-7, EPIC-2)

**Chức năng:** Đăng ký & đăng nhập tài khoản quản lý nông trại.

**Cách triển khai:**

- **API**: `POST /api/auth/farm/register`, `POST /api/auth/farm/login`.
- **Validate đầu vào**: email, số điện thoại Việt Nam `^0[35789]\d{8}$`, mật khẩu mạnh, xác nhận mật khẩu.
- **`AuthService`**: ngăn **nâng cấp vai trò ngầm** (user không thể tự ý thêm role), kiểm tra khóa/treo tài khoản.
- **UI**: `AuthPage` với tab vai trò (Farm Manager/Retailer), `LoginForm`, `RegisterForm`, `PasswordStrengthMeter` (5 luật + thanh tiến độ, chuẩn WAI-ARIA).
- **Session**: lưu `accessToken`/`refreshToken`/`currentUser` trong `localStorage` (`web/src/shared/session.ts` — dùng chung cho cả portal và admin), kiểm tra `isLoggedIn()`.

---

### 4.4. Xác thực Retailer (BICAP-36/37/38)

**Chức năng:** Đăng ký nhà bán lẻ (kích hoạt ngay), hồ sơ cá nhân, hồ sơ kinh doanh (giấy phép).

**Cách triển khai:**

- **Kích hoạt ngay**: `POST /api/auth/retailer/register` tạo user với status `ACTIVE` và trả token dùng được ngay — **không còn xác thực email**: đã xoá `VerificationEmailService`, endpoint `POST /api/auth/retailer/verify-email`, trường `verificationRequired` và token type `email_verification`. Toàn bộ email/SMTP cũng đã bị xoá khỏi backend (không còn `spring-boot-starter-mail`).
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
- **Ngưỡng an toàn**: nhiệt độ 15–40°C, độ ẩm 30–90%, pH 5.5–7.5. Vượt ngưỡng → tạo thông báo **URGENT** + **push SSE real-time** (chỉ in-app, không còn email cảnh báo).
- **Job định kỳ**: `@Scheduled` tổng hợp cảnh báo **23:59:59 hàng ngày** (dùng `findByFarmIdAndMeasuredAtBetween`).
- **UI**: `IotDashboard` — 3 thẻ số liệu với dải an toàn, nút **"Simulate IoT data"** (POST dữ liệu ngẫu nhiên để demo), lịch sử cảnh báo đọc từ **SSE**.

---

### 4.9. Quản lý Smart Contract (BICAP-6) — chi tiết ở mục 6

### 4.10. Thông báo real-time (BICAP-77)

**Chức năng:** Thông báo in-app real-time qua SSE (không còn kênh email).

**Cách triển khai:**

- **Entity `Notification`**: user, type (INFO/SUCCESS/WARNING/ALARM), title, content, channel (`IN_APP` — enum email đã bị xoá), isRead.
- **SSE fan-out theo user**: `GET /api/notifications/stream` (`text/event-stream`) — JWT qua `?token=`, **heartbeat mỗi 25 giây** giữ kết nối sống, hỗ trợ **nhiều tab/thiết bị** cho cùng user (theo dõi nhiều emitter). Ngay khi mở stream, server gửi frame `event: connected` để trình duyệt biết kết nối đã sống (không phải chờ nhịp heartbeat đầu tiên).
- **Client ngắt kết nối (reload/đóng tab)**: lần ghi kế tiếp vào emitter đã chết sẽ ném `IOException`; emitter được `complete()` + xoá khỏi registry ngay nhịp lỗi đầu tiên, và `GlobalExceptionHandler` **không** log ERROR/cố ghi body cho response SSE đã commit (trước đây sinh ERROR + stack trace + `HttpMessageNotWritableException` mỗi 25 giây).
- **Dispatch nội bộ của container**: `SecurityConfig` cho phép `DispatcherType.ASYNC` **và** `DispatcherType.ERROR` (+ đường dẫn `/error`) đi qua — cả hai lần dispatch này do container tạo, không có `SecurityContext`, nên nếu để `anyRequest().authenticated()` chặn sẽ sinh `AccessDeniedException: Access Denied` + *"Unable to handle the Spring Security Exception because the response is already committed"* lặp mỗi 25 giây. Client không tạo được dispatch loại này ⇒ không nới lỏng bảo mật (`SseAsyncDispatchSecurityTest` khoá hành vi).
- **API**: danh sách thông báo + đếm chưa đọc (`/unread-count`), đánh dấu đã đọc 1 cái hoặc tất cả (`/read-all`), **kiểm tra quyền sở hữu** khi đánh dấu đã đọc.
- **Không còn email**: broadcast chỉ tạo thông báo in-app + SSE cho người nhận (đã bỏ tham số `sendEmail`).
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

> ⚠️ Lưu ý: Redis giờ là **dependency bắt buộc** của backend — `RedisCacheConfig` tạo `RedisCacheManager` và **dừng khởi động** nếu không ping được Redis (`APP_CACHE_ENABLED=true` mặc định, không còn fallback in-memory ngầm). Chỉ test/CI mới đặt `APP_CACHE_ENABLED=false` để dùng `ConcurrentMapCacheManager`.

### 5.4. Seeder dữ liệu (`DatabaseSeeder`)

`CommandLineRunner` idempotent khởi tạo: 4 quyền, 8 vai trò + mapping, 7 tài khoản demo (`superadmin@`, `admin@`, `moderator@`, `farm@`, `retailer@`…), 4 nông trại mẫu (APPROVED/PENDING/REJECTED) kèm chứng nhận, 1 gói dịch vụ + subscription ACTIVE. **Không bao giờ ghi đè mật khẩu/vai trò đã tồn tại.**

---

## 6. Blockchain & Smart Contract

### 6.1. Nền tảng: VeChainThor

- EVM-compatible, đồng thuận **PoA**, **dual-token VET + VTHO** (gas), chi phí giao dịch thấp — phù hợp doanh nghiệp supply-chain.
- Testnet node: `https://node-testnet.vechain.dev`.

### 6.2. 4 Smart Contract (`dev/blockchain/contracts/Traceability.sol`)

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
   - `live` (**mặc định và bắt buộc cho dev/production thật**): giao dịch type-0 được RLP-encode, ký secp256k1 (RFC 6979, low-s) rồi `POST /transactions` lên node VeChainThor; kết quả xác nhận qua receipt. Cần `BLOCKCHAIN_PRIVATE_KEY` (ví signer có VTHO).
   - `mock`: sinh `txHash`/`address` giả định (UUID-based), đánh `CONFIRMED` — **chỉ chấp nhận khi `ALLOW_SIMULATION=true`** (test/CI), tương tự `BLOCKCHAIN_EXPORT_MODE=local`; `SecretConfigValidator` chặn khởi động nếu dùng ngoài test/CI.

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

Dự án có **1 ứng dụng web React duy nhất** trong `web/` (React 19 + TypeScript + Vite), gộp hai giao diện trước đây (`frontend/` farm portal và `admin-web/` admin) vào cùng một codebase. App là **SPA tối giản**, **không dùng thư viện router/state/HTTP** — tự viết bằng `fetch` + conditional rendering / `history.pushState`.

### 7.1. Web App gộp (`web/`, dev port 5174)

- **Router theo endpoint** (`web/src/App.tsx`): mọi đường dẫn bắt đầu bằng `/admin` → `AdminApp`; các đường dẫn còn lại → `PortalApp`. Dev chạy `cd web && npm run dev` → `http://localhost:5174/` (portal) và `http://localhost:5174/admin` (admin).
- **Cấu trúc mã nguồn**: `web/src/portal/**` (chuyển từ `frontend/src/**`), `web/src/admin/**` (chuyển từ `admin-web/src/**`), `web/src/shared/session.ts` (session + API base dùng chung), `web/src/index.css` (design system gộp).
- **Session dùng chung**: một cơ chế duy nhất trong `web/src/shared/session.ts` — lưu `accessToken` / `currentUser` / `refreshToken` trong `localStorage` cho cả portal và admin; đã **bỏ cơ chế chuyển token qua redirect `?token=`**.
- **Phía portal (`/`)**:
  - **Auth** với tab vai trò: Farm Manager (`/api/auth/farm/*`) & Retailer (`/api/auth/retailer/*`); đăng ký Retailer được kích hoạt ngay (không còn luồng verify email `?verifyToken=`).
  - **ServicePackages**: danh mục gói dịch vụ, mua gói (`POST /subscriptions/purchase`), `PaymentModal` (hướng dẫn chuyển khoản, copy clipboard, **poll trạng thái mỗi 5s**).
  - **Khóa VIP theo subscription**: menu `products`, `iot`, `certificates` 🔒 chỉ mở khi có subscription ACTIVE (`GET /subscriptions/my`).
  - **IotDashboard**: 3 thẻ số liệu + nút simulate + lịch sử cảnh báo real-time (SSE).
  - **NotificationBell**: thông báo real-time (SSE), badge chưa đọc.
  - **Profile**: `ProfilePage` (Farm Manager), `RetailerProfilePage` (avatar upload), `RetailerBusinessPage` (upload giấy phép).
  - **`farms/my`**: lấy nông trại của chính user (không hardcode farm id).
- **Phía admin (`/admin`)**:
  - **Landing page** 3 cổng vào (Admin / Farm / Retail), định tuyến thủ công qua `history.pushState`.
  - **Admin CRUD**: `AdminTable` + `AdminModal` (phân trang, debounced search, filter role/status, RBAC từng quyền, xóa mềm).
  - **FarmApprovalPage** (BICAP-3): 3 tab trạng thái, phê duyệt/từ chối kèm lý do bắt buộc, xem chứng nhận.
  - **FarmManagementPage** (BICAP-4): đổi trạng thái, ghi chú admin, GPS link Google Maps.
  - **SmartContractPage** (BICAP-6): triển khai & theo dõi smart contract trên VeChainThor.
  - **Session/role mapping**: `buildSession()` map roles API → permissions theo cổng (`SUPER_ADMIN` → 4 quyền admin, …). 401 → tự logout; 403 → "Access Denied". Gửi header `X-Actor-Email` cho mọi API admin.

### 7.2. Thiết kế UI chung

- **Dark "glass-panel" design system** qua CSS custom properties (`--primary`, `--glass-blur`, …), font Inter, giao diện **tiếng Việt**.
- Design system dùng chung cho cả portal và admin trong 1 file `web/src/index.css`.

---

## 8. CI/CD & Triển khai

### 8.1. GitHub Actions (`.github/workflows/ci.yml`)

Pipeline "Java & Node.js Multi-Service CI/CD" — chạy khi push `main`/`feature/*` và PR vào `main`, có `concurrency` (cancel-in-progress). **Chỉ còn 2 job:**

| Job | Nội dung |
|---|---|
| `web-ci` | `web/`: Node 22 (≥22.22.2 — jsdom 30/undici 8 bỏ hỗ trợ Node 20), `npm ci` → `npm run lint` (oxlint) → `npm test` → `npm run build` |
| `backend-ci` | `backend/`: JDK 21 (Corretto), `mvn clean test` (345 test) → `mvn package -DskipTests`, upload artifact JAR |

### 8.2. Triển khai

- **Chạy 1 port (khuyến nghị)**: build web (`cd web && npm run build` → `web/dist`), copy toàn bộ `web/dist/*` vào `backend/src/main/resources/static/`, rồi chạy backend (`cd backend && mvn spring-boot:run`). Backend phục vụ tĩnh cả hai endpoint trên cùng cổng 8080: `http://localhost:8080/` (portal) và `http://localhost:8080/admin` (admin).
- **Dev tách rời**: backend 8080 (bắt buộc MySQL remote + Redis remote + `BLOCKCHAIN_MODE=live` trong `.env`; thiếu là dừng khởi động, không còn H2/mock/in-memory mặc định), web dev server 5174.
- **Docker đã được gỡ hoàn toàn khỏi repo**: không còn `Dockerfile`, `docker-compose.db.yml`, `web/Dockerfile`, `web/nginx.conf` và job `docker-build-push` trong CI. Triển khai thực hiện trực tiếp bằng Maven/npm như trên.

### 8.3. Cấu hình môi trường

- `.env.example` liệt kê toàn bộ biến: MySQL remote, Redis remote, JWT (bắt buộc ở mọi profile), Sepay API key (bắt buộc), blockchain `live` + `ALLOW_SIMULATION=false`, `VITE_API_BASE_URL` — **không còn `SMTP_*`/`MAIL_FROM`**.
- `.env` (đã `.gitignore`) được backend **tự nạp** qua `spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]`; script `dev/run-backend.ps1` nạp `.env` rồi chạy `mvn spring-boot:run`.
- `SecureSecretInitializer`/`SecretConfigValidator` **fail-fast**: thiếu/placeholder `JWT_SECRET` hoặc `SEPAY_API_KEY`, thiếu MySQL remote, Redis không tới được, hoặc blockchain `mock`/`local` ngoài test/CI → app không khởi động. Không còn secret sinh tạm cho dev.

---

## 9. Kiểm thử

**Backend: 45 test class / 345 test pass** (JUnit 5 + MockMvc + Spring Security Test, H2 in-memory `create-drop` khai báo riêng trong `src/test/resources/application.properties` cùng `app.cache.enabled=false` + `app.allow-simulation=true`, nên chạy được **không cần** MySQL/Redis); **Web: 22 file test / 84 test** (`npm test`):

| File test | Phạm vi |
|---|---|
| `AuthControllerTest` / `AuthServiceTest` | Đăng ký, đăng nhập, validation, kích hoạt ngay tài khoản retailer (không còn email verification) |
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
# Tạo .env từ template rồi điền credentials THẬT (MySQL remote + Redis remote + ví VeChain có VTHO + Sepay key)
cp .env.example .env

# Chạy backend — backend tự nạp .env; thiếu hạ tầng/key là dừng khởi động
cd backend && mvn spring-boot:run
# hoặc từ thư mục gốc:  .\dev\run-backend.ps1

# Test: mvn test (345 test — không cần MySQL/Redis)
# Package: mvn clean package -DskipTests → backend/target/*.jar
```

> Không còn fallback: backend bắt buộc **MySQL remote + Redis remote + blockchain `live`** và secret thật (`JWT_SECRET`, `SEPAY_API_KEY`) ở **mọi profile**. H2 + mock blockchain + local stub chỉ chạy khi `ALLOW_SIMULATION=true` (test/CI); Redis không tới được cũng dừng khởi động (chỉ tắt cache bằng `APP_CACHE_ENABLED=false` khi test/CI).

### Web (React + Vite, dev port 5174)

```bash
cd web && npm install && npm run dev
# → http://localhost:5174/       (portal: Farm Manager / Retailer)
# → http://localhost:5174/admin  (admin)
```

Các lệnh khác trong `web/`: `npm test` (84 test / 22 file), `npm run lint` (oxlint), `npm run build` → `web/dist`.

### Triển khai 1 port (không dùng Docker)

```bash
cd web && npm run build
cp -r web/dist/* ../backend/src/main/resources/static/
cd ../backend && mvn spring-boot:run
# → http://localhost:8080/  (portal) và http://localhost:8080/admin  (admin)
```

### Tài khoản demo (từ seeder)

`superadmin@bicap.com`, `admin@bicap.com`, `moderator@bicap.com`, `farm@bicap.com`, `retailer@bicap.com`…

---

## 11. Tổng kết

### 11.1. Đã hoàn thành ✅

| Nhóm | Tính năng |
|---|---|
| **Xác thực & phân quyền** | JWT + RBAC (8 vai trò, 4 quyền admin), đăng ký/đăng nhập Farm Manager & Retailer (retailer **kích hoạt ngay**, không cần xác thực email), refresh token rotation, khóa tài khoản sau 5 lần sai, rate limiting, fail-fast secret |
| **Quản trị** | CRUD admin, xóa mềm, quản lý nông trại (duyệt/từ chối, chứng nhận, ghi chú, GPS, trạng thái) |
| **Nghiệp vụ nông nghiệp** | Mùa vụ (tạo/cập nhật/trạng thái), quy trình canh tác, xuất bán — đều được băm ghi lên blockchain |
| **Thanh toán** | Gói dịch vụ, subscription, đặt cọc đơn hàng 30%, webhook Sepay an toàn (apiKey, idempotent, định tuyến theo memo) |
| **Blockchain** | 4 smart contract UUPS chuẩn bảo mật OZ; BlockchainService mock/live + idempotency + retry; quản lý & triển khai contract trên admin UI |
| **Thông báo** | In-app real-time **SSE** (heartbeat, multi-tab), cảnh báo IoT theo ngưỡng, tổng hợp cuối ngày — **không còn email/SMTP** |
| **IoT** | API nhận dữ liệu cảm biến, dashboard simulate |
| **Hạ tầng** | CI/CD **2 job** (`web-ci`, `backend-ci`), **không dùng Docker**; triển khai 1 port bằng cách build web → `static/` → chạy backend; **345 test backend + 84 test web**; 15 tài liệu kỹ thuật + 6 script SQL. App mobile tài xế đã gỡ khỏi repo (backend vẫn giữ `DriverMobileController` + `/api/driver/**`) |

### 11.2. Còn thiếu / đang phát triển 🔜

- **Vận chuyển (Shipping)**: entities `vehicles`/`drivers`/`shipments`/`shipment_tracking` đã thiết kế nhưng **chưa có API/UI**.
- **Sàn giao dịch (Trading floor)**: bảng `products`/`qrcodes` đã thiết kế, chưa có CRUD sản phẩm & đặt hàng đầy đủ (hiện mới có luồng đặt cọc).
- **Truy xuất QR cho khách hàng**: `TraceabilityContract.verify()` đã có trên chain, **nhưng chưa có REST endpoint** (`/api/trace/**` đã mở permitAll trong security, chưa có controller) và chưa sinh QR thật.
- **Guest web app** — chưa phát triển.
- **App mobile tài xế**: chưa triển khai trong bản này — đã **gỡ khỏi repo** (`mobile-app/` không còn), trong khi backend **vẫn giữ** `DriverMobileController` + API `/api/driver/**` để sẵn sàng khi làm lại UI mobile.
- **Live VeChainThor**: `blockchain.mode=live` là **mặc định/bắt buộc** cho dev & production và **đã ký + broadcast giao dịch thật** (RLP-encode type-0, secp256k1 qua BouncyCastle rồi `POST /transactions`, xác nhận qua receipt); `mock`/`local` chỉ chấp nhận khi `ALLOW_SIMULATION=true` (test/CI). Chưa có script deploy tự động — ABI/bytecode vẫn phải dán tay vào form admin.
- **Redis**: là **dependency bắt buộc** — `RedisCacheConfig` tạo `RedisCacheManager` và chặn khởi động nếu không kết nối được (không còn fallback in-memory).
- **File export (CSV/Excel)** và **QR generation** từ backend.

### 11.3. Đánh giá chất lượng code

- **Không dùng Lombok** → POJO tường minh, builder pattern thủ công (dễ đọc, dễ debug, nhưng verbose hơn).
- **An toàn**: BCrypt, fail-fast secret, chống path traversal upload, chống giả mạo `X-Actor-Email`, idempotency chống trùng giao dịch thanh toán/blockchain, rate limit chống brute-force, escape LIKE chống search injection.
- **Hiệu năng**: batch-load chống N+1, repository query động, index thiết kế cho truy vấn tải cao.
- **Sạch sẽ**: exception handler toàn cục chuẩn hóa, 345 test backend + 84 test web bao phủ các service/controller then chốt.

---

*Báo cáo được tổng hợp từ toàn bộ source code, lịch sử 144 commits (5 thành viên), và 15 tài liệu kỹ thuật trong `docs/`. Thời điểm tổng kết: 12/08/2026.*
