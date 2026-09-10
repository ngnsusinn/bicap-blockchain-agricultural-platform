# Hướng dẫn Sử dụng (User Manual) — theo từng vai trò

| Thông tin | Chi tiết |
|---|---|
| **Dự án** | BICAP |
| **Mã ticket** | BICAP-98 |
| **Phiên bản** | 1.0 — 30/08/2026 |
| **URL hệ thống (bản 1-port)** | Farm/Retailer/Guest: `http://<server>:8080/` · Admin: `http://<server>:8080/admin/` |

## 0. Đăng nhập & tài khoản demo
Trang chủ có 3 tab: **🌾 Farm · 🛒 Retailer · 🛡️ Admin**. Mỗi tab có hộp **🧪 Tài khoản test — bấm để điền nhanh**.

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Super Admin | `superadmin@bicap.com` | `Superadmin@2026` |
| Admin | `admin@bicap.com` | `Adminpassword@2026` |
| Moderator | `moderator@bicap.com` | `Moderator@2026` |
| Farm Manager | `farm@bicap.com` | `Farmpassword@2026` |
| Retailer | `retailer@bicap.com` | `Retailpassword@2026` |
| Shipping Manager | `shipping_mgr@bicap.com` | `Shipping@2026` |
| Driver | `driver@bicap.com` | `Driver@2026` |

> Đăng nhập bằng tài khoản Admin ở trang chủ sẽ **tự chuyển vào Dashboard Admin**.

---

## 1. Admin (Quản trị hệ thống)

### 1.1 Dashboard Overview
Bảng số liệu: tài khoản admin, nông trại (chờ duyệt/đang hoạt động), sản phẩm, đơn hàng, báo cáo. Bấm thẻ để nhảy tới màn hình tương ứng. Bên phải có **🔔 chuông thông báo** (badge số chưa đọc, bấm để xem/đánh dấu đã đọc).

### 1.2 Quản lý Quản trị viên (BICAP-1)
- Tìm kiếm/lọc theo tên–email–SĐT, trạng thái, vai trò; phân trang.
- **Tạo Admin mới** / **Sửa** / **Xóa** (chỉ SUPER_ADMIN được tạo/xóa; không thể xóa chính mình; xóa là ngầm định đánh dấu NGỪNG HOẠT ĐỘNG).
- Chọn vai trò (SUPER_ADMIN/ADMIN/MODERATOR) — quyền (permissions) tự điền theo vai trò, có thể chỉnh.

### 1.3 Duyệt Đăng Ký Nông Trại (BICAP-3)
- 3 tab **Chờ Duyệt / Đã Duyệt / Bị Từ Chối** kèm số đếm.
- 👁️ xem hồ sơ: chủ sở hữu, diện tích, GPS, **giấy phép & chứng nhận** (link tài liệu).
- ✅ **Phê duyệt** (có cảnh báo xác nhận nếu hồ sơ thiếu giấy phép) / ❌ **Từ chối** (bắt buộc nhập lý do — lý do gửi cho chủ trại).

### 1.4 Quản lý Nông Trại (BICAP-4)
- Lọc theo trạng thái (kể cả Tạm ngưng/Ngừng hoạt động), tìm kiếm.
- Chi tiết: liên hệ, vị trí (mở Google Maps), chứng nhận, **🌱 Lịch sử mùa vụ**, ghi chú nội bộ.
- 🔄 đổi trạng thái hoạt động; ✏️ thêm/sửa ghi chú admin.

### 1.5 Giám Sát Sản Phẩm (BICAP-5)
- Thống kê tổng/đang bán/chờ xem xét/mới trong tuần + phân bố danh mục.
- Lọc danh mục/trạng thái, tìm kiếm; 👁️ chi tiết (mùa vụ, nông trại, QR).
- 🔄 đổi trạng thái sản phẩm (ẩn sản phẩm vi phạm); 🗂️ **Quản lý danh mục** CRUD (không xóa được danh mục còn sản phẩm).

### 1.6 Smart Contracts (BICAP-6)
- Danh sách hợp đồng: tên, địa chỉ, phiên bản, môi trường, trạng thái, txHash (link tra cứu VeChain).
- **Deploy contract mới** (chỉ SUPER_ADMIN): nhập tên, bytecode, ABI, môi trường TESTNET/MAINNET, phiên bản.
- Tab **Giao dịch blockchain**: theo dõi PENDING/CONFIRMED/FAILED + nút **Retry** giao dịch lỗi.

### 1.7 Báo Cáo Người Dùng (BICAP-27)
- Lọc theo trạng thái; thẻ số liệu tổng.
- ⚙️ Mở báo cáo → chọn **Đang xử lý / Đã giải quyết / Từ chối** + nội dung phản hồi (bắt buộc) → người gửi nhận thông báo.

---

## 2. Farm Manager (Chủ trang trại)

### 2.1 Bắt đầu
1. Đăng ký tab **Farm** (hoặc trang Farm của Admin Web) → đăng nhập.
2. **Nông Trại Của Tôi**: tạo hồ sơ farm (chờ admin duyệt) — cập nhật tên/địa chỉ/diện tích/GPS/loại nông sản + tải **giấy phép kinh doanh/chứng nhận** (BICAP-9).
3. **Gói Dịch Vụ**: chọn gói → thanh toán Sepay (mã chuyển khoản + nội dung) → hệ thống tự kích hoạt. Tính năng VIP (mùa vụ, xuất kho, sàn…) khóa 🔒 cho tới khi có gói ACTIVE.

