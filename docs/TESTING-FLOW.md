# 🧪 Kế Hoạch Kiểm Thử Toàn Diện — BICAP Blockchain Agricultural Platform

| Thông tin | Chi tiết |
|---|---|
| **Dự án** | BICAP — Blockchain Integration in Clean Agricultural Production |
| **Tài liệu gốc tham khảo** | `docs/testing-document.md`, `docs/uat-plan.md`, `docs/system-specification.md` |
| **Phiên bản flow** | 1.0 — 16/09/2026 |
| **Môi trường thực thi** | JDK 21 · Maven 3.9+ · Node 22+ · H2 (test) · MySQL + Redis (integration/e2e) |
| **Mục tiêu** | Kiểm thử toàn bộ tính năng quan trọng theo luồng nghiệp vụ thực tế, từ registration đến blockchain trace |

---

## Tổng quan kiến trúc kiểm thử

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    TẦNG 1 — UNIT TEST (nhanh, cô lập)                   │
│  mvn test :: npm test                                                   │
│  Service/Controller/Security/Blockchain/Utils                            │
├─────────────────────────────────────────────────────────────────────────┤
│                    TẦNG 2 — INTEGRATION TEST (module)                    │
│  @SpringBootTest + MockMvc (full filter chain + H2 seeder)               │
│  CrossRole / Lifecycle / Security Regression                             │
├─────────────────────────────────────────────────────────────────────────┤
│                    TẦNG 3 — E2E / BẮT BUỘC (multi-role)                  │
│  HTTP thật qua server đang chạy                                          │
│  FullBusinessFlow :: CrossRoleMatrix :: Load/Stress                      │
├─────────────────────────────────────────────────────────────────────────┤
│                    TẦNG 4 — UAT THỦ CÔNG (theo vai trò)                  │
│  Checklist thủ công trên bản 1-port                                      │
│  Farm → Retailer → Admin → Guest → Shipping                              │
├─────────────────────────────────────────────────────────────────────────┤
│                    TẦNG 5 — BLOCKCHAIN LIVE SMOKE                        │
│  Giao dịch thật trên VeChainThor testnet                                 │
│  TX broadcast → receipt → trace → ledger confirm                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## PHẦN 1: CHUẨN BỊ

### 1.1. Yêu cầu môi trường

```bash
# === BACKEND (test/CI — không cần MySQL/Redis) ===
cd backend
# application-test.properties dùng H2 in-memory + mock blockchain

# === BACKEND (integration/e2e — cần MySQL + Redis đang chạy) ===
# Copy .env từ .env.example, điền key thật
Copy-Item .env.example .env
# SPRING_DATASOURCE_URL, JWT_SECRET, SEPAY_API_KEY, SPRING_REDIS_HOST,
# BLOCKCHAIN_PRIVATE_KEY (nếu test live), BLOCKCHAIN_NODE_URL

# === FRONTEND ===
cd web
npm install
```

### 1.2. Chạy test đơn vị trước

```bash
# Backend unit + integration (H2, mock blockchain)
cd backend && mvn test
# Kết quả mong đợi: ~345 test pass / 0 fail

# Frontend unit + component
cd web && npm test
# Kết quả mong đợi: ~84 test pass / 0 fail
```

### 1.3. Khởi động server cho integration/e2e

```bash
# Option A — Dev mode (2 tiến trình)
# Terminal 1:
cd backend && mvn spring-boot:run
# Terminal 2:
cd web && npm run dev

# Option B — 1-port (production-like)
cd web && npm install && npm run build
rm -rf ../backend/src/main/resources/static
mkdir -p ../backend/src/main/resources/static
cp -r dist/* ../backend/src/main/resources/static/
cd ../backend && mvn spring-boot:run
```

---

## PHẦN 2: TẤM 1 — UNIT TEST (Tự động hóa)

> **Mục tiêu:** Xác minh từng lớp riêng lẻ — service, controller, security, blockchain.
> **Thực thi:** `mvn test`, `npm test`. Nhanh (giây), cô lập, không cần server.

### 2.1. Auth & RBAC (34 test)

| Test class | Kiểm tra |
|---|---|
| `AuthServiceTest` | Đăng ký (fARM/retailer), đăng nhập, khóa tài khoản sau 5 lần sai, refresh token |
| `AuthControllerTest` | Mọi endpoint login/register — status code, validation, role gating |
| `JwtAuthenticationFilterTest` | Bearer token hợp lệ/không hợp lệ, X-Actor-Email giả mạo, token expired |
| `ActorAuthorizerTest` | requireSuperAdmin / requireAdminWrite / requireAdminView phân quyền chính xác |
| `RateLimitFilterTest` | 30 req/phút, biên 30, xoay X-Forwarded-For |

```bash
cd backend && mvn -Dtest='AuthServiceTest,AuthControllerTest,JwtAuthenticationFilterTest,ActorAuthorizerTest,RateLimitFilterTest' test
```

### 2.2. Admin (6 test)

| Test class | Kiểm tra |
|---|---|
| `AdminServiceTest` | CRUD admin, soft-delete, không tự xóa mình, SUPER_ADMIN-only, gán permission |

### 2.3. Farm Approval (22 test)

| Test class | Kiểm tra |
|---|---|
| `FarmApprovalServiceTest` | Duyệt/từ chối + lý do bắt buộc, batch load, ghi chú, status transition, PENDING bị chặn dùng endpoint khác |

### 2.4. Farm Self-Service (7 test)

