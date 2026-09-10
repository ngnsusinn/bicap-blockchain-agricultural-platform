# BÁO CÁO ĐÓNG GÓP THÀNH VIÊN

> **Dự án:** BICAP — Blockchain Agricultural Platform (Tích hợp Blockchain trong sản xuất nông sản sạch)
> **Repository:** `bicap-blockchain-agricultural-platform`
> **Khoảng thời gian:** 22/07/2026 → 10/09/2026
> **Tổng số commit:** 144 (87 commit không tính merge)
> **Số thành viên:** 5

---

## 1. Phương pháp thực hiện

Báo cáo được lập từ **lịch sử Git thực tế** của repository (không phải kê khai chủ quan):

- Đếm commit theo tác giả (`git shortlog`, `git log --no-merges`).
- Chuẩn hoá nhiều danh tính Git (email/tên khác nhau) về cùng một người.
- Xác định **người sở hữu chính (primary owner)** của từng file = người có nhiều commit sửa file đó nhất → dùng để suy ra mảng phụ trách.
- Đối chiếu commit với **mã yêu cầu BICAP-xx / EPIC-xx** trong `docs/user-requirements.md`.

**Quy ước quan trọng về đường dẫn:** các đường dẫn trong báo cáo là đường dẫn **tại thời điểm lịch sử (trước refactor)**. Sau đợt tái cấu trúc gần nhất (xem §8):

| Đường dẫn cũ (trong báo cáo) | Đường dẫn hiện tại |
| --- | --- |
| `frontend/` | `web/` (phần `web/src/portal/`) |
| `admin-web/` | `web/src/admin/` |
| `mobile-app/` | *(đã gỡ khỏi repo)* |
| `src/main/java/...` | `backend/src/main/java/...` |
| `blockchain/`, `loadtest/`, `tools/` | `dev/blockchain/`, `dev/loadtest/`, `dev/tools/` |

---

## 2. Bảng tổng hợp thành viên

| # | Thành viên | Danh tính Git (email) | Tổng commit | Commit (không merge) | Mảng phụ trách chính |
| --- | --- | --- | --- | --- | --- |
| 1 | **Sin Nguyen Sun** (nhóm trưởng) | `ngnsusinn@gmail.com`, `92439052+ngnsusinn@users.noreply.github.com` | 89 | 42 | Kiến trúc & hạ tầng, Core Security, Admin, Farm, Blockchain, Notifications, Product, Trading Floor, tài liệu |
| 2 | **Pham Thi Hoang Oanh** | `phamthihoangoanh.anphu@gmail.com` (XuanAnh1312 / JANE.N) | 18 | 14 | Tích hợp Auth frontend–backend, Admin UI/RBAC, Shipping Manager (frontend), Gói dịch vụ |
| 3 | **Nguyen Tuan Anh** | `mtuananh1911@gmail.com`, `98220888+Minjju12@users.noreply.github.com` (Minjju12) | 15 | 13 | Thanh toán & gói dịch vụ, Thông báo IoT, Guest (BICAP-69/70/71), Mobile App tài xế, CI khởi tạo |
| 4 | **Duy An** | `anclashofclanshall13@gmail.com` | 11 | 9 | Core Security & RBAC, API Mùa vụ, API Đơn hàng, API Vận chuyển, Auth tài xế/điều phối |
| 5 | **Doan Phuong Thien An** | `andpt2350@ut.edu.vn` | 11 | 9 | Toàn bộ module Retailer (BICAP-36→53), Xuất mùa vụ & QR (BICAP-16/17) |

---

## 3. Chi tiết đóng góp từng thành viên

### 3.1. Sin Nguyen Sun — Nhóm trưởng / Kiến trúc sư

**Vai trò:** khởi tạo dự án, định hình kiến trúc, phát triển phần lớn backend core và Admin portal, viết phần lớn tài liệu.

**Chức năng đã làm:**

