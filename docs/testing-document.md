# Tài liệu Kiểm thử (Testing Document)

| Thông tin | Chi tiết |
|---|---|
| **Dự án** | BICAP — Blockchain Integration in Clean Agricultural Platform |
| **Mã ticket** | BICAP-96 (liên quan BICAP-86/87/88/89/90) |
| **Phiên bản** | 1.2 — 11/09/2026 |
| **Môi trường test** | Windows/Linux x64 · JDK 21 (Temurin) · Node 22.22+ hoặc 24.15+ · H2 in-memory (MODE=MySQL) · blockchain.mode=mock |

---

## 1. Chiến lược kiểm thử (Test Plan)

| Tầng | Loại | Công cụ | Phạm vi | Tần suất |
|---|---|---|---|---|
| Đơn vị (backend) | Unit | JUnit 5 + Mockito | Service/Controller/security/crypto | Mỗi commit (CI `mvn test`) |
| Đơn vị (frontend/web) | Unit/Component | Vitest + Testing Library + jsdom | `web/` hợp nhất (portal + admin): session dùng chung, LoginForm, badge styles, API helpers, StatusBadge, DashboardPage | Mỗi commit (`npm test`) |
| Tích hợp | Integration | `@SpringBootTest` + MockMvc (full filter chain) | Luồng liên module end-to-end | Mỗi commit |
| Hiệu năng | Load/Stress | k6 (`dev/loadtest/k6-loadtest.js`) + node runner | Read-path có/không cache, JWT | Trước release |
| Bảo mật | Security | Unit + integration | RBAC endpoint, JWT, rate limit, secrets fail-fast, chữ ký tx | Mỗi commit |
| Chấp nhận | UAT | Checklist thủ công (BICAP-90) | Theo vai trò, trên bản 1-port | Trước nghiệm thu |

**Tiêu chí thoát (exit criteria):** 100% unit/integration pass · error rate load test < 1% · p95 < 800ms · không có lỗ hổng bảo mật mức Cao trong checklist BICAP-89.

## 2. Kết quả kiểm thử chức năng (BICAP-86/87)

### 2.1 Tổng hợp — chạy ngày 11/09/2026 sau đợt audit (C-1…C-4, F1…F12) + vá đường live + kiểm thử cross-role
| Suite | Số test | Kết quả |
|---|---|---|
| Backend (`cd backend && mvn test`) | **326** | ✅ 326 pass / 0 fail / 0 skip |
| Web hợp nhất (`cd web && npm test`, 16 file test) | **52** | ✅ 52 pass (portal + admin) |
| Ma trận cross-role HTTP thật (`node dev/tests/cross-role-matrix.mjs`) | **122** | ✅ 122 probe pass / 0 fail |
| Web build + lint (`npm run build`, `npm run lint`) | — | ✅ build pass · 0 error (57 warning style/hooks) |
| **Tổng** | **378 test + 122 probe** | ✅ **PASS** |

Bổ sung so với bản 1.0 (279 test): +42 test backend và +24 test frontend, tập trung vào các lỗi
đã phát hiện khi audit (xem 2.4), các trang trước đây chưa có test (guest, driver, shipping, admin),
và 3 lỗi chỉ lộ ra khi **broadcast thật** lên VeChainThor (xem 2.6).
Bản 1.2 bổ sung **30 test cross-role**, **3 test rate-limit** và **122 probe HTTP đa vai trò** (xem 2.7).


### 2.6 Kiểm chứng broadcast THẬT lên VeChainThor testnet (11/09/2026)

Chạy `BLOCKCHAIN_MODE=live` với ví signer `0x4b620653cb075d4257ec676b2f0e5a56a747d350`
(500 VET / 50 VTHO từ faucet). Ba lỗi dưới đây **không thể phát hiện bằng mock mode** và đã được vá:

| # | Lỗi | Triệu chứng từ node | Bản vá |
|---|---|---|---|
| L1 | `blockRef` truyền **số block** thay vì 8 byte đầu **block ID** | `403 tx rejected: expired` | `VeChainTxSigner.blockRefFromBlockId` + dùng `best.id()` |
| L2 | Hash để ký gồm **10 field** (có field signature rỗng) thay vì **9 field** | `403 tx rejected: insufficient energy` (thor giải ra địa chỉ khác, 0 VTHO) | `signType0` băm payload 9 field; chỉ thêm signature vào bản broadcast |
| L3 | `getTransactionStatus` đọc field `txStatus` **không tồn tại** trong thor REST API | ledger mãi `PENDING`, `season.txHash` không bao giờ được ghi | Dựa vào `meta.blockID` + receipt `reverted` |