| Test class | Kiểm tra |
|---|---|
| `FarmRegistrationServiceTest` | Cập nhật hồ sơ, trùng tên, upload chứng nhận, sở hữu chéo, role check |

### 2.5. Season & Export (5+ test)

| Test class | Kiểm tra |
|---|---|
| `SeasonExportServiceTest` | Harvest → export, idempotency, đơn vị khớp, `chainMode` (live/mock) |

### 2.6. Trading Floor & Product (26+ test)

| Test class | Kiểm tra |
|---|---|
| `TradingFloorServiceTest` | Điều kiện export READY, 1–10 ảnh, quyền farm |
| `ProductServiceTest`, `ProductRepositoryIntegrationTest` | Lọc/phân trang, stats, đổi trạng thái, guard danh mục |

### 2.7. Order (40+ test)

| Test class | Kiểm tra |
|---|---|
| `OrderServiceTest`, `OrderManagementServiceTest`, `FarmManagerOrderServiceTest` | Vòng đời PENDING→ACCEPTED→DEPOSIT_PAID→SHIPPING→IN_TRANSIT→DELIVERED→COMPLETED, hủy, đặt cọc, auto-cancel scheduler |

### 2.8. Shipping (6+ test)

| Test class | Kiểm tra |
|---|---|
| `FarmShipmentServiceTest` | Quyền farm, đếm trạng thái, tỷ lệ đúng hạn |

### 2.9. Report (9 test)

| Test class | Kiểm tra |
|---|---|
| `ReportServiceTest` | Gửi mọi role, admin-only xử lý, thông báo 2 chiều |

### 2.10. Notification (20 test)

| Test class | Kiểm tra |
|---|---|
| `NotificationServiceTest`, `NotificationControllerTest`, `SseNotificationStreamTest` | unread count, đánh dấu đọc, SSE frame, emitter chết |

### 2.11. Blockchain (22 test)

| Test class | Kiểm tra |
|---|---|
| `BlockchainServiceTest`, `VeChainCryptoTest`, `BlockchainSecurityTest` | RLP vectors, RFC6979 deterministic, low-s, recovery id, địa chỉ ví/contract, RBAC tx |
| `VeChainTxSignerTest` | 9-field signing, blockRef from blockId, broadcast format |
| `VeChainClientTest` | Ánh xạ 404/pending/confirmed/error |

### 2.12. Payment & Subscription (19 test)

| Test class | Kiểm tra |
|---|---|
| `SepayServiceTest`, `SubscriptionServiceTest` | Webhook chữ ký, khớp mã trong văn bản tự do, kích hoạt gói, auto-complete |

### 2.13. Security & Guest Regression (15 test)

| Test class | Kiểm tra |
|---|---|
| `SecurityAndGuestRegressionIntegrationTest` | C-1/C-3/C-4: secret cũ bị từ chối, admin products không truy cập ẩn danh, guest chỉ thấy thông báo hệ thống, IoT chặn sai vai trò |
| `SecureSecretInitializerTest` | 7 test: chặn secret public/template, sinh secret dev, fail-fast ở prod |

### 2.14. Frontend Unit (84 test)

| Test file | Kiểm tra |
|---|---|
| `session.test.ts`, `auth.test.ts`, `useHashTab.test.ts` | Session, auth logic, hash tab routing |
| `AdminTable.test.tsx`, `DashboardPage.test.tsx`, `StatusBadge.test.tsx` | Admin components rendering |
| `LoginForm.test.tsx`, `PortalSidebar.test.tsx` | Portal components |
| `GuestArea.test.tsx`, `GuestProductSearch.test.tsx`, `GuestEducation.test.tsx` | Guest pages |
| `DriverShipmentsPage.test.tsx`, `TrackingPage.test.tsx`, `SeasonExports.test.tsx` | Driver/Shipping/Farm pages |
| `retailerPages.test.tsx`, `ShippingNotificationsPage.test.tsx` | Retailer/Shipping notifications |
| `api.test.ts`, `api.test.ts` | API helpers |

```bash
cd web && npm test
```

---

## PHẦN 3: TẦM 2 — INTEGRATION TEST (Multi-module)

> **Mục tiêu:** Kiểm thử luồng liên kết nhiều module trong cùng một Spring context.
> **Thực thi:** `mvn -Dtest=... test` trên H2 + seeder + full security filter chain.

### 3.1. Cross-Role Matrix (30 test)

**Đây là bộ test quan trọng nhất** — kiểm tra mọi chức năng từ mọi vai trò, xác nhận:
- Người dùng đúng vai trò → 200
- Người dùng sai vai trò hoặc không phải chủ sở hữu → 403
- Không có escalation quyền ngầm (ví dụ farm đăng nhập với vai trò retail không truy cập admin)

```bash
cd backend && mvn -Dtest=CrossRoleMatrixIntegrationTest test
```

**Các nhóm chức năng được quét:**

