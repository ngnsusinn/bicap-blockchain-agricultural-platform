# Tài liệu Triển khai Hệ thống (System Implementation Document)

| Thông tin | Chi tiết |
|---|---|
| **Dự án** | BICAP — Blockchain Integration in Clean Agricultural Production |
| **Mã ticket** | BICAP-95 |
| **Phiên bản tài liệu** | 1.0 — 30/08/2026 |
| **Phạm vi** | Mô tả quá trình triển khai hệ thống: công nghệ, kiến trúc thực tế, cấu hình, quy trình phát triển & đóng gói |

---

## 1. Tổng quan triển khai

BICAP được triển khai theo mô hình **monorepo** gồm 4 thành phần:

| Thành phần | Thư mục | Công nghệ | Vai trò |
|---|---|---|---|
| Backend API | `src/` | Java 21, Spring Boot 3.3 | REST API, nghiệp vụ, tích hợp blockchain |
| Farm/Retailer Portal | `frontend/` | React 19, TypeScript, Vite 8 | Cổng Farm Manager, Retailer, trang Guest, trang chủ đăng nhập 3 vai trò |
| Admin Web | `admin-web/` | React 19, TypeScript, Vite 8 | Bảng điều khiển quản trị (RBAC, duyệt farm, giám sát sản phẩm, smart contract, báo cáo) |
| Smart Contracts | `blockchain/contracts/` | Solidity 0.8.24, OpenZeppelin Upgradeable | 4 hợp đồng: FarmingSeason, FarmingProcess, Export, Traceability (UUPS, AccessControl, Pausable, ReentrancyGuard) |

## 2. Kiến trúc kỹ thuật thực tế

### 2.1 Backend
- **Kiến trúc lớp**: Controller → Service → Repository (Spring Data JPA), DTO riêng cho từng luồng (record cho request bất biến, class POJO cho response — không dùng Lombok).
- **Bảo mật** (BICAP-72/80/81): Spring Security 6 + JWT (HS256, base64 ≥32 bytes, fail-fast qua `SecretConfigValidator`), RBAC theo vai trò (`ActorAuthorizer`, `CurrentUser`), `RateLimitFilter` (30 req/phút/IP cho `/api/auth/**`), CSP headers, BCrypt password, khóa tài khoản sau nhiều lần đăng nhập sai (`LoginAttemptService`).
- **Realtime**: SSE (`/api/notifications/stream`) cho thông báo in-app (BICAP-77).
- **Cache** (BICAP-79): `RedisCacheConfig` — tự chọn Redis khi ping được, tự fallback in-memory khi Redis vắng mặt (không chặn khởi động). `@Cacheable` cho danh mục & chi tiết sản phẩm sàn; `@CacheEvict` khi ghi.
- **Blockchain** (BICAP-6/74/80/81): 2 chế độ qua `BLOCKCHAIN_MODE`:
  - `mock`: hash mô phỏng, xác nhận tức thì (dev/CI).
  - `live`: giao dịch type-0 được **RLP-encode, ký secp256k1 (RFC 6979, canonical low-s, recovery id), broadcast qua REST node VeChainThor** (`POST /transactions`), nhận diện kết quả qua receipt. Gói `common/blockchain`: `RlpEncoder`, `VeChainTxSigner`, `VeChainClient`, `VeChainWallet`, `Hashes` (blake2b-256 + keccak-256).
  - **Tự phục hồi** (BICAP-80): `BlockchainMaintenanceJob` chạy nền — 15s xác nhận PENDING từ receipt, 60s auto-retry FAILED (tối đa 3 lần), hết hạn PENDING kẹt >30 phút.
- **Thanh toán** (BICAP-78): cổng Sepay — webhook đối chiếu chữ ký, mã đặt cọc `paymentCode`, tự kích hoạt subscription/order.
- **Định tuyến SPA** (chế độ 1 port): `SpaForwardController` forward `/`, `/trace/**` → Farm Portal; `/admin/**` → Admin Web (Vite `base=/admin/`).

### 2.2 Dữ liệu
- **MySQL 5.7.41** (production, `DDL_AUTO=validate`) / **H2 MODE=MySQL** (dev/test, `update`).
- Schema tham chiếu: `docs/bicap-79-database-setup.md` + `docs/sql/*.sql` (migration thủ công theo ticket).
- **Redis 8.6**: cache tầng application (TTL 60s mặc định, cấu hình `APP_CACHE_TTL_SECONDS`).