| Mảng | Mô tả | Ticket | Commit tiêu biểu |
| --- | --- | --- | --- |
| Khởi tạo dự án | Cấu trúc ban đầu, Maven, package base, `application.properties` | — | `afe918e`, `82eef37`, `7d20c56` |
| Quản trị Admin & RBAC | CRUD tài khoản admin, gán vai trò/quyền, màn hình Admin Management | BICAP-1 | `3f3e535` |
| Phê duyệt nông trại | Danh sách/duyệt/từ chối đăng ký nông trại | BICAP-3 | `ba44ba9`, `9c9abe8` |
| Quản lý chi tiết nông trại | Chứng nhận, liên hệ, vị trí, ghi chú admin | BICAP-4 | `77818df` |
| Giám sát sản phẩm & danh mục | Duyệt/từ chối sản phẩm, quản lý category | BICAP-5 | `75bd42f` |
| Smart Contract | Deploy/cập nhật/quản lý hợp đồng trên VeChainThor | BICAP-6 | `8c09c09` |
| Xác thực đa cổng | Backend role support + UI đăng nhập 3 cổng | BICAP-7, BICAP-36 | `2e2850b`, `14333f8` |
| Sàn giao dịch | Đăng ký đẩy sản phẩm lên sàn | BICAP-18 | `0d1188f` |
| Xử lý đơn hàng (Farm) | Farm Manager xử lý yêu cầu mua | BICAP-20 | `2ed08e6` |
| Đối tác bán lẻ | Endpoint + UI xem nhà bán lẻ đã ký hợp đồng | BICAP-21 | `8cd3612` |
| Thông báo real-time | SSE, DTO, email, test cho hệ thống thông báo | BICAP-77 | `71e661e` |
| Báo cáo người dùng | API + UI báo cáo gửi Admin | BICAP-27 | `5c95645` |
| Blockchain VeChain | `VeChainClient`, `VeChainWallet`, `VeChainTxSigner`, `RlpEncoder`, `Hashes`, job xác nhận/retry giao dịch | BICAP-74, BICAP-80, BICAP-81 | `be27295`, `e7cd8b9` |
| Admin Dashboard | UI dashboard, thống kê, farm management page | BICAP-1→6 | `5c95645` |
| Hạ tầng & bảo mật | Redis cache, harden security/payment, rate limit, CSP | BICAP-79, BICAP-81 | `be27295`, `e7cd8b9` |
| Triển khai web 1-port | `SpaForwardController`, nhúng bundle vào backend | BICAP-84 | — |
| Tài liệu | User Requirements, SRS, Architecture Design, Detail Design, System Implementation, Testing, UAT, User Manual, Installation Guide, BAO-CAO-TONG-HOP, readme | BICAP-91→98 | `21f9fc7`, `8e764f1`, `6986191`, `9eb256e`, `d63995b` |

**File/module sở hữu chính:** `config/DatabaseSeeder.java`, `config/SecurityConfig.java` (đồng sở hữu), `common/security/*`, `service/AdminService.java`, `service/FarmApprovalService.java`, `service/ProductService.java`, `service/BlockchainService.java`, `service/TradingFloorService.java`, `controller/AdminController.java`, `controller/SmartContractController.java`, toàn bộ `admin-web/` (38 file), `docs/`.

---

### 3.2. Pham Thi Hoang Oanh — Frontend Auth & Shipping Manager

**Vai trò:** cầu nối xác thực frontend ↔ backend, chuẩn hoá Admin UI theo RBAC, phát triển cổng Shipping Manager và gói dịch vụ; tham gia điều phối merge PR.

**Chức năng đã làm:**

| Mảng | Mô tả | Ticket | Commit tiêu biểu |
| --- | --- | --- | --- |
| Tích hợp Auth FE–BE | Nối form login/register với API backend, gắn JWT vào header, lưu token | BICAP-7, BICAP-36 | `169c102`, `fc91e64` |
| Xử lý lỗi & validate Auth | Hiển thị lý do đăng nhập/đăng ký thất bại, regex Gmail/mật khẩu | BICAP-7 | `66ed5f8`, `6b80bd1`, `ecfb722` |
| Admin UI theo RBAC | Đồng bộ giao diện admin với vai trò/quyền từ backend | BICAP-1 | `99b547d` |
| Cập nhật hồ sơ chủ sở hữu | Màn hình cập nhật thông tin cá nhân | BICAP-8 | `01c0580` |
| Gói dịch vụ & Thanh toán | Màn hình gói dịch vụ, mua/thanh toán | BICAP-10, BICAP-11 | `18a78fd` |
| Shipping Manager (frontend) | Toàn bộ trang vận chuyển: shipments, tracking, vehicles, drivers, báo cáo, thông báo | BICAP-54→62 | `3d326f5`, `52189ed`, `f723e61` |
| Cấu hình | Chỉnh `application.properties` cho môi trường dev | — | `034c517` |
| Điều phối | Merge PR #74, #76, #80; xử lý conflict | — | `e4cb010`, `cc8a178`, `4c30257` |