| Nhóm | Kiểm tra |
|---|---|
| Đăng nhập portal | đúng portal (200) · sai portal farm→retail, retail→admin (401) |
| X-Actor-Email | JWT farm/retail/driver + header admin → 401; admin thiếu header → 400 |
| Farm self-service | owner 200 · farm khác tenant 403 · retailer/driver/shipping/admin 403 · guest 401 |
| Duyệt farm | admin-view 200 · farm/retail 403 · moderator ghi 403 |
| Mùa vụ / quy trình / xuất kho | owner 200 · farm khác 403 · admin chỉ đọc export |
| Sàn giao dịch + giám sát sản phẩm | farm sở hữu 201/200 · farm khác 403 · retailer đọc danh sách 403 |
| Đơn hàng | farm list 200 · retailer my 200 · accept/reject chỉ farm sở hữu · deposit/cancel/complete chỉ retailer sở hữu |
| Vận chuyển | mọi `/api/shipping/**` chỉ SHIPPING_MGR |
| Tài xế | `/api/driver/**` chỉ SHIP_DRIVER · QR sai/thiếu → 400 |
| Marketplace | retailer 200 · farm/driver/guest 403 · catalogue công khai 200 |
| IoT | chủ farm 200 · farm khác 403 · admin bắc cầu 200 |
| Báo cáo | mọi vai trò gửi 201 · moderator/admin đọc 200 · chỉ admin xử lý |
| Thông báo | guest chỉ hệ thống 200 · broadcast chỉ SHIPPING_MGR |
| Gói dịch vụ / thuê bao | danh sách công khai 200 · tạo gói chỉ admin · mua gói chỉ chủ farm |
| Blockchain / smart contract | ledger & contract: admin/moderator 200, farm/retail 403 · deploy: superadmin-only |
| Tài khoản admin | liệt kê: admin/superadmin/moderator 200 · tạo: chỉ superadmin |
| Đối tác bán lẻ | `/api/retailers` chỉ FARM_MANAGER · `/api/retailer/**` chỉ RETAILER |

### 3.2. Full Lifecycle Integration (12 bước)

```bash
cd backend && mvn -Dtest=FullLifecycleIntegrationTest test
```

| # | Bước | Kết quả mong đợi |
|---|---|---|
| 1 | Farm đăng ký + tạo farm | 201, status PENDING |
| 2 | Admin duyệt farm | 200, APPROVED |
| 2b | Mua & kích hoạt gói | subscription ACTIVE |
| 3 | Tạo mùa vụ + quy trình | 201, txHash `0x…` |
| 3b | Harvest kèm sản lượng | 200, harvestedQuantity |
| 4 | Xuất kho | traceHash + QR cấp |
| 5 | Đẩy sản phẩm lên sàn → admin duyệt | PENDING_REVIEW → ACTIVE |
| 6 | Retailer tìm kiếm → đặt hàng | keyword khớp 1 kết quả, đơn PENDING |
| 7 | Farm chấp nhận + đặt cọc | ACCEPTED → DEPOSIT_PAID |
| 8 | Shipping tạo lô (xe + tài xế) | 201 |
| 9 | Driver pickup → tracking → deliver | trạng thái dịch chuyển đúng |
| 10 | Retailer xác nhận hoàn tất | COMPLETED |
| 11 | Farm gửi báo cáo → admin xử lý | OPEN → RESOLVED |
| 12 | Notification tới các bên | unreadCount > 0 |

### 3.3. Security & Guest Regression Integration

```bash
cd backend && mvn -Dtest=SecurityAndGuestRegressionIntegrationTest test
```

| Finding | Test | Nội dung |
|---|---|---|
| C-1 | `SecureSecretInitializerTest` | Từ chối secret mặc định cũ, template; sinh secret dev; fail-fast prod |
| C-1 | `forgedTokenSignedWithShippedDefaultSecret_isRejected` | JWT ký bằng secret cũ → 4xx |
| C-2 | `SepayServiceTest` | Nội dung chuyển khoản chứa mã ở giữa vẫn khớp |
| C-3 | `anonymous*` | `/api/admin/products/**` không truy cập ẩn danh |
| C-3 | `guestFeed_containsOnlySystemAnnouncements` | Guest chỉ thấy thông báo hệ thống |
| C-4 | `iot_*` | Retailer/Driver bị 403; chủ farm hợp lệ 200; ngoài dải → 400 |

### 3.4. Error & Edge Case Integration

| Test class | Kiểm tra |
|---|---|
| `GlobalExceptionHandlerTest` | Client ngắt SSE, lỗi thật vẫn 500 |
| `SseAsyncDispatchSecurityTest` | SSE không bị AuthorizationFilter chặn ở dispatch ASYNC |
| `DatabaseSeederTest` | 4 farm seed + không trùng tên (idempotent) |
| `SeasonExportServiceTest` | live/mock chainMode |

```bash
cd backend && mvn -Dtest='GlobalExceptionHandlerTest,SseAsyncDispatchSecurityTest,DatabaseSeederTest' test
```

---

## PHẦN 4: TẦM 3 — E2E / BẮT BUỘC (HTTP thật trên server đang chạy)

> **Mục tiêu:** Mô phỏng người dùng thật qua HTTP, xác nhận toàn bộ luồng nghiệp vụ xuyên suốt.
> **Thực thi:** `node dev/tests/cross-role-matrix.mjs` — cần backend đang chạy trên `:8080`.

### 4.1. Cross-Role Matrix HTTP (122 probe)

```bash
node dev/tests/cross-role-matrix.mjs
# Kết quả mong đợi: 122 probe pass / 0 fail
```

**Fixture dựng qua API** theo đúng chuỗi nghiệp vụ:
```
farm đăng ký → admin duyệt → mua gói → mùa vụ → thu hoạch → xuất kho → đẩy sàn → 
admin duyệt → retailer đặt → farm chấp nhận → shipping tạo lô
```
→ Bước dựng dữ liệu cũng là 1 ca kiểm thử tích hợp.

### 4.2. Manual HTTP Smoke (chạy bằng curl)