Cách ly và xác minh: dựng lại giao dịch bằng SDK chính thức (`thor-devkit`) với cùng tham số,
so **byte-by-byte** phần RLP, rồi đối chiếu với giao dịch thật trên testnet.

Kết quả sau khi vá — 3 giao dịch thật, đều `reverted = false`, `gasUsed = 23176`, `0.23176 VTHO`/tx:

| Entity | Tx hash | Block | Ledger |
|---|---|---|---|
| `SEASON` | `0x9b74bd3dc6b54bf6b1fdf22b4cc7c5b7cb150f671d953f1479f402dfb99bfbfc` | 25909484 | CONFIRMED |
| `PROCESS` | `0xa888293c175392d0b4b389692931d5cfbfb0dd0d04e3aaf36c3664b6e7dd0670` | 25909484 | CONFIRMED |
| `SEASON_EXPORT` | `0x2de43af9362ca20c110a37e55b32c755f95ca75b20394035c650f20d3d618738` | 25909484 | CONFIRMED |

`origin` của cả ba = `0x4b620653cb075d4257ec676b2f0e5a56a747d350`; export trả `chainMode = LIVE`;
`season.txHash` được job `BlockchainMaintenanceJob` ghi ngược về entity sau khi confirm.
Tra cứu: `https://explore-testnet.vechain.org/transactions/<txHash>`.
Test hồi quy: `VeChainCryptoTest.signType0_matchesOfficialSdkGoldenVector` (raw khớp SDK)
và `VeChainClientTest` (ánh xạ 404 / pending / confirmed / error).

### 2.7 Kiểm thử cross-role toàn diện (11/09/2026)

Mỗi chức năng được kiểm tra từ **mọi phía**, không chỉ từ vai trò sở hữu:
admin ⇄ farm, farm ⇄ admin, farm ⇄ retail, retail ⇄ farm, shipping ⇄ driver,
cùng vai trò khác tenant (chống IDOR), đăng nhập sai portal và khách ẩn danh.

| # | Bộ kiểm thử | Số TC | Cách chạy |
|---|---|---|---|
| 1 | `CrossRoleMatrixIntegrationTest` (MockMvc, full security filter chain + H2 seeder) | **30** | `cd backend && mvn -Dtest=CrossRoleMatrixIntegrationTest test` |
| 2 | `RateLimitFilterTest` (unit, biên 30 req/phút) | **3** | `cd backend && mvn -Dtest=RateLimitFilterTest test` |
| 3 | `dev/tests/cross-role-matrix.mjs` (HTTP thật trên server đang chạy) | **122 probe** | `node dev/tests/cross-role-matrix.mjs` |

Fixture của bộ test được dựng **qua chính API** theo đúng chuỗi nghiệp vụ đa vai trò
(farm tự đăng ký → admin duyệt → mua gói → mùa vụ → thu hoạch → xuất kho → đẩy sàn →
admin duyệt → retailer đặt → farm chấp nhận → shipping tạo lô), nên bước dựng dữ liệu
cũng là một ca kiểm thử tích hợp.

Các nhóm chức năng được quét (mỗi nhóm kiểm tra cả vai trò được phép lẫn bị từ chối):