**File/module sở hữu chính:** `frontend/src/components/Auth/*`, `frontend/src/pages/Shipping/*`, `frontend/src/pages/FarmManager/ServicePackages.tsx`, `frontend/src/utils/auth.ts`, `admin-web/src/components/LoginSimulator.tsx`, `service/ServicePackageService.java`, `service/SubscriptionService.java`, `service/ShipmentService.java`, `controller/ServicePackageController.java`, `controller/ShippingController.java`, `controller/SubscriptionController.java`.

---

### 3.3. Nguyen Tuan Anh — Thanh toán, IoT, Guest & Mobile App

**Vai trò:** phát triển module thanh toán/gói dịch vụ, thông báo IoT, toàn bộ tính năng Guest và **toàn bộ ứng dụng mobile tài xế**; thiết lập CI ban đầu.

**Chức năng đã làm:**

| Mảng | Mô tả | Ticket | Commit tiêu biểu |
| --- | --- | --- | --- |
| CI/CD khởi tạo | Tạo GitHub Actions workflow đầu tiên | BICAP-83 | `3de13a2` |
| Thanh toán & Gói dịch vụ | API + UI mua gói, `PaymentModal`, subscription, tích hợp Sepay | BICAP-10, BICAP-11, BICAP-78 | `269da45` |
| Thông báo IoT | Nhận thông báo nhiệt độ/độ ẩm/pH trong ngày (backend + UI) | BICAP-26 | `4d7bca1` |
| Guest — Thông báo chung | Nhận thông báo chung về nền tảng | BICAP-69 | `e7f73ba`, `d238cbc` |
| Guest — Tìm kiếm/lọc sản phẩm | Tìm kiếm, lọc theo nguồn gốc/loại/chứng nhận/tồn kho | BICAP-70 | `fc57d72`, `7c62ab0` |
| Guest — Nội dung giáo dục | Giao diện bài viết/video giáo dục nông nghiệp | BICAP-71 | `9ee3a9f`, `5b50364` |
| Kiểm thử | `NotificationControllerTest` cho guest notifications | BICAP-86 | `477194e` |
| **Mobile App tài xế** | Toàn bộ 18 file `mobile-app/`: đăng nhập, danh sách chuyến hàng, quét QR, xác nhận lấy/giao hàng, cập nhật hành trình, gửi báo cáo | BICAP-63→68, BICAP-85 | `f69df56` |
| Cấu hình | `CorsConfig`, `SepayConfig`, `IotDataController`, `IotDataService` | — | — |

**File/module sở hữu chính:** toàn bộ `mobile-app/` (18 file), `frontend/src/pages/Guest/*`, `frontend/src/components/PaymentModal.tsx`, `frontend/src/components/NotificationBell.tsx`, `service/impl/NotificationServiceImpl.java`, `service/IotDataService.java`, `service/MarketplaceService.java`, `controller/NotificationController.java`, `controller/ProductController.java`, `controller/IotDataController.java`, `config/CorsConfig.java`, `config/SepayConfig.java`.

---

### 3.4. Duy An — Backend Core Security & API nghiệp vụ

**Vai trò:** xây dựng nền tảng bảo mật JWT/RBAC, và các API backend lớn: mùa vụ, đơn hàng, vận chuyển.

**Chức năng đã làm:**

| Mảng | Mô tả | Ticket | Commit tiêu biểu |
| --- | --- | --- | --- |
| Core Security & RBAC | `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetailsService`, `SecurityConfig`, `ActorAuthorizer` | BICAP-72 | `a7ee1b7` |
| API Mùa vụ | CRUD farming season, cập nhật quy trình, lưu blockchain; kèm tài liệu + fix seeder | BICAP-73 | `7d060e9`, `4e18f9e`, `955cc11` |
| API Đơn hàng | Vòng đời đơn hàng đầy đủ: đặt mua, đặt cọc, hủy, xác nhận giao nhận | BICAP-75 | `7a728cb` |
| API Vận chuyển | Shipment, tracking, phương tiện, tài xế | BICAP-76 | `d372b6f`, `376c6bc` |
| Auth chuyên biệt | Endpoint login riêng cho Shipping Manager và Driver | BICAP-72 | `738ece9` |
| Ổn định CI | Sửa cấu hình test, loại bỏ file `Application`/`ApplicationTests` lỗi thời | BICAP-83 | `682d678`, `2d9fd34`, `e69379a` |