Thực hiện trên terminal với backend đang chạy:

```bash
BASE=http://localhost:8080

# 1. Đăng ký Farm
curl -X POST $BASE/api/auth/farm/register \
  -H "Content-Type: application/json" \
  -d '{"identifier":"newfarm@test.com","password":"Farmpassword@2026","fullName":"New Farm"}'

# 2. Đăng nhập Farm → lấy accessToken
curl -X POST $BASE/api/auth/farm/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"newfarm@test.com","password":"Farmpassword@2026"}'
# → lưu accessToken

# 3. Farm đăng ký nông trại
curl -X POST $BASE/api/farms/register \
  -H "Authorization: Bearer $accessToken" \
  -F "name=Farm New" \
  -F "address=Hanoi" \
  -F "area=100"

# 4. Admin duyệt farm (dùng superadmin)
curl -X PUT $BASE/api/admin/farms/1/approve \
  -H "Authorization: Bearer $adminToken" \
  -H "X-Actor-Email: superadmin@bicap.com"

# 5. Farm tạo mùa vụ
curl -X POST $BASE/api/farms/1/seasons \
  -H "Authorization: Bearer $farmToken" \
  -H "Content-Type: application/json" \
  -d '{"name":"Vụ Xuân 2026","productType":"Rau","variety":"Xanh","area=50}'

# 6. ... (tiếp tục full lifecycle)
```

---

## PHẦN 5: TẦM 4 — LOAD & STRESS TEST

> **Mục tiêu:** Xác nhận hệ thống chịu tải tốt, p95 < 800ms, error < 1%.
> **Thực thi:** `k6 run dev/loadtest/k6-loadtest.js` hoặc `node dev/loadtest/node-loadtest.mjs`.

### 5.1. Load — 20 VUs × 200 req/pha

```bash
k6 run dev/loadtest/k6-loadtest.js
# HOẶC
node dev/loadtest/node-loadtest.mjs --vus 20 --requests 200
```

| Kịch bản | Req | Lỗi | TB | p50 | p95 | p99 |
|---|---|---|---|---|---|---|
| GET /api/categories | 200 | 0 | 41.5ms | 37.0 | 80.2 | 110.2 |
| GET /api/marketplace/products | 200 | 0 | 74.3ms | 50.3 | 189.3 | 356.3 |
| GET /api/service-packages | 200 | 0 | 27.2ms | 25.4 | 44.6 | 104.5 |

### 5.2. Stress — 100 VUs × 500 req/pha

```bash
node dev/loadtest/node-loadtest.mjs --vus 100 --requests 500
# HOẶC
k6 run dev/loadtest/k6-loadtest.js -e VUS=100 -e RPS=500
```

| Kịch bản | Req | Lỗi | TB | p95 | p99 |
|---|---|---|---|---|---|
| categories (cache) | 500 | 0 | 133.4ms | 239.9 | 248.8 |
| marketplace search | 500 | 0 | 152.0ms | 495.9 | 641.3 |
| service-packages | 500 | 0 | 81.8ms | 111.7 | 127.5 |
| **Tổng** | **1500** | **0 (0%)** | — | — | **~625 req/s** |

**Exit criteria:** p95 < 800ms, error rate 0%.

---

## PHẦN 6: TẦM 5 — BLOCKCHAIN LIVE SMOKE TEST

> **Mục tiêu:** Xác nhận giao dịch broadcast thật lên VeChainThor testnet hoạt động.
> **Điều kiện:** `BLOCKCHAIN_MODE=live`, ví signer đã nạp VTHO.

### 6.1. Cấu hình live

```bash
# .env
BLOCKCHAIN_MODE=live
BLOCKCHAIN_EXPORT_MODE=vechain
BLOCKCHAIN_PRIVATE_KEY=<32-byte-hex-private-key>
BLOCKCHAIN_NODE_URL=https://testnet.vechain.org
```

### 6.2. Live Smoke Test (3 giao dịch thật)

```bash
# Khởi động backend với .env
cd backend && mvn spring-boot:run

# 1. Tạo mùa vụ → ghi nhận txHash
# 2. Xuất kho → nhận traceHash + QR
# 3. Kiểm tra ledger qua API
curl $BASE/api/admin/contracts/blockchain-status
curl $BASE/api/blockchain/transactions
```

**Kết quả mong đợi:**

| Entity | Trạng thái | Tx hash | Block | Ledger |
|---|---|---|---|---|
| SEASON | CONFIRMED | `0x9b74…` | 25909484 | CONFIRMED |
| PROCESS | CONFIRMED | `0xa888…` | 25909484 | CONFIRMED |
| SEASON_EXPORT | CONFIRMED | `0x2de4…` | 25909484 | CONFIRMED |

**Kiểm tra trên explorer:** `https://explore-testnet.vechain.org/transactions/<txHash>`

### 6.3. Blockchain Regression Test

```bash
cd backend && mvn -Dtest='VeChainCryptoTest,VeChainClientTest,BlockchainSecurityTest' test
```

| Test | Kiểm tra |
|---|---|
| `signType0_matchesOfficialSdkGoldenVector` | Raw bytes khớp SDK chính thức |
| `blockRefFromBlockId` | 8-byte block ID, không phải số block |
| `getTransactionStatus` meta.blockID mapping | PENDING/CONFIRMED/FAILED đúng |

---

## PHẦN 7: UAT THỦ CÔNG — THEO VAI TRÒ

