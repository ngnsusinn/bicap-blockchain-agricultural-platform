# Kế hoạch Kiểm thử Chấp nhận Người dùng (UAT — BICAP-90)

| Thông tin | Chi tiết |
|---|---|
| **Phiên bản** | 1.0 — 30/08/2026 |
| **Đối tượng tham gia** | Giảng viên hướng dẫn (PO), trưởng nhóm (PM), đại diện từng vai trò kỹ thuật |
| **Môi trường** | Bản 1-port: `build-web.bat` + `run-backend.bat` → `http://localhost:8080` (H2 + seed) hoặc staging MySQL/Redis |
| **Dữ liệu** | Tài khoản test đã seed (xem `docs/user-manual.md` §0) |
| **Thời lượng dự kiến** | 1 buổi ~2 giờ |

## 1. Điều kiện vào/ra
- **Điều kiện vào:** CI xanh (241 backend + 28 frontend test pass); smoke test `docs/installation-guide.md` §4 đạt; tài liệu user-manual phát cho người test.
- **Điều kiện ra:** ≥90% ca UAT PASS; không còn lỗi Severity Cao/Trung chưa xử lý; chữ ký xác nhận của các bên.

## 2. Kịch bản UAT theo vai trò

### Nhóm A — Farm Manager (8 ca)
| ID | Ca kiểm thử | Các bước | Kết quả mong đợi |
|---|---|---|---|
| UAT-A1 | Đăng ký & đăng nhập | Đăng ký farm mới → đăng nhập | Vào Farm Portal, sidebar đầy đủ |
| UAT-A2 | Tạo farm | Nông Trại Của Tôi → điền hồ sơ + tải GPKD | Farm PENDING, chứng nhận hiển thị ở tab Chứng Nhận |
| UAT-A3 | Mua gói | Gói Dịch Vụ → chọn gói → thanh toán | Sau webhook, tính năng 🔒 mở khóa |
| UAT-A4 | Mùa vụ + blockchain | Tạo mùa vụ → thêm 2 bước quy trình | Mỗi bước có txHash `0x…` |
| UAT-A5 | Harvest có sản lượng | Chuyển HARVESTED → nhập sản lượng | Bắt buộc nhập; season HARVESTED |
| UAT-A6 | Xuất kho + QR | Xuất kho → mở link trace | QR hiển thị, trang trace công khai đúng dữ liệu |
| UAT-A7 | Đẩy lên sàn | Sàn Giao Dịch → form + ảnh | Sản phẩm PENDING_REVIEW trong "Sản Phẩm Đã Đăng" |
| UAT-A8 | Xử lý đơn & báo cáo | Accept đơn của retailer; gửi báo cáo cho admin | Trạng thái đổi; nhận phản hồi admin qua 🔔 |

### Nhóm B — Retailer (6 ca)
| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-B1 | Đăng ký + xác nhận email | Login được sau verify |
| UAT-B2 | Tìm kiếm/lọc sàn | Thấy sản phẩm ACTIVE của farm đã duyệt |
| UAT-B3 | Quét QR / trace | Toàn bộ mùa vụ–quy trình–chứng nhận |
| UAT-B4 | Đặt mua + đặt cọc | Đơn PENDING → ACCEPTED → DEPOSIT_PAID |
| UAT-B5 | Hủy đơn kèm lý do | Farm nhận thông báo |
| UAT-B6 | Xác nhận nhận + tải ảnh | Đơn COMPLETED |

### Nhóm C — Admin (7 ca)
| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-C1 | Login admin từ trang chủ | Vào **thẳng** Dashboard Admin (SSO) |
| UAT-C2 | Dashboard số liệu | Khớp dữ liệu thực (farms chờ, sản phẩm, báo cáo) |
| UAT-C3 | Duyệt/từ chối farm | Từ chối phải có lý do; farm nhận thông báo |
| UAT-C4 | Quản lý farm | Đổi status, ghi chú, xem lịch sử mùa vụ |
| UAT-C5 | Duyệt sản phẩm | PENDING_REVIEW → ACTIVE; ẩn sản phẩm vi phạm |
| UAT-C6 | Quản trị admin | Tạo MODERATOR; thử xóa chính mình → bị chặn |
| UAT-C7 | Xử lý báo cáo | RESOLVED + phản hồi; người gửi nhận 🔔 |

### Nhóm D — Guest (3 ca)
| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-D1 | Thông báo chung | Xem được không cần login |
| UAT-D2 | Tìm kiếm sản phẩm | Kết quả chỉ sản phẩm ACTIVE |
| UAT-D3 | Trang trace | Truy cập mở theo `/trace/<hash>` |

### Nhóm E — Vận hành & phi chức năng (5 ca)
| ID | Ca kiểm thử | Kết quả mong đợi |
|---|---|---|
| UAT-E1 | Đăng nhập sai 5 lần | Tài khoản khóa tạm, thông báo rõ |
| UAT-E2 | F5 mọi deep-link (/, /admin/, /admin/farm, /trace/x) | Không 404 |
| UAT-E3 | Logout ở 1 cổng | Thoát cả 2 cổng (chung origin) |
| UAT-E4 | Chạy `node loadtest/node-loadtest.mjs` | p95 < 800ms, error 0% |
| UAT-E5 | Blockchain live smoke (nếu có testnet key) | Tx PENDING → CONFIRMED trong ~1 phút, thấy trên explorer |

## 3. Mẫu biên bản từng ca
```
Ca: UAT-__ | Người test: ____ | Ngày: ____
Kết quả: ☐ PASS ☐ FAIL ☐ BLOCKED
Ghi chú/tần suất lỗi: ______________________________
Ảnh đính kèm (nếu FAIL): __________________________
```

## 4. Phân loại lỗi
| Severity | Định nghĩa | Xử lý |
|---|---|---|
| Cao | Mất/sai dữ liệu, chặn luồng chính, lỗ hổng bảo mật | Fix trước khi nghiệm thu |
| Trung | Luồng phụ lỗi, có đường vòng | Fix hoặc cam kết lịch |
| Thấp | Thẩm mỹ, gợi ý | Ghi backlog |

## 5. Biên bản tổng kết (mẫu)
| Hạng mục | Giá trị |
|---|---|
| Tổng số ca | 29 |
| PASS / FAIL / BLOCKED | ___ / ___ / ___ |
| Tỷ lệ đạt (điều kiện ra ≥ 90%) | ___% |
| Lỗi Cao/Trung còn tồn | ___ |
| **Kết luận** | ☐ Nghiệm thu ☐ Nghiệm thu có điều kiện ☐ Không đạt |

| Vai trò | Họ tên | Chữ ký | Ngày |
|---|---|---|---|
| PO / Giảng viên | | | |
| PM / Trưởng nhóm | | | |
| Đại diện Backend | | | |
| Đại diện Frontend | | | |

## 6. Hướng dẫn thực hiện nhanh cho người test
1. Chạy hệ thống: `build-web.bat` → `run-backend.bat` → mở `http://localhost:8080/`.
2. Mỗi vai trò dùng **nút điền nhanh tài khoản test** trên form login.
3. Thực hiện theo thứ tự nhóm A → B → C (một luồng xuyên suốt: farm tạo hàng → retailer mua → admin duyệt) → D → E.
4. Ghi biên bản từng ca vào mẫu §3, nộp về PM tổng hợp vào §5.