**File/module sở hữu chính:** `common/security/JwtTokenProvider.java`, `common/security/JwtAuthenticationFilter.java`, `config/SecurityConfig.java`, `service/SeasonService.java`, `service/ProcessService.java`, `service/OrderService.java`, `service/ShipmentService.java`, `service/VehicleService.java`, `service/DriverService.java`, `controller/DriverMobileController.java`, `entity/Order.java`, `entity/Driver.java`, `entity/Vehicle.java`, `entity/Shipment.java`, `entity/ShipmentTracking.java`, `docs/BICAP-73-Farming-Season-Report.md`, `docs/sql/bicap-76-shipment-schema.sql`.

---

### 3.5. Doan Phuong Thien An — Module Retailer (end-to-end)

**Vai trò:** phát triển trọn vẹn vai trò Nhà bán lẻ từ xác thực → marketplace → đơn hàng → vận chuyển → báo cáo, cùng tính năng xuất mùa vụ & QR.

**Chức năng đã làm:**

| Mảng | Mô tả | Ticket | Commit tiêu biểu |
| --- | --- | --- | --- |
| Xuất mùa vụ & QR | Export mùa vụ, sinh QR lưu blockchain, schema DB | BICAP-16, BICAP-17 | `970ca51` |
| Xác thực Retailer | Đăng ký/đăng nhập, xác thực email | BICAP-36 | `3cfbf60`, `9f632dc` |
| Hồ sơ & Giấy phép KD | Cập nhật thông tin cá nhân, business license | BICAP-37, BICAP-38 | `fad974d` |
| Marketplace | Tìm kiếm, xem chi tiết, quét QR, tạo yêu cầu mua, đặt cọc | BICAP-39→43 | `cd627ef` |
| Đơn hàng Retailer | Hủy đơn, lịch sử, chi tiết/trạng thái đơn | BICAP-44, BICAP-45, BICAP-46 | `ef0933e` |
| Thông báo & Tracking | Nhận/gửi thông báo với Farm Manager, xem quy trình vận chuyển | BICAP-47, BICAP-48, BICAP-49 | `d2749db` |
| Hoàn tất giao nhận | Nhận thông báo từ shipper, xác nhận đã nhận hàng, upload ảnh, báo cáo Admin | BICAP-50→53 | `848d21e`, `a10fd28` |

**File/module sở hữu chính:** `service/OrderService.java`, `service/RetailerProfileService.java`, `service/RetailerShipmentService.java`, `service/SeasonExportService.java`, `service/QrCodeService.java`, `service/LocalFileStorageService.java`, `service/LoginAttemptService.java`, `controller/OrderController.java`, `controller/MarketplaceController.java`, `controller/RetailerController.java`, `controller/SeasonExportController.java`, `controller/AuthController.java`, `entity/Order.java`, `entity/RetailerBusinessProfile.java`, `entity/SeasonExport.java`, `entity/BusinessType.java`, toàn bộ `frontend/src/pages/Retailer/*`, `docs/sql/bicap-16-17-schema.sql`, `docs/sql/bicap-36-38-schema.sql`.

---

## 4. Bảng đối chiếu module ↔ người phụ trách chính

Thống kê theo **file sở hữu chính** (số file trong module mà mỗi người có nhiều commit nhất):

| Module | Sin | Oanh | TuanAnh | DuyAn | ThienAn |
| --- | ---: | ---: | ---: | ---: | ---: |
| Backend — controller | 14 | 3 | 3 | 3 | 5 |
| Backend — service | 19 | 5 | 3 | 3 | 12 |
| Backend — entity | 19 | 0 | 4 | 4 | 6 |
| Backend — dto | 33 | 4 | 9 | 22 | 14 |
| Backend — repository | 15 | 1 | 3 | 3 | 3 |
| Backend — config | 4 | 0 | 2 | 0 | 1 |
| Backend — security (`common/`) | 14 | 0 | 0 | 0 | 0 |
| Backend — test | 25 | 1 | 1 | 1 | 10 |
| Web portal — FarmManager | 4 | 11 | 1 | 0 | 0 |
| Web portal — Retailer | 0 | 4 | 0 | 0 | 4 |
| Web portal — Shipping | 3 | 4 | 0 | 0 | 0 |
| Web portal — Guest | 0 | 0 | 3 | 0 | 0 |
| Web portal — Auth | 3 | 0 | 0 | 0 | 1 |
| Admin web | 38 | 5 | 0 | 0 | 0 |
| Mobile app | 0 | 0 | 18 | 0 | 0 |
| Docs | 17 | 1 | 0 | 2 | 2 |