> **Mục tiêu:** Kiểm tra thủ công toàn bộ luồng từ góc nhìn từng vai trò trên bản 1-port.
> **Thực thi:** Trình duyệt → `http://localhost:8080/` (portal) hoặc `http://localhost:8080/admin`

### Tài khoản test

| Vai trò | Email | Mật khẩu | Đăng nhập tại |
|---|---|---|---|
| Super Admin | `superadmin@bicap.com` | `Superadmin@2026` | `/admin` |
| Admin | `admin@bicap.com` | `Adminpassword@2026` | `/admin` |
| Moderator | `moderator@bicap.com` | `Moderator@2026` | `/admin` |
| Farm Manager | `farm@bicap.com` | `Farmpassword@2026` | `/` |
| Retailer | `retailer@bicap.com` | `Retailpassword@2026` | `/` |
| Shipping Manager | `shipping_mgr@bicap.com` | `Shipping@2026` | `/` |

### 7.1. Nhóm A — Farm Manager (8 ca)

| ID | Ca kiểm thử | Các bước | Kết quả mong đợi |
|---|---|---|---|
| UAT-A1 | Đăng ký & đăng nhập | Đăng ký farm mới → đăng nhập | Vào Farm Portal, sidebar đầy đủ |
| UAT-A2 | Tạo farm | Nông Trại Của Tôi → điền hồ sơ + tải GPKD | Farm PENDING, chứng nhận hiển thị |
| UAT-A3 | Mua gói | Gói Dịch Vụ → chọn gói → thanh toán | Sau webhook, tính năng 🔒 mở khóa |
| UAT-A4 | Mùa vụ + blockchain | Tạo mùa vụ → thêm 2 bước quy trình | Mỗi bước có txHash `0x…` |
| UAT-A5 | Harvest có sản lượng | Chuyển HARVESTED → nhập sản lượng | Bắt buộc nhập; season HARVESTED |
| UAT-A6 | Xuất kho + QR | Xuất kho → mở link trace | QR hiển thị, trang trace công khai đúng dữ liệu |
| UAT-A7 | Đẩy lên sàn | Sàn Giao Dịch → form + ảnh | Sản phẩm PENDING_REVIEW trong "Sản Phẩm Đã Đăng" |
| UAT-A8 | Xử lý đơn & báo cáo | Accept đơn của retailer; gửi báo cáo cho admin | Trạng thái đổi; nhận phản hồi admin qua 🔔 |

### 7.2. Nhóm B — Retailer (6 ca)

| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-B1 | Đăng ký + xác nhận email | Login được sau verify |
| UAT-B2 | Tìm kiếm/lọc sàn | Thấy sản phẩm ACTIVE của farm đã duyệt |
| UAT-B3 | Quét QR / trace | Toàn bộ mùa vụ–quy trình–chứng nhận |
| UAT-B4 | Đặt mua + đặt cọc | Đơn PENDING → ACCEPTED → DEPOSIT_PAID |
| UAT-B5 | Hủy đơn kèm lý do | Farm nhận thông báo |
| UAT-B6 | Xác nhận nhận + tải ảnh | Đơn COMPLETED |

### 7.3. Nhóm C — Admin (7 ca)

| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-C1 | Login admin từ trang chủ | Vào **thẳng** Dashboard Admin (SSO) |
| UAT-C2 | Dashboard số liệu | Khớp dữ liệu thực (farms chờ, sản phẩm, báo cáo) |
| UAT-C3 | Duyệt/từ chối farm | Từ chối phải có lý do; farm nhận thông báo |
| UAT-C4 | Quản lý farm | Đổi status, ghi chú, xem lịch sử mùa vụ |
| UAT-C5 | Duyệt sản phẩm | PENDING_REVIEW → ACTIVE; ẩn sản phẩm vi phạm |
| UAT-C6 | Quản trị admin | Tạo MODERATOR; thử xóa chính mình → bị chặn |
| UAT-C7 | Xử lý báo cáo | RESOLVED + phản hồi; người gửi nhận 🔔 |

### 7.4. Nhóm D — Guest (3 ca)

| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-D1 | Thông báo chung | Xem được không cần login |
| UAT-D2 | Tìm kiếm sản phẩm | Kết quả chỉ sản phẩm ACTIVE |
| UAT-D3 | Trang trace | Truy cập mở theo `/trace/<hash>` |

### 7.5. Nhóm E — Vận hành & phi chức năng (5 ca)

| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-E1 | Đăng nhập sai 5 lần | Tài khoản khóa tạm, thông báo rõ |
| UAT-E2 | F5 mọi deep-link (/, /admin/, /admin/farm, /trace/x) | Không 404 |
| UAT-E3 | Logout ở 1 cổng | Thoát cả 2 cổng (chung origin) |
| UAT-E4 | Chạy load test | p95 < 800ms, error 0% |
| UAT-E5 | Blockchain live smoke | Tx PENDING → CONFIRMED trong ~1 phút |

---

## PHẦN 8: MA TRẬN KIỂM THỬ TỔNG HỢP

### 8.1. Phủ sóng theo module