| Nhóm | Góc nhìn được kiểm tra |
|---|---|
| Đăng nhập portal | đúng portal (200) · sai portal farm→retail, retail→admin, driver→shipping… (401, không nâng quyền ngầm) |
| `X-Actor-Email` | JWT farm/retail/driver + header admin → 401; admin thiếu header → 400 |
| Farm self-service | owner 200 · farm khác tenant 403 · retailer/driver/shipping/admin 403 · guest 401/403; retailer tạo hồ sơ farm → 403 |
| Duyệt farm | admin-view (admin/superadmin/moderator) 200 · farm/retail 403 · moderator ghi (approve/reject) 403 · farm tự duyệt farm mình 403 |
| Mùa vụ / quy trình / xuất kho | owner 200 · farm khác tenant 403 · retailer 403 · admin chỉ đọc export (không đọc process) · trace công khai 200 |
| Sàn giao dịch + giám sát sản phẩm | farm sở hữu 201/200 · farm khác tenant 403 · moderator đọc 200 / ghi 403 · retailer đọc danh sách admin 403 (kể cả **không gửi header**) |
| Đơn hàng | farm list 200 / retailer 403 · retailer `my` 200 / farm 403 · accept/reject chỉ farm sở hữu · deposit/cancel/complete chỉ retailer sở hữu |
| Vận chuyển | mọi endpoint `/api/shipping/**` chỉ SHIPPING_MGR (farm/retail/driver/admin đều 403) |
| Tài xế | `/api/driver/**` chỉ SHIP_DRIVER · tài xế không được gán lô → 403 · QR sai/thiếu → 400 · luồng pickup→tracking(báo cáo)→deliver→retailer complete chạy đủ 2 chiều |
| Marketplace | retailer 200 · farm/driver/guest 403 · catalogue công khai & trace ẩn danh 200 |
| IoT | chủ farm 200 · farm khác tenant/retailer/driver 403 · admin/superadmin bắc cầu quyền sở hữu (200) · ngoài dải 400 |
| Báo cáo | mọi vai trò gửi được 201 · moderator/admin đọc 200 · farm/retail đọc 403 · chỉ admin xử lý (moderator 403) |
| Thông báo | guest chỉ thấy thông báo hệ thống 200 · đánh dấu đã đọc tin của người khác 403 · broadcast chỉ SHIPPING_MGR 201 |
| Gói dịch vụ / thuê bao | danh sách công khai 200 · danh sách admin moderator/admin 200, farm 403 · tạo gói chỉ admin 201, moderator/farm 403 · mua gói chỉ chủ farm/admin, farm khác & retailer 403 |
| Blockchain / smart contract | ledger & contract: admin/moderator 200, farm/retail 403 · retry tx: moderator 403 · deploy: superadmin mới được, admin/moderator 403 |
| Tài khoản admin | liệt kê: admin/superadmin/moderator 200, farm/retail 403 · tạo admin: chỉ superadmin 201, admin/moderator 403 |
| Đối tác bán lẻ / hồ sơ | `/api/retailers` chỉ FARM_MANAGER · `/api/retailer/**` chỉ RETAILER · hồ sơ cá nhân mọi vai trò đã đăng nhập |

**Lỗi hồi quy bắt được và đã vá nhờ bộ này:** CR-01 và CR-02 (xem mục 5).


### 2.4 Bộ test hồi quy sau audit (mới)
| Finding | Test | Nội dung kiểm tra |
|---|---|---|
| C-1 | `SecureSecretInitializerTest` | Từ chối secret mặc định đã từng public trong repo và mọi giá trị template (`replace-with-…`, `your_…`); sinh secret ngẫu nhiên khi để trống ở dev; fail-fast ở profile `prod` |
| C-1 | `SecurityAndGuestRegressionIntegrationTest.forgedTokenSignedWithShippedDefaultSecret_isRejected` | JWT ký bằng secret mặc định cũ → 4xx |
| C-2 | `SepayServiceTest.matchesDeposit_whenCodeIsEmbeddedInFreeTextMemo` | Nội dung chuyển khoản chứa mã ở giữa câu vẫn khớp và ghi nhận cọc |
| C-2 | `SepayServiceTest.matchesSubscription_whenCodeArrivesInSepayCodeField` | Dùng trường `code` do Sepay cung cấp |
| C-3 | `SecurityAndGuestRegressionIntegrationTest` (anonymous*) | `/api/admin/products/**`, `/api/admin/products/categories` không truy cập ẩn danh được, kể cả khi giả `X-Actor-Email` |
| C-3 | `SecurityAndGuestRegressionIntegrationTest.guestFeed_containsOnlySystemAnnouncements` | Guest chỉ thấy thông báo hệ thống, không lộ tin nhắn riêng |
| C-4 | `SecurityAndGuestRegressionIntegrationTest.iot_*` | Retailer/Driver bị 403; chủ farm hợp lệ 200; giá trị ngoài dải → 400 |
| F1 | `SeasonExportServiceTest.create_records{Live,Mock}ChainMode` | `chainMode` phản ánh đúng việc broadcast thật hay mô phỏng |
| F2 | `AdminServiceTest.createAdmin_persistsRequestedPermissions` | Permission được lưu vào `user_permissions` và trả về trong response |
| F3/F5 | `SecurityAndGuestRegressionIntegrationTest.public*` | Catalogue và nội dung giáo dục công khai trả dữ liệu thật |
| F8 | `SecurityAndGuestRegressionIntegrationTest.shippingReadyToShipAndCompleted_areDistinctEndpoints` | Hai danh sách tách biệt đúng ngữ nghĩa |
| F11 | `SecurityAndGuestRegressionIntegrationTest.driverPickup_requiresScannedTraceHash` | Xác nhận lấy hàng bắt buộc có QR khớp |
| F12 | `SecurityAndGuestRegressionIntegrationTest.legacyExportListing_isTenantScoped` | Chặn đọc chéo tenant ở endpoint export cũ |