> Lưu ý: bảng này dùng heuristic "người sửa file nhiều nhất". Với các file được nhiều người cùng sửa (ví dụ `SecurityConfig.java`, `OrderService.java`, `App.tsx`), cần đọc thêm §3 để biết ai khởi tạo và ai mở rộng.

---

## 5. Bảng đối chiếu BICAP ticket ↔ người thực hiện

| Ticket | Nội dung | Người thực hiện chính | Hỗ trợ |
| --- | --- | --- | --- |
| BICAP-1 | CRUD tài khoản admin, phân quyền RBAC | Sin | Oanh (UI) |
| BICAP-3 | Phê duyệt/từ chối đăng ký nông trại | Sin | |
| BICAP-4 | Quản lý chi tiết nông trại | Sin | |
| BICAP-5 | Giám sát sản phẩm & danh mục | Sin | |
| BICAP-6 | Quản lý Smart Contract | Sin | |
| BICAP-7 | Đăng ký/đăng nhập (Farm) | Sin | Oanh (FE auth) |
| BICAP-8 / BICAP-9 | Cập nhật hồ sơ / giấy phép nông trại | Oanh, Sin | |
| BICAP-10 / BICAP-11 | Mua & thanh toán gói dịch vụ | TuanAnh | Oanh, Sin |
| BICAP-12 → BICAP-15 | Xem/tạo/cập nhật mùa vụ & quy trình | DuyAn | Sin (UI) |
| BICAP-16 / BICAP-17 | Xuất mùa vụ & sinh QR | ThienAn | |
| BICAP-18 / BICAP-19 | Đăng ký đẩy sản phẩm lên sàn | Sin | |
| BICAP-20 / BICAP-21 | Xử lý yêu cầu mua / thông tin nhà bán lẻ | Sin | |
| BICAP-22 → BICAP-25 | Quy trình & báo cáo vận chuyển (Farm) | Oanh, Sin | |
| BICAP-26 | Thông báo nhiệt độ/độ ẩm/pH | TuanAnh | |
| BICAP-27 | Gửi báo cáo cho Admin | Sin | ThienAn, Oanh |
| BICAP-36 / 37 / 38 | Xác thực & hồ sơ Retailer | ThienAn | Oanh (FE auth) |
| BICAP-39 → 43 | Marketplace Retailer (tìm, chi tiết, QR, đặt mua, đặt cọc) | ThienAn | |
| BICAP-44 / 45 / 46 | Đơn hàng Retailer | ThienAn | DuyAn (API) |
| BICAP-47 / 48 / 49 | Thông báo & tracking Retailer | ThienAn | |
| BICAP-50 → 53 | Hoàn tất giao nhận, upload ảnh, báo cáo | ThienAn | |
| BICAP-54 → 57 | Quản lý lô vận chuyển (Shipping Manager) | Oanh | DuyAn (API) |
| BICAP-58 / 59 | Quản lý phương tiện & tài xế | Oanh | DuyAn |
| BICAP-60 / 61 / 62 | Báo cáo, thông báo, báo cáo từ tài xế | Oanh | Sin |
| BICAP-63 → 68 | Mobile App tài xế | TuanAnh | DuyAn (API `DriverMobileController`) |
| BICAP-69 / 70 / 71 | Guest: thông báo, tìm kiếm, giáo dục | TuanAnh | |
| BICAP-72 | API xác thực & phân quyền | DuyAn | Sin |
| BICAP-73 | API quản lý mùa vụ | DuyAn | |
| BICAP-74 | API tích hợp VeChainThor | Sin | |
| BICAP-75 | API quản lý đơn hàng | DuyAn | ThienAn, Sin |
| BICAP-76 | API quản lý vận chuyển | DuyAn | Oanh |
| BICAP-77 | API thông báo real-time | Sin | TuanAnh (IoT/Guest) |
| BICAP-78 | API thanh toán | TuanAnh | Oanh |
| BICAP-79 | Thiết kế & triển khai CSDL | Sin | |
| BICAP-80 / 81 | NFR blockchain: đồng thời, bảo mật | Sin | |
| BICAP-82 | NFR hạ tầng mở rộng (Docker/Cloud) | Sin, Oanh | *(Docker đã gỡ — xem §8)* |
| BICAP-83 | CI/CD pipeline | TuanAnh (khởi tạo) | Sin (refactor), DuyAn (fix) |
| BICAP-84 | Triển khai Web App | Sin | |
| BICAP-85 | Triển khai Mobile App | TuanAnh | *(đã gỡ — xem §8)* |
| BICAP-86 → 90 | Kiểm thử, UAT | Sin (chủ trì) | TuanAnh, DuyAn, ThienAn |
| BICAP-91 → 98 | Bộ tài liệu (UR, SRS, kiến trúc, thiết kế, testing, install, manual) | Sin | DuyAn, ThienAn, Oanh |