### 2.2 Mùa vụ (BICAP-12→15)
- **Quản Lý Mùa Vụ**: tạo mùa vụ (tên, loại, giống, diện tích, ngày bắt đầu) — ghi blockchain.
- Chọn mùa vụ → xem chi tiết, **thêm bước quy trình** (SOIL_PREP/SEEDING/FERTILIZATION/PEST_CONTROL/HARVESTING + ngày, vật tư, ghi chú) — mỗi bước ghi blockchain.
- Chuyển trạng thái: **HARVESTED** (hệ thống hỏi **sản lượng + đơn vị** — bắt buộc để được xuất kho) hoặc CANCELLED.

### 2.3 Xuất kho & QR (BICAP-16/17)
- **Xuất Kho & QR**: chọn mùa vụ HARVESTED → nhập số lượng/đơn vị/ngày/kho → hệ thống tạo **mã QR truy xuất** (link `/trace/<hash>` công khai) + ghi blockchain.

### 2.4 Sàn giao dịch (BICAP-18/19)
- **Sàn Giao Dịch**: chọn lô xuất đã có QR → điền tên, mô tả (≥50 ký tự), số lượng, giá, danh mục, 1–10 ảnh → **đẩy lên sàn** (trạng thái *Chờ admin duyệt*).
- **Sản Phẩm Đã Đăng**: theo dõi trạng thái từng sản phẩm (PENDING_REVIEW/ACTIVE/…).

### 2.5 Đơn hàng & đối tác (BICAP-20/21)
- **Đơn Hàng**: xem yêu cầu mua từ retailer → ✅ Chấp nhận / ❌ Từ chối (kèm lý do). Đơn chấp nhận chuyển sang chờ đặt cọc → vận chuyển.
- **Nhà Bán Lẻ**: hồ sơ các đối tác đã giao dịch (giấy phép, lịch sử mua).

### 2.6 Vận chuyển & IoT (BICAP-22/23/26)
- **Vận Chuyển**: lô hàng xuất từ trại mình (tài xế, biển số, trạng thái, lịch sử GPS) + **báo cáo tổng hợp** (đúng hạn/giao/trả).
- **Giám Sát IoT**: chỉ số nhiệt độ/độ ẩm/pH trong ngày + cảnh báo.

### 2.7 Khác
- **Chứng Nhận**: xem giấy tờ đã tải lên + hạn dùng.
- **Báo Cáo Cho Admin**: gửi khiếu nại/phản hồi/sự cố; xem phản hồi tại đây.
- **Cài Đặt**: đổi mật khẩu. 🔔 chuông thông báo trên đầu trang.

---

## 3. Retailer (Nhà bán lẻ)

1. Đăng ký tab **Retailer** → xác nhận email (link trong mail) → đăng nhập.
2. **Thông tin cá nhân / Giấy phép kinh doanh**: cập nhật hồ sơ + tải GPKD (BICAP-37/38).
3. **Sàn nông sản**: tìm kiếm/lọc (từ khóa, danh mục, vùng, chứng nhận, giá) → xem chi tiết (hình, mùa vụ, quy trình, nông trại) → **Tạo yêu cầu đặt mua** (số lượng, giá đề xuất, ngày giao, địa chỉ).
4. **Quét QR**: nhập/dán hash hoặc quét → xem toàn bộ hồ sơ truy xuất.
5. **Đơn mua**: theo dõi trạng thái; khi farm chấp nhận → **thanh toán đặt cọc** (Sepay); hủy đơn khi chưa xử lý; xác nhận **giao hoàn tất** khi nhận hàng; tải ảnh sản phẩm đã nhận.
6. Nhận thông báo từ farm & vận chuyển (🔔).

---

## 4. Shipping Manager & Driver

> Màn hình Web Shipping Manager và App Driver đang trong giai đoạn phát triển (issue mở BICAP-54→68). Hiện có sẵn **API** cho mọi luồng: đăng nhập `/api/auth/shipping/login` & `/api/auth/driver/login`, tạo/hủy lô, quản lý xe/tài xế, pickup/tracking/deliver, báo cáo sự cố.

---

## 5. Guest (Khách không đăng nhập)

- Trang chủ → **🔔 Xem thông báo chung (Guest)**: tin nền tảng.
- **Tìm Kiếm Sản Phẩm (BICAP-70)**: duyệt/lọc sản phẩm đang bán.
- **Nội Dung Giáo Dục (BICAP-71)**: bài viết/video nông nghiệp sạch.
- Mở link QR `/trace/<hash>`: xem hồ sơ truy xuất nguồn gốc đầy đủ của lô hàng.

---

## 6. Quy ước trạng thái nhanh

| Miền | Trạng thái |
|---|---|
| Nông trại | PENDING → APPROVED / REJECTED; SUSPENDED / INACTIVE (admin) |
| Mùa vụ | IN_PROGRESS → HARVESTED / CANCELLED |
| Sản phẩm | PENDING_REVIEW → ACTIVE / INACTIVE |
| Đơn hàng | PENDING → ACCEPTED → DEPOSIT_PAID → SHIPPING → DELIVERED → COMPLETED (hoặc CANCELLED) |
| Lô vận chuyển | PICKING_UP → IN_TRANSIT → DELIVERED / RETURNED |
| Báo cáo | OPEN → IN_PROGRESS → RESOLVED / REJECTED |
| Giao dịch blockchain | PENDING → CONFIRMED / FAILED (tự retry ≤3 lần) |

## 7. Mẹo & xử lý sự cố cho người dùng
- Quên mật khẩu: liên hệ Admin đổi (Admin có thể tạo lại tài khoản quản trị).
- Bị khóa tạm thời sau nhiều lần sai mật khẩu: chờ hết thời gian khóa hoặc liên hệ admin.
- Nút 🔒 VIP: cần gói dịch vụ ACTIVE (mục 2.1.3).
- Tải tài liệu: chấp nhận ảnh/PDF, tối đa 10MB/tệp.