### 2.5 Kiểm chứng thủ công trên server thật (11/09/2026)
Ngoài test tự động, toàn bộ luồng được chạy lại bằng HTTP thật (`curl`) trên backend đang chạy:
farm tạo mùa vụ → quy trình → thu hoạch → export (nhận `chainMode`, `txHash`, ledger `SEASON_EXPORT`) →
đẩy sản phẩm lên sàn → admin duyệt → retailer đặt hàng → farm chấp nhận → retailer đặt cọc →
webhook Sepay **có văn bản kèm theo** → đơn `DEPOSIT_PAID` → shipping tạo lô → tài xế quét QR
(sai/thiếu → 400, đúng → 200) → báo cáo tài xế → Shipping Manager nhận thông báo.
Đồng thời xác nhận: JWT giả bằng secret cũ → 403, guest chỉ thấy thông báo hệ thống,
`/api/iot/sensors` chặn sai vai trò, `/actuator/health` = `UP`.

### 2.2 Ánh xạ test theo module (trích các lớp chính)
| Module / Ticket | Test class | Số TC | Vùng kiểm tra chính |
|---|---|---|---|
| Auth & RBAC (BICAP-72) | `AuthServiceTest`, `AuthControllerTest`, `JwtAuthenticationFilterTest`, `ActorAuthorizerTest` | 34 | Đăng ký/đăng nhập, khóa tài khoản, JWT, phân quyền 3 cấp admin |
| Admin (BICAP-1) | `AdminServiceTest` | 6 | CRUD, soft-delete, không tự xóa, SUPER_ADMIN-only, gán permission (F2) |
| Farm approval (BICAP-3/4) | `FarmApprovalServiceTest` | 22 | Duyệt/từ chối + lý do bắt buộc, batch load, ghi chú, status |
| Farm self-service (BICAP-9) | `FarmRegistrationServiceTest` | 7 | Cập nhật hồ sơ, trùng tên, upload chứng nhận, sở hữu chéo |
| Season & process (BICAP-12→15/73) | `SeasonExportServiceTest`, tích hợp trong lifecycle | 5 | Harvest → export, idempotency, đơn vị khớp, `chainMode` (F1) |
| Trading floor (BICAP-18/19) | `TradingFloorServiceTest` | 8 | Điều kiện export READY, 1–10 ảnh, quyền farm |
| Sản phẩm (BICAP-5) | `ProductServiceTest`, `ProductRepositoryIntegrationTest` | 18 | Lọc/phân trang, stats, đổi trạng thái, guard danh mục |
| Đơn hàng (BICAP-20/42→46/75) | `OrderServiceTest`, `OrderManagementServiceTest`, `FarmManagerOrderServiceTest` | 40 | Vòng đời PENDING→…→COMPLETED, hủy, đặt cọc |
| Vận chuyển (BICAP-22/23/76) | `ShipmentService` qua lifecycle IT, `FarmShipmentServiceTest` | 6 | Quyền farm, đếm trạng thái, tỷ lệ đúng hạn |
| Báo cáo (BICAP-27) | `ReportServiceTest` | 9 | Gửi mọi role, admin-only xử lý, thông báo 2 chiều |
| Notification (BICAP-77) | `NotificationServiceTest`, `NotificationControllerTest` | 17 | unread count, đánh dấu đọc, SSE |
| Blockchain (BICAP-6/74/80/81) | `BlockchainServiceTest`, `VeChainCryptoTest`, `BlockchainSecurityTest` | 22 | RLP vectors, RFC6979 deterministic, low-s, recovery id, địa chỉ ví/contract, RBAC tx |
| Thanh toán (BICAP-78) | `SepayServiceTest`, `SubscriptionServiceTest` | 19 | Webhook chữ ký, khớp mã trong văn bản tự do (C-2), kích hoạt gói |
| Bảo mật/guest hồi quy | `SecurityAndGuestRegressionIntegrationTest` | 15 | C-1/C-3/C-4, catalogue & education công khai (F3/F5), F8, F11, F12 |
| Secret hardening | `SecureSecretInitializerTest` | 7 | C-1: chặn secret public/template, sinh secret dev, fail-fast ở prod |
| Cross-role / RBAC (BICAP-96) | `CrossRoleMatrixIntegrationTest`, `RateLimitFilterTest` + `dev/tests/cross-role-matrix.mjs` | 30 + 3 + 122 | Ma trận 10 vai trò × mọi nhóm chức năng; chống giả mạo header, IDOR, sai portal, guest |
| Frontend web (`web/`) | xem mục 2.4/2.6 | — | Portal + Admin: guest area, catalogue thật, education API, driver QR, tracking, admin permission, smart contract |