| # | Module | Unit Test | Integration Test | E2E Probe | UAT Ca |
|---|---|---|---|---|---|
| 1 | Auth & RBAC | 34 | CrossRole (login) | Đăng nhập portal | UAT-E1 |
| 2 | Admin CRUD | 6 | CrossRole (admin) | Admin endpoints | UAT-C1…C6 |
| 3 | Farm Approval | 22 | CrossRole (farm) | Duyệt farm | UAT-A2, UAT-C3 |
| 4 | Farm Self-Service | 7 | CrossRole (farm) | Đăng ký, hồ sơ | UAT-A1 |
| 5 | Season & Process | 5 | FullLifecycle | Mùa vụ, quy trình | UAT-A4, UAT-A5 |
| 6 | Export & QR | 5 | FullLifecycle | Xuất kho, QR | UAT-A6 |
| 7 | Trading Floor | 8 | CrossRole | Đăng bán, sàn | UAT-A7 |
| 8 | Product | 18 | CrossRole | Danh mục, sản phẩm | UAT-D2 |
| 9 | Order | 40 | FullLifecycle | Đặt mua, cọc | UAT-B4, UAT-A8 |
| 10 | Shipping | 6 | CrossRole | Lô, driver, xe | — |
| 11 | Driver Mobile | — | CrossRole | QR, pickup, tracking | — |
| 12 | Marketplace | — | CrossRole | Tìm kiếm | UAT-B2 |
| 13 | IoT | — | CrossRole | Cảm biến | — |
| 14 | Report | 9 | CrossRole | Gửi, xử lý | UAT-A8, UAT-C7 |
| 15 | Notification | 20 | CrossRole | SSE, broadcast | UAT-E2 |
| 16 | Payment (Sepay) | 19 | CrossRole | Webhook | UAT-A3 |
| 17 | Subscription | 19 | CrossRole | Gói dịch vụ | UAT-A3 |
| 18 | Blockchain | 22 | CrossRole | Live TX | UAT-E5 |
| 19 | Smart Contract | — | CrossRole | Deploy, status | — |
| 20 | Guest | — | SecurityRegression | Catalogue, education, trace | UAT-D1…D3 |
| 21 | Profile | — | CrossRole | Hồ sơ, đổi mật khẩu | — |
| 22 | Frontend | 84 | — | — | UAT-A1…E5 |

### 8.2. Phủ sóng theo vai trò

| Vai trò | Unit/Integration | E2E | UAT |
|---|---|---|---|
| SUPER_ADMIN | Admin CRUD, blockchain deploy | Tất cả admin endpoints | UAT-C1…C6 |
| ADMIN | Admin CRUD, farm approval | Tất cả admin endpoints | UAT-C3…C7 |
| MODERATOR | Đọc admin, blockchain read | Đọc admin endpoints | — |
| FARM_MANAGER | Mùa vụ, quy trình, export, order accept | Farm self-service | UAT-A1…A8 |
| RETAILER | Order, payment, marketplace | Retailer endpoints | UAT-B1…B6 |
| SHIPPING_MGR | Shipment, vehicle, driver, broadcast | Shipping endpoints | — |
| SHIP_DRIVER | Driver API (QR, tracking, pickup, deliver) | Driver endpoints | — |
| GUEST | Public catalog, education, trace | Guest endpoints | UAT-D1…D3 |

---

## PHẦN 9: CHẠY TỔNG HỢP — TỐT NHẤT

### 9.1. Script chạy toàn bộ kiểm thử

```bash
#!/bin/bash
# run-all-tests.sh — Chạy toàn bộ kiểm thử BICAP
set -e

echo "═══════════════════════════════════════════════════════════════"
echo "  BICAP — Complete Test Suite"
echo "═══════════════════════════════════════════════════════════════"

# ── TẦNG 1: UNIT TEST ──
echo ""
echo "【1/5】Unit Tests — Backend..."
cd backend && mvn test -q
echo "✅ Backend unit tests passed"

echo ""
echo "【2/5】Unit Tests — Frontend..."
cd ../web && npm test -- --run -q
echo "✅ Frontend unit tests passed"

# ── TẦNG 2: INTEGRATION TEST ──
echo ""
echo "【3/5】Integration Tests..."
cd ../backend

echo "  ├─ CrossRoleMatrix..."
mvn test -Dtest=CrossRoleMatrixIntegrationTest -q

echo "  ├─ FullLifecycle..."
mvn test -Dtest=FullLifecycleIntegrationTest -q

echo "  ├─ SecurityRegression..."
mvn test -Dtest=SecurityAndGuestRegressionIntegrationTest -q

echo "  ├─ Blockchain..."
mvn test -Dtest='VeChainCryptoTest,VeChainClientTest,BlockchainSecurityTest' -q

echo "  ├─ ErrorHandling..."
mvn test -Dtest='GlobalExceptionHandlerTest,SseAsyncDispatchSecurityTest' -q

echo "✅ All integration tests passed"

# ── TẦNG 3: LOAD TEST (cần server đang chạy) ──
echo ""
echo "【4/5】Load/Stress Test (yêu cầu backend đang chạy trên :8080)..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    node ../dev/loadtest/node-loadtest.mjs --vus 100 --requests 500
    echo "✅ Load test passed"
else
    echo "⚠️  Backend không chạy trên :8080 — bỏ qua load test"
fi

# ── TẦNG 4: CROSS-ROLE HTTP PROBE ──
echo ""
echo "【5/5】Cross-Role HTTP Probe (yêu cầu backend đang chạy)..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    node ../dev/tests/cross-role-matrix.mjs
    echo "✅ Cross-role probe passed"
else
    echo "⚠️  Backend không chạy trên :8080 — bỏ qua cross-role probe"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ TẤT CẢ KIỂM THỬ HOÀN TẤT"
echo "═══════════════════════════════════════════════════════════════"
```

### 9.2. Thứ tự kiểm thử khuyến nghị (nếu chạy thủ công)