### 2.3 Frontend
- SPA không router thư viện — điều hướng bằng state + History API; session trong `localStorage`.
- Trang chủ (farm portal) có 3 tab đăng nhập Farm/Retailer/**Admin** + nút **điền nhanh tài khoản test**; đăng nhập ADMIN tự chuyển hướng sang `/admin/?token=…` — Admin Web đổi token qua `/api/profile` và vào thẳng dashboard (SSO một port).
- Gọi API bằng `fetch` + `VITE_API_BASE_URL` (mặc định `http://localhost:8080`).

## 3. Cấu hình triển khai (environment variables)

Toàn bộ qua `.env` (xem `.env.example`):

| Nhóm | Biến | Ghi chú |
|---|---|---|
| Server | `SERVER_PORT` | mặc định 8080 |
| Database | `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `DDL_AUTO` | production: MySQL + `validate` |
| Redis | `SPRING_REDIS_HOST/PORT/PASSWORD/SSL`, `APP_CACHE_ENABLED`, `APP_CACHE_TTL_SECONDS` | thiếu Redis → tự fallback in-memory |
| JWT | `JWT_SECRET` (bắt buộc), `JWT_EXPIRATION_MS`, `REFRESH_TOKEN_EXPIRATION_MS`, `VERIFICATION_TOKEN_EXPIRATION_MS` | fail-fast nếu yếu/placeholder |
| Blockchain | `BLOCKCHAIN_MODE` (`mock`/`live`), `BLOCKCHAIN_NODE_URL`, `BLOCKCHAIN_PRIVATE_KEY` (bắt buộc khi live), `BLOCKCHAIN_GAS_PRICE/GAS_ATTEST/GAS_DEPLOY/EXPIRATION`, `BLOCKCHAIN_CONFIRM_INTERVAL_MS/RETRY_INTERVAL_MS` | ví signer cần VTHO trả phí gas |
| Thanh toán | `SEPAY_ACCOUNT_NO/BANK_NAME/API_KEY` | fail-fast nếu placeholder |
| Email | `SMTP_*`, `MAIL_FROM`, `FRONTEND_URL` | xác minh email retailer |
| Upload | `UPLOAD_DIR` | mặc định `uploads/`, phục vụ tại `/uploads/**` |
| Frontend | `VITE_API_BASE_URL`, `VITE_ADMIN_PORTAL_URL` | build-time |

## 4. Quy trình phát triển & đóng gói

### 4.1 Branching & CI
- Nhánh tính năng `feature/BICAP-xx-*` → PR → merge `main`. CI (`.github/workflows/ci.yml`): lint+build 2 frontend, `mvn clean test` + package backend, build & push Docker image.

### 4.2 Đóng gói một port (khuyến nghị demo/test)
```bat
build-web.bat      :: build 2 React app → src/main/resources/static/ (+ /admin)
run-backend.bat    :: mvn spring-boot:run  → http://localhost:8080
```
JAR chứa sẵn 2 SPA: Farm Portal tại `/`, Admin Web tại `/admin/`.

### 4.3 Đóng gói container
```bash
mvn package -DskipTests
docker build -t bicap-api .
docker-compose -f docker-compose.db.yml up -d --build
```
`Dockerfile`: JRE 21, user không đặc quyền `bicap`, HEALTHCHECK `/actuator/health`.

## 5. Kiểm soát chất lượng đã áp dụng
- 241 test backend (unit + integration + security + crypto vectors).
- 28 test frontend (vitest + Testing Library).
- Load test `loadtest/` (k6 + node runner) — kết quả trong `docs/testing-document.md`.
- Seed dữ liệu nhất quán (`DatabaseSeeder` — không ghi đè tài khoản đã tồn tại).
- Tài khoản test chuẩn (bảng trong `docs/run-guide.md` §4).

## 6. Nhật ký triển khai theo mốc
| Mốc | Nội dung | Tham chiếu |
|---|---|---|
| Tuần 1 | CI, auth/RBAC nền, admin CRUD | BICAP-1/72, PR #1–#5 |
| Tuần 2 | Farm approval, IoT, subscription/Sepay, smart contract mgmt | BICAP-3/4/5/6/26/78 |
| Tuần 3 | Season API + blockchain anchoring, notifications SSE | BICAP-73/77 |
| Tuần 4 | Marketplace, đơn hàng lifecycle, retailer auth | BICAP-18/39–46/75 |
| Tuần 5 | Shipping API, driver endpoints, hoàn thiện farm portal | BICAP-76 + các bổ sung |
| Tuần 6 | Reports dùng chung, single-port, Redis/cache, VeChain live, kiểm thử & tài liệu | BICAP-27/79/80/86–98 |