### 2.3 Kịch bản tích hợp end-to-end (`FullLifecycleIntegrationTest`)
12 bước trên context thật (security filter + JWT + H2 + seeder):

| # | Bước | Kết quả mong đợi | Thực tế |
|---|---|---|---|
| 1 | Farm đăng ký + tạo farm | 201, status PENDING | ✅ |
| 2 | Admin duyệt farm | 200, APPROVED | ✅ |
| 2b | Mua & kích hoạt gói | subscription ACTIVE | ✅ |
| 3 | Tạo mùa vụ + quy trình | 201, txHash `0x…` (mock) | ✅ |
| 3b | Harvest kèm sản lượng | 200, harvestedQuantity lưu | ✅ |
| 4 | Xuất kho | traceHash + QR cấp | ✅ |
| 5 | Đẩy sản phẩm lên sàn → admin duyệt | PENDING_REVIEW → ACTIVE | ✅ |
| 6 | Retailer tìm kiếm → đặt hàng | keyword khớp 1 kết quả, đơn PENDING | ✅ |
| 7 | Farm chấp nhận + đặt cọc | ACCEPTED → DEPOSIT_PAID (webhook mô phỏng) | ✅ |
| 8 | Shipping tạo lô (xe + tài xế) | 201 | ✅ |
| 9 | Driver pickup → tracking → deliver | trạng thái dịch chuyển đúng | ✅ |
| 10 | Retailer xác nhận hoàn tất | COMPLETED | ✅ |
| 11 | Farm gửi báo cáo → admin xử lý | OPEN → RESOLVED | ✅ |
| 12 | Notification tới các bên | unreadCount > 0 | ✅ |

**Lỗi sản xuất tìm ra nhờ integration test:** `farming_seasons` thiếu cột `harvested_quantity`/`harvest_unit` khiến luồng export không chạy được trên schema tự sinh — đã sửa (entity + bắt buộc sản lượng khi HARVESTED + UI Seasons).

## 3. Kết quả kiểm thử hiệu năng (BICAP-88)

Chạy `dev/loadtest/node-loadtest.mjs` trên bản 1-port (H2, cache in-memory, máy dev cá nhân):

### 3.1 Load — 20 VUs × 200 req/pha
| Kịch bản | Req | Lỗi | TB | p50 | p95 | p99 |
|---|---|---|---|---|---|---|
| A — GET /api/categories | 200 | 0 | 41.5ms | 37.0 | 80.2 | 110.2 |
| B — GET /api/marketplace/products | 200 | 0 | 74.3ms | 50.3 | 189.3 | 356.3 |
| C — GET /api/service-packages | 200 | 0 | 27.2ms | 25.4 | 44.6 | 104.5 |