---

## 6. Đóng góp tài liệu

| Tài liệu | Người viết chính |
| --- | --- |
| `user-requirements.md`, `software-requirement-specifications.md`, `requirement.md` | Sin |
| `architecture-design.md`, `detail-design.md` | Sin |
| `system-implementation.md`, `testing-document.md`, `uat-plan.md`, `user-manual.md`, `installation-guide.md`, `run-guide.md` | Sin |
| `BAO-CAO-TONG-HOP.md`, `readme.md` | Sin |
| `module-core-security.md` | DuyAn, Sin |
| `bicap-79-database-setup.md` | Sin |
| `BICAP-73-Farming-Season-Report.md` | DuyAn |
| `docs/sql/bicap-16-17-schema.sql`, `bicap-36-38-schema.sql` | ThienAn |
| `docs/sql/bicap-76-shipment-schema.sql` | DuyAn |
| `docs/sql/bicap-5-categories.sql`, `bicap-18-marketplace-products.sql`, `bicap-27-reports-schema.sql` | Sin |

---

## 7. Giai đoạn phát triển theo thời gian

| Giai đoạn | Nội dung | Thành viên tham gia |
| --- | --- | --- |
| 22/07 → 28/07 | Khởi tạo repo, tài liệu yêu cầu/kiến trúc, package base, CI ban đầu, Auth & RBAC, Admin CRUD | Sin, DuyAn, Oanh, TuanAnh |
| 29/07 → 06/08 | Hoàn thiện Auth đa vai trò, Farm (BICAP-1→7), thanh toán/gói dịch vụ, thông báo IoT, Smart Contract, Retailer auth | Sin, Oanh, TuanAnh, ThienAn |
| 10/08 → 17/08 | Notifications real-time, Product monitoring, Mùa vụ API, Trading Floor, Đơn hàng, Đối tác bán lẻ | Sin, DuyAn |
| 20/08 → 31/08 | Retailer marketplace/orders, API vận chuyển, Guest, thống kê Admin, Redis/VeChain, SRS/DB | ThienAn, TuanAnh, DuyAn, Sin |
| 03/09 → 10/09 | Mobile app tài xế, hoàn tất Shipping Manager, Retailer BICAP-50→53, reports, UAT, readme | TuanAnh, Oanh, ThienAn, Sin |

---

## 8. Ghi chú về lần tái cấu trúc (10/09/2026)

Sau khi hoàn tất các ticket trên, repository đã được **tái cấu trúc** theo yêu cầu của chủ dự án (Sin Nguyen Sun):

- Gộp `frontend/` + `admin-web/` thành **một ứng dụng web duy nhất** `web/`, truy cập theo endpoint: `/` (Farm/Retailer/Shipping/Guest) và `/admin` (quản trị).
- Chuyển backend vào `backend/`, gom công cụ phát triển vào `dev/`.
- **Gỡ bỏ** `mobile-app/` (app mobile tài xế — BICAP-63→68/BICAP-85) và **toàn bộ Docker** (BICAP-82); backend vẫn giữ API `/api/driver/**`.
- Xoá các script `.bat` và file IDE; hướng dẫn chạy chuyển vào `readme.md`.
- CI rút gọn còn 2 job: `web-ci` và `backend-ci`.

Phần việc này do **trợ lý AI thực hiện theo yêu cầu**, không phải đóng góp tính năng của thành viên nào; được ghi lại ở đây để báo cáo phản ánh đúng trạng thái repository.

---

## 9. Hạn chế của báo cáo

- Số liệu lấy từ Git nên **không phản ánh** công việc không commit (họp, thiết kế, review, hỗ trợ).
- Một số commit dùng tên/email khác nhau đã được gộp thủ công; có thể còn sót danh tính chưa nhận diện.
- Nhiều commit được đặt tên chung chung ("updated", "đã fix", "reupdate") nên việc quy kết chức năng dựa một phần vào file thay đổi và mã ticket trong branch.
- Heuristic "primary owner" chỉ mang tính tương đối với các file được nhiều người cùng sửa.
