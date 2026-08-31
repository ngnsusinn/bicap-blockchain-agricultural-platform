# Tài liệu Kiểm thử (Testing Document)

| Thông tin | Chi tiết |
|---|---|
| **Dự án** | BICAP — Blockchain Integration in Clean Agricultural Platform |
| **Mã ticket** | BICAP-96 (liên quan BICAP-86/87/88/89/90) |
| **Phiên bản** | 1.0 — 30/08/2026 |
| **Môi trường test** | Windows x64 · JDK 21 (Temurin) · Node 24 · H2 in-memory (MODE=MySQL) · blockchain.mode=mock |

---

## 1. Chiến lược kiểm thử (Test Plan)

| Tầng | Loại | Công cụ | Phạm vi | Tần suất |
|---|---|---|---|---|
| Đơn vị (backend) | Unit | JUnit 5 + Mockito | Service/Controller/security/crypto | Mỗi commit (CI `mvn test`) |
| Đơn vị (frontend) | Unit/Component | Vitest + Testing Library + jsdom | Session helpers, LoginForm, badge styles, API helpers, StatusBadge, DashboardPage | Mỗi commit (`npm test`) |
| Tích hợp | Integration | `@SpringBootTest` + MockMvc (full filter chain) | Luồng liên module end-to-end | Mỗi commit |
| Hiệu năng | Load/Stress | k6 (`loadtest/k6-loadtest.js`) + node runner | Read-path có/không cache, JWT | Trước release |
| Bảo mật | Security | Unit + integration | RBAC endpoint, JWT, rate limit, secrets fail-fast, chữ ký tx | Mỗi commit |
| Chấp nhận | UAT | Checklist thủ công (BICAP-90) | Theo vai trò, trên bản 1-port | Trước nghiệm thu |

**Tiêu chí thoát (exit criteria):** 100% unit/integration pass · error rate load test < 1% · p95 < 800ms · không có lỗ hổng bảo mật mức Cao trong checklist BICAP-89.

## 2. Kết quả kiểm thử chức năng (BICAP-86/87)

### 2.1 Tổng hợp — chạy ngày 30/08/2026
| Suite | Số test | Kết quả |
|---|---|---|
| Backend (`mvn test`, 33 lớp test) | **241** | ✅ 241 pass / 0 fail / 0 skip |
| Farm Portal (`npm test`) | **17** | ✅ pass |
| Admin Web (`npm test`) | **11** | ✅ pass |
| **Tổng** | **269** | ✅ **PASS** |

### 2.2 Ánh xạ test theo module (trích các lớp chính)
| Module / Ticket | Test class | Số TC | Vùng kiểm tra chính |
|---|---|---|---|
| Auth & RBAC (BICAP-72) | `AuthServiceTest`, `AuthControllerTest`, `JwtAuthenticationFilterTest`, `ActorAuthorizerTest` | 34 | Đăng ký/đăng nhập, khóa tài khoản, JWT, phân quyền 3 cấp admin |
| Admin (BICAP-1) | `AdminServiceTest` | 5 | CRUD, soft-delete, không tự xóa, SUPER_ADMIN-only |
| Farm approval (BICAP-3/4) | `FarmApprovalServiceTest` | 22 | Duyệt/từ chối + lý do bắt buộc, batch load, ghi chú, status |
| Farm self-service (BICAP-9) | `FarmRegistrationServiceTest` | 7 | Cập nhật hồ sơ, trùng tên, upload chứng nhận, sở hữu chéo |
| Season & process (BICAP-12→15/73) | `SeasonExportServiceTest`, tích hợp trong lifecycle | 3+ | Harvest → export, idempotency, đơn vị khớp |
| Trading floor (BICAP-18/19) | `TradingFloorServiceTest` | 8 | Điều kiện export READY, 1–10 ảnh, quyền farm |
| Sản phẩm (BICAP-5) | `ProductServiceTest`, `ProductRepositoryIntegrationTest` | 18 | Lọc/phân trang, stats, đổi trạng thái, guard danh mục |
| Đơn hàng (BICAP-20/42→46/75) | `OrderServiceTest`, `OrderManagementServiceTest`, `FarmManagerOrderServiceTest` | 40 | Vòng đời PENDING→…→COMPLETED, hủy, đặt cọc |
| Vận chuyển (BICAP-22/23/76) | `ShipmentService` qua lifecycle IT, `FarmShipmentServiceTest` | 6 | Quyền farm, đếm trạng thái, tỷ lệ đúng hạn |
| Báo cáo (BICAP-27) | `ReportServiceTest` | 9 | Gửi mọi role, admin-only xử lý, thông báo 2 chiều |
| Notification (BICAP-77) | `NotificationServiceTest`, `NotificationControllerTest` | 17 | unread count, đánh dấu đọc, SSE |
| Blockchain (BICAP-6/74/80/81) | `BlockchainServiceTest`, `VeChainCryptoTest`, `BlockchainSecurityTest` | 22 | RLP vectors, RFC6979 deterministic, low-s, recovery id, địa chỉ ví/contract, RBAC tx |
| Thanh toán (BICAP-78) | `SepayServiceTest`, `SubscriptionServiceTest` | 16 | Webhook chữ ký, kích hoạt gói |
| Frontend farm | `auth.test.ts`, `LoginForm.test.tsx`, `ui.test.ts` | 17 | Phiên localStorage, quick-fill 3 role, validation, endpoint mapping |
| Frontend admin | `api.test.ts`, `StatusBadge.test.tsx`, `DashboardPage.test.tsx` | 11 | API_ORIGIN, header, format date, dashboard render + error toast |

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