### 3.2 Stress — 100 VUs × 500 req/pha (file `dev/loadtest/results-stress-100vu.json`)
| Kịch bản | Req | Lỗi | TB | p95 | p99 |
|---|---|---|---|---|---|
| A — categories (cache) | 500 | 0 | 133.4ms | 239.9 | 248.8 |
| B — marketplace search | 500 | 0 | 152.0ms | 495.9 | 641.3 |
| C — service-packages | 500 | 0 | 81.8ms | 111.7 | 127.5 |
| **Tổng** | **1500** | **0 (0%)** | — | — | **~625 req/s** |

**Kết luận:** đạt ngưỡng thoát (p95 < 800ms, error 0%). Thị trường thật với MySQL + Redis cache kỳ vọng tốt hơn H2/in-memory. Chạy k6: `k6 run dev/loadtest/k6-loadtest.js -e BASE_URL=…`.

## 4. Kết quả kiểm thử bảo mật (BICAP-89)

| # | Ca kiểm thử | Kết quả |
|---|---|---|
| S1 | Đọc `/api/blockchain/transactions` với FARM_MANAGER/RETAILER | ✅ 403 (ADMIN_VIEW required) |
| S2 | Đọc với MODERATOR | ✅ 200 (read-only role) |
| S3 | Retry tx với MODERATOR | ✅ 403 (ADMIN_WRITE required) |
| S4 | Retry với actor không tồn tại | ✅ từ chối |
| S5 | `BLOCKCHAIN_MODE=live` thiếu private key | ✅ app fail-fast khi boot |
| S6 | live có key hợp lệ | ✅ boot bình thường |
| S7 | Chữ ký tx: deterministic RFC6979, canonical low-s, recovery id 0/1 | ✅ unit test |
| S8 | Địa chỉ ví/contract suy ra ổn định, đúng định dạng | ✅ unit test |
| S9 | JWT secret yếu/placeholder, Sepay placeholder | ✅ fail-fast (`SecretConfigValidator`) |
| S10 | Brute-force `/api/auth/**` | ✅ 429 sau 30 req/phút/IP (`RateLimitFilter`) |
| S11 | SPA/static route không lộ API cần auth | ✅ chỉ shell HTML/asset được permitAll |
| S12 | Soft-delete admin, không tự xóa chính mình | ✅ `AdminServiceTest` |
| S13 | Giả mạo `X-Actor-Email` (JWT farm/retail/driver + header admin) | ✅ 401 — header phải khớp subject của JWT (`JwtAuthenticationFilter`) |
| S14 | Đăng nhập sai portal (farm→retail, retail→admin, driver→shipping…) | ✅ 401, không nâng quyền ngầm (M-3) |
| S15 | Retailer/driver đọc `/api/admin/products` khi **không** gửi `X-Actor-Email` | ✅ 403 (đã vá CR-01) |
| S16 | Retailer tạo hồ sơ nông trại `/api/farms/register` | ✅ 403 (đã vá CR-02) |
| S17 | Xoay `X-Forwarded-For` để né rate limit 30 req/phút | ✅ vẫn 429 (đã vá CR-06; mặc định key theo địa chỉ socket) |
| S18 | `PUT /api/shipping/drivers/{id}` với `status` tùy ý | ✅ 400 (đã vá CR-05) |
| S19 | `PUT /api/retailer/profile` thiếu trường bắt buộc | ✅ 400, không còn 500 (đã vá CR-04) |
| S20 | Guest đọc gói `INACTIVE` theo id | ✅ 404 (đã vá CR-03) |
| S21 | Vai trò không phải FARM_MANAGER nhưng sở hữu farm (dữ liệu cũ) | ✅ 403 ở mùa vụ/quy trình/xuất kho/thuê bao (đã vá CR-07) |