```
1. mvn test                              ← Unit tests (nhanh, phát hiện lỗi sớm)
2. npm test                              ← Frontend unit tests
3. mvn -Dtest=CrossRoleMatrixIntegrationTest test    ← RBAC (phát hiện lỗi bảo mật sớm)
4. mvn -Dtest=FullLifecycleIntegrationTest test      ← Full business flow
5. mvn -Dtest=SecurityAndGuestRegressionIntegrationTest test  ← Security regression
6. Khởi động server + node cross-role-matrix.mjs       ← E2E probe HTTP
7. k6 run dev/loadtest/k6-loadtest.js                  ← Load/Stress
8. UAT thủ công (theo nhóm A→B→C→D→E)                  ← User acceptance
9. Blockchain live smoke (nếu có testnet key)           ← On-chain verification
```

---

## PHẦN 10: BẢNG KIỂM TRA TÍNH NĂNG QUAN TRỌNG (Checklist)

> Dùng checklist này để xác nhận từng tính năng quan trọng hoạt động đúng sau khi triển khai.

### 10.1. Bảo mật & Xác thực

- [ ] Đăng ký FARM_MANAGER + RETAILER hoạt động
- [ ] Đăng nhập đúng portal → 200, sai portal → 401
- [ ] 5 lần đăng nhập sai → tài khoản khóa 30 phút
- [ ] JWT hết hạn → refresh hoạt động (RETAILER)
- [ ] JWT giả bằng secret cũ → 403
- [ ] X-Actor-Email không khớp JWT → 401
- [ ] `X-Actor-Email` không gửi cho endpoint admin → 400
- [ ] Rate limit 30 req/phút cho `/api/auth/**`
- [ ] Guest chỉ thấy thông báo hệ thống (`is_system = true`)
- [ ] Admin products không truy cập ẩn danh được

### 10.2. Quản trị hệ thống

- [ ] SUPER_ADMIN tạo ADMIN/MODERATOR
- [ ] Admin không thể xóa chính mình
- [ ] Admin xem/duyệt/từ chối farm với lý do bắt buộc
- [ ] Moderator chỉ đọc, không ghi
- [ ] Admin quản lý sản phẩm: PENDING_REVIEW → ACTIVE/INACTIVE
- [ ] Admin quản lý danh mục (CRUD)
- [ ] Admin xem blockchain transactions, retry transaction
- [ ] SUPER_ADMIN deploy smart contract

### 10.3. Nông trại

- [ ] Farm đăng ký → PENDING, upload GPKD
- [ ] Admin duyệt → APPROVED, thông báo cho farm
- [ ] Admin từ chối → REJECTED + lý do, farm nộp lại
- [ ] Farm tạo mùa vụ (IN_PROGRESS)
- [ ] Thêm quy trình canh tác (SOIL_PREP, SEEDING, FERTILIZATION, PEST_CONTROL, HARVESTING)
- [ ] Mỗi quy trình ghi txHash (blockchain attestation)
- [ ] Chuyển HARVESTED + nhập sản lượng
- [ ] Xuất kho → traceHash + QR code
- [ ] Đẩy sản phẩm lên sàn → PENDING_REVIEW
- [ ] Farm xem đơn hàng của mình, accept/reject
- [ ] Farm gửi báo cáo → admin xử lý
- [ ] Farm xem IoT dashboard (nhiệt độ/độ ẩm/pH)
- [ ] Farm mua gói dịch vụ → PENDING_PAYMENT → ACTIVE

### 10.4. Nhà bán lẻ

- [ ] Retailer đăng ký + đăng nhập
- [ ] Tìm kiếm sản phẩm trên sàn (keyword, category, price)
- [ ] Xem chi tiết sản phẩm (cache Redis)
- [ ] Đặt mua → PENDING
- [ ] Farm accept → ACCEPTED
- [ ] Retailer đặt cọc 30% → DEPOSIT_PAID
- [ ] Hủy đơn (PENDING/ACCEPTED → CANCELLED; DEPOSIT_PAID → CANCEL_REQUESTED)
- [ ] Xem lịch sử đơn hàng (my orders)
- [ ] Retailer confirm delivery → COMPLETED
- [ ] Retailer upload delivery images (≤ 5)
- [ ] Retailer gửi báo cáo → admin xử lý
- [ ] Retailer nhận thông báo (SSE)

### 10.5. Vận chuyển

- [ ] Shipping Manager tạo lô từ đơn DEPOSIT_PAID
- [ ] Phân công tài xế (IDLE) + xe (AVAILABLE)
- [ ] Shipment status: PICKING_UP → IN_TRANSIT → DELIVERED
- [ ] Shipping Manager xem shipment, cancel (chỉ PICKING_UP)
- [ ] Quản lý xe (CRUD), quản lý tài xế (CRUD, gán xe)
- [ ] Driver nhận lô, scan QR → pickup (traceHash phải khớp)
- [ ] Driver tracking (GPS, ≤ 500km so với checkpoint trước)
- [ ] Driver báo cáo sự cố (`REPORT_*` prefix)
- [ ] Driver deliver → order DELIVERED
- [ ] Shipping Manager broadcast thông báo

### 10.6. Thanh toán & Gói dịch vụ

- [ ] Sepay webhook xác nhận cọc (mã trong văn bản tự do)
- [ ] Sepay webhook kích hoạt subscription
- [ ] Mua gói → PENDING_PAYMENT → ACTIVE (webhook)
- [ ] Subscription unique (farm_id, status) — chống race
- [ ] Hủy subscription → CANCELLED