Chạy `loadtest/node-loadtest.mjs` trên bản 1-port (H2, cache in-memory, máy dev cá nhân):

### 3.1 Load — 20 VUs × 200 req/pha
| Kịch bản | Req | Lỗi | TB | p50 | p95 | p99 |
|---|---|---|---|---|---|---|
| A — GET /api/categories | 200 | 0 | 41.5ms | 37.0 | 80.2 | 110.2 |
| B — GET /api/marketplace/products | 200 | 0 | 74.3ms | 50.3 | 189.3 | 356.3 |
| C — GET /api/service-packages | 200 | 0 | 27.2ms | 25.4 | 44.6 | 104.5 |

### 3.2 Stress — 100 VUs × 500 req/pha (file `loadtest/results-stress-100vu.json`)
| Kịch bản | Req | Lỗi | TB | p95 | p99 |
|---|---|---|---|---|---|
| A — categories (cache) | 500 | 0 | 133.4ms | 239.9 | 248.8 |
| B — marketplace search | 500 | 0 | 152.0ms | 495.9 | 641.3 |
| C — service-packages | 500 | 0 | 81.8ms | 111.7 | 127.5 |
| **Tổng** | **1500** | **0 (0%)** | — | — | **~625 req/s** |

**Kết luận:** đạt ngưỡng thoát (p95 < 800ms, error 0%). Thị trường thật với MySQL + Redis cache kỳ vọng tốt hơn H2/in-memory. Chạy k6: `k6 run loadtest/k6-loadtest.js -e BASE_URL=…`.

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

## 5. Lỗi đã ghi nhận & khắc phục (defect log)
| ID | Mô tả | Severity | Phát hiện bởi | Trạng thái |
|---|---|---|---|---|
| DEF-01 | Thiếu cột harvested_quantity/harvest_unit → export lỗi trên schema tự sinh | Cao | FullLifecycleIT | ✅ Đã sửa (entity + API + UI) |
| DEF-02 | Trang chủ farm portal bị `@GetMapping("/")` che mất SPA shell | Trung | Verify 1-port | ✅ Đã sửa (SpaForwardController) |
| DEF-03 | Đăng nhập admin trên farm portal rơi vào màn hình lặp | Thấp | UAT nội bộ | ✅ Đã sửa (SSO redirect + token) |
| DEF-04 | Health 503 khi SMTP chưa cấu hình | Thấp | Boot test | ⚠️ Ghi nhận — mail không chặn luồng chính; cấu hình SMTP thật khi deploy |

## 6. UAT (BICAP-90)
Kế hoạch + kịch bản + biên bản mẫu: xem `docs/uat-plan.md`.

## 7. Phụ lục — cách chạy
```bash
mvn test                                   # backend 241 TC
cd frontend  && npm test                   # 17 TC
cd admin-web && npm test                   # 11 TC
node loadtest/node-loadtest.mjs --vus 100 --requests 500   # stress
k6 run loadtest/k6-loadtest.js             # k6 (cài k6 trước)
```