## 5. Lỗi đã ghi nhận & khắc phục (defect log)
| ID | Mô tả | Severity | Phát hiện bởi | Trạng thái |
|---|---|---|---|---|
| DEF-01 | Thiếu cột harvested_quantity/harvest_unit → export lỗi trên schema tự sinh | Cao | FullLifecycleIT | ✅ Đã sửa (entity + API + UI) |
| DEF-02 | Trang chủ farm portal bị `@GetMapping("/")` che mất SPA shell | Trung | Verify 1-port | ✅ Đã sửa (SpaForwardController) |
| DEF-03 | Đăng nhập admin trên farm portal rơi vào màn hình lặp | Thấp | UAT nội bộ | ✅ Đã sửa (SSO redirect + token) |
| DEF-04 | Health 503 khi SMTP chưa cấu hình | Thấp | Boot test | ⚠️ Ghi nhận — mail không chặn luồng chính; cấu hình SMTP thật khi deploy |
| CR-01 | `GET /api/admin/products` bỏ qua RBAC khi thiếu header `X-Actor-Email` → retailer/driver/farm đọc được danh sách sản phẩm của admin | Cao | `CrossRoleMatrixIT` (26 TC) + probe HTTP | ✅ Đã sửa — `ProductService.getProducts` luôn gọi `checkView` |
| CR-02 | `POST /api/farms/register` không yêu cầu vai trò `FARM_MANAGER` → mọi tài khoản đã đăng nhập tạo được hồ sơ nông trại | Cao | `CrossRoleMatrixIT` | ✅ Đã sửa — dùng `requireFarmManager()` |
| CR-03 | `GET /api/service-packages/{id}` trả cả gói `INACTIVE` cho khách ẩn danh (khác với danh sách chỉ trả ACTIVE) | Trung | Probe HTTP thật | ✅ Đã sửa — chỉ trả ACTIVE; admin vẫn xem qua `/admin/all` |
| CR-04 | `PUT /api/retailer/profile` thiếu `@Valid` trên `@ModelAttribute` → thiếu trường bắt buộc trả **500** thay vì 400 | Trung | Probe HTTP thật | ✅ Đã sửa — thêm `@Valid` cho profile + hồ sơ doanh nghiệp |
| CR-05 | `PUT /api/shipping/drivers/{id}` không validate `status` → nhận giá trị tùy ý, phá vỡ state machine driver | Trung | Probe HTTP thật | ✅ Đã sửa — chỉ nhận IDLE/ON_TRIP/OFFLINE |
| CR-06 | `RateLimitFilter` tin `X-Forwarded-For` do client gửi → xoay header là bỏ qua giới hạn 30 req/phút | Trung | Probe HTTP thật | ✅ Đã sửa — mặc định dùng địa chỉ socket; chỉ tin XFF khi bật `app.security.trust-forwarded-for=true` |
| CR-07 | Bất nhất role-check: `SeasonService`, `ProcessService`, `ExportController.createExport`, `SubscriptionController` chỉ kiểm tra quyền sở hữu, **không** yêu cầu `FARM_MANAGER` (khác các module farm còn lại) | Thấp | Audit ma trận endpoint | ✅ Đã sửa — yêu cầu `FARM_MANAGER` (admin-view vẫn bắc cầu ở subscription) |
| CR-08 | `DatabaseSeederTest` khẳng định tổng số farm = 4 và mọi farm đều có chứng nhận → vỡ khi test tích hợp khác tạo farm trong DB H2 dùng chung | Thấp | `mvn test` toàn bộ | ✅ Đã sửa — chỉ kiểm tra 4 farm seed + bất biến "không trùng tên" |

## 6. UAT (BICAP-90)
Kế hoạch + kịch bản + biên bản mẫu: xem `docs/uat-plan.md`.

## 7. Phụ lục — cách chạy
```bash
cd backend && mvn test                # backend 326 TC (port 8080 khi chạy app)
cd backend && mvn -Dtest='CrossRoleMatrixIntegrationTest,RateLimitFilterTest' test   # cross-role (30) + rate limit (3)
cd web     && npm test                # web 52 TC (hợp nhất portal + admin)
node dev/tests/cross-role-matrix.mjs  # 122 probe HTTP đa vai trò (cần backend đang chạy)
node dev/loadtest/node-loadtest.mjs --vus 100 --requests 500   # stress
k6 run dev/loadtest/k6-loadtest.js    # k6 (cài k6 trước)
```

> **Chạy 1 port (mặc định):** `cd web && npm install && npm run build` → copy `web/dist/*` vào `backend/src/main/resources/static/` → `cd backend && mvn spring-boot:run` → mở `http://localhost:8080/` (portal) và `http://localhost:8080/admin` (admin dashboard).
> **Chế độ dev:** `cd web && npm run dev` (5174) + `cd backend && mvn spring-boot:run` (8080).