### 10.7. Blockchain

- [ ] Mùa vụ tạo → txHash ghi nhận
- [ ] Quy trình canh tác → txHash mỗi bước
- [ ] Xuất kho → broadcast transaction → txHash + traceHash + QR
- [ ] `BlockchainMaintenanceJob` xác nhận receipt → ghi status CONFIRMED
- [ ] Kiểm tra `chainMode = LIVE` khi live mode
- [ ] `GET /api/trace/{hash}` → trả đầy đủ product + farm + season + export + timeline
- [ ] Smart contract deploy (SUPER_ADMIN only), update, status
- [ ] RLP encoding 9-field, RFC6979 deterministic, low-s signature

### 10.8. Công khai & Guest

- [ ] `GET /api/public/products` → chỉ ACTIVE (không cần login)
- [ ] `GET /api/public/education` → chỉ PUBLISHED
- [ ] `GET /api/trace/{hash}` → công khai (chỉ lô READY)
- [ ] `GET /api/notifications` → guest chỉ thấy hệ thống
- [ ] Trang `/trace/{hash}` → SPA rendering đúng

### 10.9. Hiệu năng

- [ ] Load test: p95 < 800ms, error 0%
- [ ] Categories cache: TTL 60s, Redis hit
- [ ] Marketplace detail cache: Redis hit

---

## PHẦN 11: PHỤ LỤC — CÁC LỆNH CHẠY THÔNG DỤNG

```bash
# ── Backend ──
cd backend && mvn test                                    # Tất cả unit + integration
cd backend && mvn test -Dtest=FarmApprovalServiceTest     # 1 class
cd backend && mvn test -Dtest='CrossRoleMatrixIntegrationTest,RateLimitFilterTest' test  # Multiple classes
cd backend && mvn clean package -DskipTests               # Build JAR
cd backend && mvn spring-boot:run                         # Run app

# ── Frontend ──
cd web && npm test                                        # Unit tests
cd web && npm run build                                   # Build for production
cd web && npm run dev                                     # Dev server (port 5174)
cd web && npm run lint                                    # Lint check

# ── Cross-role HTTP probes ──
node dev/tests/cross-role-matrix.mjs                      # 122 probes (need server running)

# ── Load test ──
k6 run dev/loadtest/k6-loadtest.js                        # k6
node dev/loadtest/node-loadtest.mjs --vus 100 --requests 500  # Node

# ── Blockchain ──
BC=$(find ~/.m2/repository/org/bouncycastle -name 'bcprov-jdk18on-*.jar' | head -1)
java -cp "backend/target/classes:$BC" dev/tools/WalletGen.java  # Sinh ví signer

# ── 1-port mode ──
cd web && npm install && npm run build
rm -rf ../backend/src/main/resources/static
mkdir -p ../backend/src/main/resources/static
cp -r dist/* ../backend/src/main/resources/static/
cd ../backend && mvn spring-boot:run
```

---

## PHẦN 12: GHI CHÚ VỀ CẤU HÌNH `.env` CHO TESTING

### Test/CI profile (`src/test/resources/application.properties`)
- H2 in-memory — không cần MySQL
- `app.cache.enabled=false` — không cần Redis
- `blockchain.mode=mock` — hash giả lập
- `app.allow-simulation=true` — cho phép mock mode
- Test-only secrets được chấp nhận (không dùng giá trị production)

### Production profile (`.env` cho `mvn spring-boot:run`)
```properties
SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/bicap_db?...
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<32+ byte Base64>
SEPAY_API_KEY=<real sepay key>
SPRING_REDIS_HOST=<redis host>
SPRING_REDIS_PORT=6379
SPRING_REDIS_PASSWORD=<password>
BLOCKCHAIN_MODE=live
BLOCKCHAIN_EXPORT_MODE=vechain
BLOCKCHAIN_PRIVATE_KEY=<32-byte-hex>
BLOCKCHAIN_NODE_URL=https://testnet.vechain.org
APP_CACHE_ENABLED=true
ALLOW_SIMULATION=false
```

> **Quan trọng:** `SecureSecretInitializer` + `SecretConfigValidator` sẽ **dừng khởi động** nếu `.env` chứa placeholder, secret đã từng công bố trong repo, hoặc cấu hình không hợp lệ.

---

## TÓM TẮT

| Tầng | Công cụ | Số test | Thời gian | Cần server? |
|---|---|---|---|---|
| Unit (backend) | JUnit 5 + Mockito | ~345 | 1-2 phút | Không |
| Unit (frontend) | Vitest + Testing Library | ~84 | 30 giây | Không |
| Integration | `@SpringBootTest` + MockMvc | ~30 + full lifecycle | 2-3 phút | Không |
| Cross-role HTTP | Node script | 122 probe | 2-5 phút | Có (:8080) |
| Load/Stress | k6 / Node | 1500+ req | 1-2 phút | Có (:8080) |
| Blockchain Live | VeChainThor testnet | 3 TX | ~1 phút | Có + testnet |
| UAT thủ công | Checklist | 29 ca | ~2 giờ | Có (:8080) |
| **Tổng cộng** | | **~450+ automated + 29 manual** | **~15 phút** | |

> **Tham chiếu:** `docs/testing-document.md` (kết quả test chi tiết), `docs/uat-plan.md` (kịch bản UAT), `docs/system-specification.md` (đặc tả toàn diện), `docs/requirement.md` (yêu cầu nghiệp vụ).
