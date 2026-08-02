# TÀI LIỆU ĐẶC TẢ YÊU CẦU PHẦN MỀM (SOFTWARE REQUIREMENT SPECIFICATIONS — SRS)

## DỰ ÁN: TÍCH HỢP BLOCKCHAIN TRONG SẢN XUẤT NÔNG SẢN SẠCH (BICAP)

| Thông tin | Chi tiết |
|---|---|
| **Tên dự án (EN)** | Blockchain Integration in Clean Agricultural Production |
| **Tên dự án (VN)** | Tích hợp Blockchain trong sản xuất nông sản sạch |
| **Viết tắt** | BICAP |
| **Loại tài liệu** | Software Requirement Specifications (SRS) |
| **Phiên bản tài liệu** | 1.0 |
| **Ngày tạo** | 23/07/2026 |
| **Trạng thái** | Bản nháp (Draft) |
| **Tham chiếu** | IEEE 830-1998 Standard |

---

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Mô tả tổng quan hệ thống](#2-mô-tả-tổng-quan-hệ-thống)
3. [Yêu cầu giao diện bên ngoài](#3-yêu-cầu-giao-diện-bên-ngoài)
4. [Đặc tả yêu cầu chức năng](#4-đặc-tả-yêu-cầu-chức-năng)
5. [Yêu cầu phi chức năng](#5-yêu-cầu-phi-chức-năng)
6. [Yêu cầu cơ sở dữ liệu](#6-yêu-cầu-cơ-sở-dữ-liệu)
7. [Ràng buộc thiết kế](#7-ràng-buộc-thiết-kế)
8. [Ma trận truy xuất yêu cầu](#8-ma-trận-truy-xuất-yêu-cầu)
9. [Phụ lục](#9-phụ-lục)

---

## 1. Giới thiệu

### 1.1. Mục đích tài liệu

Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này mô tả chi tiết và đầy đủ các yêu cầu chức năng, yêu cầu phi chức năng, giao diện và ràng buộc kỹ thuật của hệ thống BICAP — nền tảng tích hợp Blockchain trong sản xuất nông sản sạch.

Tài liệu SRS được xây dựng dựa trên:
- **Tài liệu yêu cầu dự án (Requirement Document)**
- **Tài liệu yêu cầu người dùng (User Requirements Document v2.0)**

Tài liệu này là cơ sở cho:
- Đội phát triển (Developers) thiết kế và lập trình hệ thống
- Đội kiểm thử (Testers) xây dựng test case
- Đội DevOps triển khai hạ tầng
- Các bên liên quan (Stakeholders) xác nhận và nghiệm thu sản phẩm

### 1.2. Phạm vi tài liệu

Tài liệu bao gồm đặc tả cho **toàn bộ 7 sản phẩm phần mềm** trong hệ thống BICAP:

| STT | Sản phẩm | Nền tảng | Đối tượng sử dụng |
|-----|----------|----------|-------------------|
| 1 | Admin Web App | Web Application | Quản trị viên hệ thống (Admin) |
| 2 | Farm Management Web App | Web Application | Chủ trang trại / Hợp tác xã (Farm Manager) |
| 3 | Retailer Web App | Web Application | Nhà bán lẻ / Nhà phân phối (Retailer) |
| 4 | Shipping Management Web App | Web Application | Quản lý vận chuyển (Shipping Manager) |
| 5 | Shipping Driver Mobile App | Mobile Application | Tài xế vận chuyển (Shipping Driver) |
| 6 | Guest App | Web / Mobile Application | Khách truy cập (Guest) |
| 7 | Backend Web API | Backend Service | Tất cả các module |

### 1.3. Đối tượng đọc tài liệu

| Đối tượng | Mục đích sử dụng |
|-----------|-----------------|
| **Developers** | Hiểu rõ yêu cầu kỹ thuật để thiết kế và triển khai hệ thống |
| **Testers** | Xây dựng test plan và test case dựa trên đặc tả chức năng |
| **Project Manager** | Quản lý phạm vi dự án và theo dõi tiến độ |
| **DevOps Engineers** | Cấu hình hạ tầng, CI/CD và triển khai |
| **Stakeholders / Giảng viên** | Đánh giá và phê duyệt yêu cầu hệ thống |

### 1.4. Định nghĩa, từ viết tắt và thuật ngữ

#### 1.4.1. Từ viết tắt

| Viết tắt | Giải nghĩa |
|----------|------------|
| **BICAP** | Blockchain Integration in Clean Agricultural Production |
| **SRS** | Software Requirement Specifications |
| **API** | Application Programming Interface |
| **RBAC** | Role-Based Access Control (Phân quyền dựa trên vai trò) |
| **CRUD** | Create, Read, Update, Delete |
| **CI/CD** | Continuous Integration / Continuous Deployment |
| **IoT** | Internet of Things |
| **QR Code** | Quick Response Code |
| **NFR** | Non-Functional Requirement |
| **UAT** | User Acceptance Testing |
| **UI** | User Interface |
| **UX** | User Experience |

#### 1.4.2. Thuật ngữ nghiệp vụ

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| **Farming Season (Mùa vụ)** | Một chu kỳ sản xuất nông nghiệp hoàn chỉnh, từ khi gieo trồng đến khi thu hoạch và xuất kho |
| **Trading Floor (Sàn giao dịch)** | Nền tảng trực tuyến nơi trang trại đăng bán sản phẩm và nhà bán lẻ tìm kiếm, đặt mua nông sản |
| **Smart Contract** | Chương trình tự thực thi (self-executing program) chạy trên Blockchain VeChainThor, quản lý logic nghiệp vụ truy xuất nguồn gốc |
| **VeChainThor** | Nền tảng Blockchain công cộng dành cho doanh nghiệp, tối ưu cho quản lý chuỗi cung ứng |
| **Truy xuất nguồn gốc (Traceability)** | Khả năng theo dõi toàn bộ lịch sử sản xuất, vận chuyển của sản phẩm nông sản từ trang trại đến tay người tiêu dùng |
| **Shipment (Lô vận chuyển)** | Đơn vị quản lý vận chuyển hàng hóa từ nông trại đến nhà bán lẻ |
| **Export (Xuất kho)** | Hành động xuất sản phẩm đã thu hoạch ra khỏi kho của trang trại |

### 1.5. Tài liệu tham chiếu

| STT | Tài liệu | Phiên bản | Ghi chú |
|-----|----------|-----------|---------|
| 1 | Requirement Document (requirement.md) | 1.0 | Tài liệu yêu cầu gốc của dự án |
| 2 | User Requirements Document (user-requirements.md) | 2.0 | Yêu cầu chi tiết người dùng, đồng bộ Jira |
| 3 | IEEE 830-1998 | — | Chuẩn viết tài liệu SRS |
| 4 | BICAP Jira Board | — | Quản lý Epic/Story trên Jira |

---

## 2. Mô tả tổng quan hệ thống

### 2.1. Bối cảnh và tầm nhìn

Hệ thống BICAP được phát triển nhằm giải quyết các thách thức mà ngành nông nghiệp Việt Nam đang đối mặt:

**Vấn đề:**
- Các trang trại vừa và nhỏ gặp khó khăn trong giám sát và quản lý quy trình sản xuất
- Thiếu công cụ đáp ứng tiêu chuẩn an toàn thực phẩm một cách minh bạch
- Người tiêu dùng không có cách xác minh nguồn gốc và chất lượng sản phẩm nông sản

**Giải pháp:**
Tận dụng công nghệ Blockchain (VeChainThor) để xây dựng nền tảng **truy xuất nguồn gốc nông sản từ trang trại đến bàn ăn (farm-to-table)**, kết nối trang trại — nhà bán lẻ — người tiêu dùng trên cùng một hệ sinh thái minh bạch.

### 2.2. Các giải pháp cốt lõi

| STT | Giải pháp | Mô tả | Công nghệ chính |
|-----|-----------|-------|-----------------|
| GP-01 | Tối ưu giám sát sản xuất | Giám sát quy trình sản xuất nông nghiệp theo thời gian thực thông qua IoT và hệ thống quản lý | IoT Sensors, Web App |
| GP-02 | Truy xuất nguồn gốc Blockchain | Ghi nhận toàn bộ vòng đời sản phẩm (gieo trồng → chăm sóc → thu hoạch → xuất kho → vận chuyển) lên Blockchain bất biến | VeChainThor, Smart Contract |
| GP-03 | Mã QR truy xuất | Tạo mã QR chứa liên kết đến thông tin truy xuất nguồn gốc trên Blockchain cho mỗi lô hàng xuất kho | QR Generation, Blockchain |
| GP-04 | Phân tích và dự báo | Tối ưu chi phí và hiệu quả sản xuất thông qua phân tích dữ liệu lịch sử và dự báo | Data Analytics |
| GP-05 | Sàn giao dịch kết nối | Kết nối trực tiếp trang trại với nhà phân phối/bán lẻ, hỗ trợ giao dịch trực tuyến | Trading Floor Platform |

### 2.3. Kiến trúc tổng quan hệ thống

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          HỆ THỐNG BICAP                                │
│                                                                         │
│   ┌─────────────────── WEB APPLICATIONS ──────────────────────────┐    │
│   │                                                                │    │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │    │
│   │  │  Admin   │  │   Farm   │  │ Retailer │  │  Shipping    │  │    │
│   │  │ Web App  │  │ Mgmt App │  │ Web App  │  │  Mgmt App    │  │    │
│   │  │(ReactJS) │  │(ReactJS) │  │(ReactJS) │  │ (ReactJS)    │  │    │
│   │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘  │    │
│   └───────┼──────────────┼──────────────┼──────────────┼──────────┘    │
│           │              │              │              │                │
│           └──────────────┴──────────────┴──────────────┘                │
│                                 │                                       │
│                         ┌───────┴────────┐                              │
│                         │  BACKEND API   │                              │
│                         │  (Java Spring  │                              │
│                         │     Boot)      │                              │
│                         └───────┬────────┘                              │
│                                 │                                       │
│             ┌───────────────────┼───────────────────┐                   │
│             │                   │                   │                   │
│      ┌──────┴──────┐   ┌───────┴───────┐   ┌───────┴───────┐          │
│      │ MySQL 5.7.41│   │   Redis 8.6   │   │ VeChainThor   │          │
│      │  Database   │   │    Cache      │   │  Blockchain   │          │
│      └─────────────┘   └───────────────┘   └───────────────┘          │
│                                                                         │
│   ┌─────────────────── MOBILE APPLICATIONS ───────────────────────┐    │
│   │                                                                │    │
│   │  ┌──────────────┐   ┌──────────────┐                          │    │
│   │  │  Ship Driver │   │    Guest     │                          │    │
│   │  │  Mobile App  │   │ Mobile/Web   │                          │    │
│   │  │(React Native)│   │(React Native)│                          │    │
│   │  └──────────────┘   └──────────────┘                          │    │
│   └────────────────────────────────────────────────────────────────┘    │
│                                                                         │
│   ┌─────────────────── EXTERNAL SYSTEMS ──────────────────────────┐    │
│   │                                                                │    │
│   │  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐      │    │
│   │  │ IoT Sensors  │   │  Payment     │   │  Cloud Infra │      │    │
│   │  │(Temp/pH/Hum) │   │  Gateway     │   │ (AWS/GCloud) │      │    │
│   │  └──────────────┘   └──────────────┘   └──────────────┘      │    │
│   └────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.4. Vai trò người dùng (Actors)

| Vai trò | Mã | Nền tảng | Mô tả chi tiết |
|---------|-----|----------|----------------|
| **Admin** | ADM | Web App | Quản trị viên hệ thống — quản lý tài khoản, phê duyệt trang trại, giám sát sản phẩm, triển khai Smart Contract |
| **Farm Manager** | FM | Web App | Chủ trang trại / HTX — đăng ký trang trại, quản lý mùa vụ, giao dịch trên sàn, theo dõi vận chuyển |
| **Retailer** | RT | Web App | Nhà bán lẻ / phân phối — tìm kiếm sản phẩm, đặt mua nông sản, theo dõi đơn hàng |
| **Shipping Manager** | SM | Web App | Quản lý vận chuyển — tạo lô vận chuyển, quản lý phương tiện và tài xế |
| **Shipping Driver** | SD | Mobile App | Tài xế — cập nhật hành trình, xác nhận giao nhận, quét QR Code |
| **Guest** | GS | Web / Mobile | Khách truy cập — tra cứu sản phẩm, xem nội dung giáo dục |

### 2.5. Luồng nghiệp vụ chính (Main Business Flow)

```
  ┌─────────┐    Đăng ký &     ┌─────────────┐   Tạo mùa vụ    ┌──────────────┐
  │  Farm   │──── Xác minh ────│   Admin     │                  │  Blockchain  │
  │ Manager │    trang trại    │  (Phê duyệt) │                  │ (VeChainThor)│
  └────┬────┘                  └─────────────┘                  └──────┬───────┘
       │                                                               │
       │  Quản lý mùa vụ                                              │
       │  (Gieo trồng → Chăm sóc → Thu hoạch)  ──── Ghi lên BC ──────┘
       │                                                               │
       │  Xuất kho + Tạo QR Code  ──────────── Ghi lên BC ────────────┘
       │
       │  Đẩy lên sàn giao dịch
       │
       ▼
  ┌──────────┐    Tìm kiếm &     ┌──────────┐
  │  Sàn     │◄──── Đặt mua ─────│ Retailer │
  │ Giao dịch│                    └─────┬────┘
  └────┬─────┘                          │
       │                                │  Đặt cọc + Xác nhận
       │  Đơn hàng thành công           │
       ▼                                ▼
  ┌──────────────┐    Tạo shipment   ┌──────────────┐
  │  Shipping    │◄──────────────────│  Đơn hàng    │
  │  Manager     │                   │  thành công   │
  └──────┬───────┘                   └──────────────┘
         │  Phân công tài xế
         ▼
  ┌──────────────┐    Lấy hàng      ┌──────────┐    Giao hàng    ┌──────────┐
  │  Shipping    │───── tại ─────────│  Farm    │────── cho ──────│ Retailer │
  │  Driver      │     trang trại   │          │                 │(Xác nhận)│
  └──────────────┘                  └──────────┘                 └──────────┘
                                                                       │
  ┌──────────┐    Quét QR Code                                         │
  │  Guest / │◄──── Truy xuất ──── Sản phẩm đến tay người tiêu dùng ──┘
  │ Consumer │     nguồn gốc
  └──────────┘
```

### 2.6. Giả định và Phụ thuộc

#### Giả định:
- Người dùng Farm Manager và Retailer có kết nối internet ổn định
- Trang trại có giấy phép kinh doanh hợp lệ để đăng ký trên hệ thống
- Thiết bị IoT (cảm biến nhiệt độ, độ ẩm, pH) đã được lắp đặt tại trang trại
- VeChainThor Blockchain network hoạt động ổn định

#### Phụ thuộc:
- VeChainThor blockchain platform phải hoạt động và hỗ trợ deploy Smart Contract
- Cổng thanh toán trực tuyến (payment gateway) đã được tích hợp
- Dịch vụ cloud (AWS/Google Cloud) đã được cấu hình
- Các cảm biến IoT phải gửi được dữ liệu đến API backend

---

## 3. Yêu cầu giao diện bên ngoài

### 3.1. Giao diện người dùng (User Interface)

#### 3.1.1. Web Application (Admin, Farm Management, Retailer, Shipping Manager)

| Thuộc tính | Đặc tả |
|------------|--------|
| **Công nghệ** | ReactJS / Next.js (TypeScript) |
| **Responsive** | Hỗ trợ hiển thị trên Desktop (≥1024px) và Tablet (≥768px) |
| **Trình duyệt** | Chrome (v90+), Firefox (v88+), Safari (v14+), Edge (v90+) |
| **Ngôn ngữ** | Tiếng Việt (mặc định), Tiếng Anh (tùy chọn) |
| **Thiết kế** | Giao diện thân thiện, dễ sử dụng, phù hợp với nông dân có hạn chế kỹ năng công nghệ |
| **Navigation** | Sidebar menu + Breadcrumb navigation |
| **Thông báo** | Real-time notification bell + Toast messages |

#### 3.1.2. Mobile Application (Shipping Driver, Guest)

| Thuộc tính | Đặc tả |
|------------|--------|
| **Công nghệ** | React Native (TypeScript) |
| **Nền tảng** | Android (≥8.0) và iOS (≥14.0) |
| **Camera** | Hỗ trợ quét mã QR Code, chụp ảnh |
| **GPS** | Hỗ trợ theo dõi vị trí tài xế (Shipping Driver) |
| **Offline** | Hỗ trợ cache dữ liệu cơ bản khi mất kết nối |
| **Push Notification** | Hỗ trợ FCM (Firebase Cloud Messaging) |

### 3.2. Giao diện phần cứng (Hardware Interface)

| Thiết bị | Giao diện | Mục đích |
|----------|-----------|----------|
| **Cảm biến IoT** | REST API / MQTT | Thu thập dữ liệu nhiệt độ, độ ẩm, pH từ trang trại |
| **Camera thiết bị di động** | Native Camera API | Quét mã QR Code, chụp ảnh sản phẩm |
| **GPS Module** | Geolocation API | Theo dõi vị trí tài xế vận chuyển |

### 3.3. Giao diện phần mềm (Software Interface)

| Hệ thống bên ngoài | Giao thức | Mục đích |
|--------------------|-----------|----------|
| **VeChainThor Blockchain** | Connex / Thor RESTful API / web3j | Ghi/đọc giao dịch, deploy Smart Contract |
| **Cổng thanh toán** | REST API (HTTPS) | Xử lý thanh toán mua gói dịch vụ, đặt cọc đơn hàng |
| **Dịch vụ Email** | SMTP / Spring Mail API | Gửi email xác nhận, thông báo |
| **Firebase Cloud Messaging** | FCM REST API / Firebase Admin SDK | Push notification cho ứng dụng mobile |
| **Redis 8.6** | Redis Protocol (Spring Data Redis) | Caching dữ liệu, session management |
| **MySQL 5.7.41** | JDBC / Spring Data JPA (Hibernate) | Lưu trữ dữ liệu quan hệ |

### 3.4. Giao diện truyền thông (Communication Interface)

| Giao thức | Mục đích | Chi tiết |
|-----------|----------|----------|
| **HTTPS (TLS 1.2+)** | Truyền tải dữ liệu an toàn | Tất cả API call đều sử dụng HTTPS |
| **WebSocket** | Thông báo real-time | Cập nhật trạng thái đơn hàng, vận chuyển, thông báo IoT |
| **REST API** | Giao tiếp Client-Server | JSON format, RESTful conventions |
| **MQTT** | Giao tiếp IoT | Nhận dữ liệu cảm biến từ thiết bị IoT |

---

## 4. Đặc tả yêu cầu chức năng

### 4.1. Module Admin Web App (EPIC-1)

> **Mô tả module:** Ứng dụng Web dành cho Quản trị viên hệ thống — quản lý tài khoản, phê duyệt nông trại, giám sát sản phẩm, triển khai Smart Contract.

---

#### SRS-ADM-001: Quản lý tài khoản Admin

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-ADM-001 |
| **Tham chiếu UR** | BICAP-1 |
| **Tên** | Quản lý tài khoản Admin |
| **Mô tả** | Hệ thống cho phép Admin tạo, xem, chỉnh sửa, xóa các tài khoản Admin khác và phân quyền vai trò |
| **Độ ưu tiên** | High |
| **Actor** | Admin |

**Đầu vào (Input):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Họ và tên | String (255) | Có | Không rỗng |
| Email | String (255) | Có | Định dạng email hợp lệ, duy nhất trong hệ thống |
| Mật khẩu | String (128) | Có | Tối thiểu 8 ký tự, chứa chữ hoa, chữ thường, số, ký tự đặc biệt |
| Số điện thoại | String (15) | Không | Định dạng số điện thoại VN hợp lệ |
| Vai trò (Role) | Enum | Có | Giá trị: `SUPER_ADMIN`, `ADMIN`, `MODERATOR` |
| Quyền (Permissions) | Array\<String\> | Có | Danh sách permission codes |
| Trạng thái | Enum | Có | Giá trị: `ACTIVE`, `INACTIVE`, `SUSPENDED` |

**Đầu ra (Output):**
- Danh sách tài khoản Admin kèm thông tin và trạng thái
- Thông báo thành công/thất bại khi thao tác CRUD

**Luồng chính (Main Flow):**
1. Admin đăng nhập vào hệ thống
2. Admin truy cập menu "Quản lý tài khoản"
3. Hệ thống hiển thị danh sách tài khoản Admin với phân trang, tìm kiếm, lọc
4. Admin chọn hành động (Tạo mới / Xem / Sửa / Xóa)
5. *Tạo mới:* Admin nhập thông tin → Hệ thống validate → Tạo tài khoản → Gửi email xác nhận
6. *Chỉnh sửa:* Admin chọn tài khoản → Cập nhật thông tin → Hệ thống validate → Lưu thay đổi
7. *Xóa:* Admin chọn tài khoản → Xác nhận xóa → Hệ thống soft-delete (đánh dấu trạng thái `INACTIVE`)

**Luồng ngoại lệ (Exception Flow):**
- E1: Email đã tồn tại → Hiển thị lỗi "Email đã được sử dụng"
- E2: Mật khẩu không đáp ứng yêu cầu → Hiển thị lỗi cụ thể
- E3: Admin cố xóa chính mình → Hiển thị lỗi "Không thể xóa tài khoản đang đăng nhập"
- E4: Admin không có quyền `SUPER_ADMIN` → Không hiển thị nút tạo/xóa Admin khác

**Quy tắc nghiệp vụ:**
- BR1: Chỉ `SUPER_ADMIN` mới có quyền tạo/xóa tài khoản Admin khác
- BR2: Xóa tài khoản là soft-delete (không xóa vật lý khỏi database)
- BR3: Tài khoản mới tạo mặc định trạng thái `ACTIVE`

---

#### SRS-ADM-002: Phê duyệt đăng ký nông trại

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-ADM-002 |
| **Tham chiếu UR** | BICAP-3 |
| **Tên** | Phê duyệt đăng ký nông trại mới |
| **Mô tả** | Hệ thống cho phép Admin xem danh sách nông trại đăng ký mới, xem xét hồ sơ và phê duyệt hoặc từ chối |
| **Độ ưu tiên** | High |
| **Actor** | Admin |

**Đầu vào (Input):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã đăng ký trang trại | UUID | Có | ID của yêu cầu đăng ký cần xử lý |
| Hành động | Enum | Có | Giá trị: `APPROVE`, `REJECT` |
| Lý do từ chối | String (1000) | Có (khi REJECT) | Không rỗng khi từ chối |

**Đầu ra (Output):**
- Danh sách nông trại chờ duyệt kèm hồ sơ đăng ký
- Thông báo kết quả phê duyệt/từ chối
- Email thông báo gửi đến Farm Manager

**Luồng chính (Main Flow):**
1. Admin truy cập menu "Duyệt đăng ký nông trại"
2. Hệ thống hiển thị danh sách nông trại đang chờ duyệt (trạng thái `PENDING`)
3. Admin chọn một nông trại để xem chi tiết hồ sơ
4. Hệ thống hiển thị: tên trang trại, địa chỉ, giấy phép kinh doanh, chứng nhận, thông tin chủ sở hữu
5. Admin chọn "Phê duyệt" hoặc "Từ chối"
6. *Phê duyệt:* Hệ thống cập nhật trạng thái trang trại thành `APPROVED` → Gửi email thông báo
7. *Từ chối:* Admin nhập lý do → Hệ thống cập nhật trạng thái `REJECTED` → Gửi email kèm lý do

**Luồng ngoại lệ (Exception Flow):**
- E1: Hồ sơ thiếu giấy phép kinh doanh → Hệ thống cảnh báo Admin trước khi phê duyệt
- E2: Admin từ chối mà không nhập lý do → Hiển thị lỗi "Vui lòng nhập lý do từ chối"

**Quy tắc nghiệp vụ:**
- BR1: Farm Manager chỉ có thể sử dụng đầy đủ chức năng sau khi được Admin phê duyệt
- BR2: Nông trại bị từ chối có thể nộp lại hồ sơ sau khi bổ sung thông tin

---

#### SRS-ADM-003: Quản lý thông tin nông trại

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-ADM-003 |
| **Tham chiếu UR** | BICAP-4 |
| **Tên** | Quản lý thông tin chi tiết nông trại |
| **Mô tả** | Hệ thống cho phép Admin truy cập, xem và quản lý thông tin chi tiết của tất cả nông trại trên hệ thống |
| **Độ ưu tiên** | Medium |
| **Actor** | Admin |

**Đầu vào (Input):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã nông trại | UUID | Có | ID nông trại cần quản lý |
| Trạng thái hoạt động | Enum | Không | `ACTIVE`, `SUSPENDED`, `INACTIVE` |
| Ghi chú Admin | String (2000) | Không | Ghi chú nội bộ |

**Đầu ra (Output):**
- Danh sách nông trại với bộ lọc (trạng thái, vùng miền, loại sản phẩm)
- Chi tiết nông trại: chứng nhận, liên hệ, vị trí, lịch sử hoạt động, trạng thái

**Luồng chính (Main Flow):**
1. Admin truy cập "Quản lý nông trại"
2. Hệ thống hiển thị danh sách nông trại với phân trang, tìm kiếm theo tên/địa chỉ, lọc theo trạng thái
3. Admin chọn nông trại để xem chi tiết
4. Hệ thống hiển thị thông tin đầy đủ: chứng nhận, giấy phép, liên hệ, vị trí (bản đồ), thống kê mùa vụ
5. Admin có thể thay đổi trạng thái hoạt động hoặc thêm ghi chú

---

#### SRS-ADM-004: Giám sát sản phẩm trên nền tảng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-ADM-004 |
| **Tham chiếu UR** | BICAP-5 |
| **Tên** | Giám sát và quản lý sản phẩm trên nền tảng |
| **Mô tả** | Hệ thống cho phép Admin giám sát tất cả sản phẩm đăng ký, quản lý danh mục, mô tả và đảm bảo tính chính xác dữ liệu |
| **Độ ưu tiên** | Medium |
| **Actor** | Admin |

**Đầu vào (Input):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Từ khóa tìm kiếm | String (255) | Không | Tìm theo tên sản phẩm, trang trại |
| Bộ lọc danh mục | String | Không | Lọc theo danh mục sản phẩm |
| Trạng thái sản phẩm | Enum | Không | `ACTIVE`, `INACTIVE`, `PENDING_REVIEW` |

**Đầu ra (Output):**
- Danh sách sản phẩm với thông tin chi tiết
- Dashboard thống kê: tổng sản phẩm, phân bố danh mục, sản phẩm mới
- Danh mục sản phẩm có thể tạo/sửa/xóa

**Luồng chính (Main Flow):**
1. Admin truy cập "Quản lý sản phẩm"
2. Hệ thống hiển thị dashboard tổng quan: tổng sản phẩm, số lượng theo danh mục, sản phẩm mới
3. Admin có thể xem danh sách sản phẩm, lọc theo danh mục/trạng thái/trang trại
4. Admin có thể quản lý danh mục sản phẩm (CRUD)
5. Admin có thể đánh dấu sản phẩm vi phạm hoặc cần rà soát

---

#### SRS-ADM-005: Quản lý Smart Contract

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-ADM-005 |
| **Tham chiếu UR** | BICAP-6 |
| **Tên** | Triển khai, cập nhật và quản lý Smart Contract trên VeChainThor |
| **Mô tả** | Hệ thống cho phép Admin triển khai (deploy), cập nhật và quản lý Smart Contract trên Blockchain VeChainThor để duy trì tính chính xác và minh bạch của dữ liệu truy xuất nguồn gốc |
| **Độ ưu tiên** | High |
| **Actor** | Admin |

**Đầu vào (Input):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Tên Smart Contract | String (255) | Có | Không rỗng, duy nhất |
| Bytecode (compiled) | Binary | Có | Bytecode đã compile từ Solidity |
| ABI | JSON | Có | ABI hợp lệ |
| Môi trường triển khai | Enum | Có | `TESTNET`, `MAINNET` |
| Phiên bản | String (20) | Có | Semantic versioning (vd: 1.0.0) |

**Đầu ra (Output):**
- Danh sách Smart Contract đã triển khai kèm địa chỉ, phiên bản, trạng thái
- Lịch sử triển khai và cập nhật
- Kết quả giao dịch deploy trên VeChainThor

**Luồng chính (Main Flow):**
1. Admin truy cập "Quản lý Smart Contract"
2. Hệ thống hiển thị danh sách Smart Contract đang hoạt động
3. Admin chọn "Triển khai Smart Contract mới"
4. Admin upload bytecode và ABI, chọn môi trường
5. Hệ thống gửi giao dịch deploy lên VeChainThor
6. Hệ thống trả về địa chỉ contract và transaction hash
7. Admin xác nhận và lưu thông tin contract

**Luồng ngoại lệ (Exception Flow):**
- E1: Không đủ gas (VTHO) để deploy → Hiển thị lỗi "Không đủ VTHO"
- E2: Bytecode không hợp lệ → Hiển thị lỗi "Smart Contract không hợp lệ"
- E3: Mạng VeChainThor không khả dụng → Hiển thị lỗi kết nối

---

### 4.2. Module Farm Management Web App (EPIC-2)

> **Mô tả module:** Ứng dụng Web dành cho Quản lý Nông trại — đăng ký tài khoản, quản lý mùa vụ, giao dịch với Retailer, theo dõi vận chuyển.

---

#### SRS-FM-001: Đăng ký và Đăng nhập tài khoản

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-001 |
| **Tham chiếu UR** | BICAP-7 |
| **Tên** | Đăng ký và đăng nhập tài khoản Farm Manager |
| **Mô tả** | Hệ thống cho phép chủ trang trại đăng ký tài khoản mới bằng email/số điện thoại và đăng nhập để sử dụng hệ thống |
| **Độ ưu tiên** | High |
| **Actor** | Farm Manager |

**Đầu vào — Đăng ký (Registration Input):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Họ và tên | String (255) | Có | Không rỗng, tối thiểu 2 ký tự |
| Email | String (255) | Có | Định dạng email hợp lệ, duy nhất |
| Số điện thoại | String (15) | Có | Định dạng VN: 0[3|5|7|8|9]xxxxxxxx |
| Mật khẩu | String (128) | Có | Tối thiểu 8 ký tự, chứa chữ hoa + chữ thường + số + ký tự đặc biệt |
| Xác nhận mật khẩu | String (128) | Có | Phải trùng với Mật khẩu |

**Đầu vào — Đăng nhập (Login Input):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Email hoặc Số điện thoại | String | Có | Tài khoản đã đăng ký |
| Mật khẩu | String | Có | Mật khẩu đúng |

**Đầu ra (Output):**
- *Đăng ký thành công:* Email xác nhận, chuyển đến trang cập nhật hồ sơ
- *Đăng nhập thành công:* JWT Token (Access Token + Refresh Token), chuyển đến Dashboard
- *Thất bại:* Thông báo lỗi cụ thể

**Luồng chính — Đăng ký:**
1. Người dùng truy cập trang đăng ký
2. Nhập thông tin: họ tên, email, số điện thoại, mật khẩu
3. Hệ thống validate dữ liệu đầu vào
4. Hệ thống kiểm tra email/số điện thoại chưa tồn tại
5. Tạo tài khoản với trạng thái `PENDING_VERIFICATION`
6. Gửi email xác nhận (verification link)
7. Người dùng click link xác nhận → Tài khoản chuyển sang `ACTIVE` (chờ phê duyệt nông trại)

**Luồng chính — Đăng nhập:**
1. Người dùng nhập email/SĐT + mật khẩu
2. Hệ thống xác thực thông tin
3. Tạo JWT Token (Access Token: 15 phút, Refresh Token: 7 ngày)
4. Trả về token và chuyển hướng đến Dashboard

**Luồng ngoại lệ:**
- E1: Email đã tồn tại → "Email đã được đăng ký"
- E2: Sai mật khẩu → "Email hoặc mật khẩu không đúng"
- E3: Tài khoản bị khóa → "Tài khoản đã bị tạm ngưng"
- E4: Đăng nhập sai 5 lần liên tiếp → Khóa tài khoản 30 phút

**Quy tắc nghiệp vụ:**
- BR1: Mỗi email chỉ được đăng ký 1 tài khoản
- BR2: Mật khẩu được hash bằng bcrypt trước khi lưu vào database
- BR3: Token được lưu ở phía client (localStorage hoặc httpOnly cookie)

---

#### SRS-FM-002: Cập nhật thông tin cá nhân

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-002 |
| **Tham chiếu UR** | BICAP-8 |
| **Tên** | Cập nhật thông tin cá nhân chủ trang trại |
| **Mô tả** | Hệ thống cho phép chủ trang trại cập nhật thông tin cá nhân bao gồm họ tên, SĐT, địa chỉ, ảnh đại diện |
| **Độ ưu tiên** | Medium |
| **Actor** | Farm Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Họ và tên | String (255) | Có | Không rỗng |
| Số điện thoại | String (15) | Có | Định dạng VN hợp lệ |
| Địa chỉ | String (500) | Không | — |
| Ảnh đại diện | File (Image) | Không | JPG/PNG, tối đa 5MB |

---

#### SRS-FM-003: Cập nhật giấy phép kinh doanh và thông tin nông trại

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-003 |
| **Tham chiếu UR** | BICAP-9 |
| **Tên** | Cập nhật giấy phép kinh doanh và thông tin nông trại |
| **Mô tả** | Hệ thống cho phép chủ trang trại tải lên Giấy phép kinh doanh và cập nhật thông tin nông trại để Admin xác minh |
| **Độ ưu tiên** | Medium |
| **Actor** | Farm Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Tên nông trại | String (255) | Có | Không rỗng |
| Địa chỉ nông trại | String (500) | Có | Không rỗng |
| Diện tích (ha) | Decimal | Có | > 0 |
| Loại cây trồng/vật nuôi | Array\<String\> | Có | Chọn từ danh mục hệ thống |
| Giấy phép kinh doanh | File (PDF/Image) | Có | PDF/JPG/PNG, tối đa 10MB |
| Chứng nhận (nếu có) | Array\<File\> | Không | PDF/JPG/PNG, mỗi file tối đa 10MB |
| Mô tả nông trại | String (2000) | Không | — |
| Tọa độ GPS | Object {lat, lng} | Không | Tọa độ hợp lệ |

**Quy tắc nghiệp vụ:**
- BR1: Sau khi cập nhật giấy phép, trạng thái nông trại chuyển thành `PENDING_APPROVAL`
- BR2: Admin sẽ nhận thông báo để duyệt hồ sơ nông trại

---

#### SRS-FM-004: Mua gói dịch vụ

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-004 |
| **Tham chiếu UR** | BICAP-10 |
| **Tên** | Mua gói dịch vụ sử dụng |
| **Mô tả** | Hệ thống cho phép chủ trang trại xem danh sách gói dịch vụ và chọn mua gói phù hợp |
| **Độ ưu tiên** | High |
| **Actor** | Farm Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã gói dịch vụ | UUID | Có | Gói dịch vụ phải tồn tại và còn hiệu lực |

**Đầu ra:**
- Danh sách gói dịch vụ kèm thông tin: tên, mô tả, giá, thời hạn, tính năng bao gồm
- Gói hiện tại của Farm Manager (nếu có)

**Luồng chính:**
1. Farm Manager truy cập "Gói dịch vụ"
2. Hệ thống hiển thị danh sách gói: tên, giá, thời hạn, so sánh tính năng
3. Farm Manager chọn gói mong muốn
4. Hệ thống hiển thị chi tiết gói và nút "Mua ngay"
5. Farm Manager xác nhận → Chuyển sang bước thanh toán (SRS-FM-005)

---

#### SRS-FM-005: Thanh toán gói dịch vụ

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-005 |
| **Tham chiếu UR** | BICAP-11 |
| **Tên** | Thanh toán khi mua gói dịch vụ |
| **Mô tả** | Hệ thống hỗ trợ thanh toán gói dịch vụ qua chuyển khoản ngân hàng hoặc ví điện tử |
| **Độ ưu tiên** | High |
| **Actor** | Farm Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã gói dịch vụ | UUID | Có | Từ bước SRS-FM-004 |
| Phương thức thanh toán | Enum | Có | `BANK_TRANSFER`, `E_WALLET` |
| Thông tin thanh toán | Object | Có | Phụ thuộc phương thức (số tài khoản, ví, ...) |

**Đầu ra:**
- Mã giao dịch thanh toán
- Trạng thái thanh toán: `PENDING`, `SUCCESS`, `FAILED`
- Hóa đơn điện tử (khi thành công)

**Luồng chính:**
1. Farm Manager chọn phương thức thanh toán
2. Hệ thống chuyển hướng đến cổng thanh toán
3. Farm Manager thực hiện thanh toán
4. Cổng thanh toán trả kết quả (callback)
5. Thanh toán thành công → Kích hoạt gói dịch vụ → Gửi hóa đơn qua email
6. Thanh toán thất bại → Hiển thị lỗi, cho phép thử lại

---

#### SRS-FM-006: Xem danh sách quy trình mùa vụ

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-006 |
| **Tham chiếu UR** | BICAP-12 |
| **Tên** | Xem danh sách các quy trình mùa vụ |
| **Mô tả** | Hệ thống hiển thị danh sách tất cả mùa vụ kèm trạng thái để Farm Manager có cái nhìn tổng quan |
| **Độ ưu tiên** | Medium |
| **Actor** | Farm Manager |

**Đầu ra:**
- Danh sách mùa vụ với phân trang (20 items/page)
- Thông tin mỗi mùa vụ: tên, loại sản phẩm, ngày bắt đầu, trạng thái, tiến trình (%)
- Bộ lọc: trạng thái (`IN_PROGRESS`, `HARVESTED`, `EXPORTED`), khoảng thời gian, loại sản phẩm
- Tìm kiếm theo tên mùa vụ

---

#### SRS-FM-007: Xem chi tiết mùa vụ

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-007 |
| **Tham chiếu UR** | BICAP-13 |
| **Tên** | Xem chi tiết một mùa vụ |
| **Mô tả** | Hệ thống hiển thị toàn bộ thông tin chi tiết của một mùa vụ bao gồm giống cây, timeline, quy trình, vật tư sử dụng |
| **Độ ưu tiên** | Medium |
| **Actor** | Farm Manager |

**Đầu ra:**
| Phần | Nội dung hiển thị |
|------|-------------------|
| Thông tin chung | Tên mùa vụ, loại sản phẩm, giống, diện tích, ngày bắt đầu, ngày kết thúc dự kiến, trạng thái |
| Timeline quy trình | Danh sách các bước đã thực hiện (bón phân, phun thuốc, tưới nước...) theo thời gian |
| Vật tư sử dụng | Phân bón, thuốc BVTV, giống — số lượng, ngày sử dụng |
| Hình ảnh minh chứng | Gallery ảnh kèm ngày chụp và mô tả |
| Dữ liệu IoT | Biểu đồ nhiệt độ, độ ẩm, pH theo thời gian |
| Blockchain | Transaction hash, link xem trên VeChain Explorer |

---

#### SRS-FM-008: Tạo mùa vụ mới (Ghi Blockchain)

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-008 |
| **Tham chiếu UR** | BICAP-14 |
| **Tên** | Tạo mùa vụ mới và ghi lên Blockchain |
| **Mô tả** | Hệ thống cho phép tạo mùa vụ mới. **Thông tin mùa vụ được ghi lên VeChainThor Blockchain** để đảm bảo tính minh bạch và bất biến |
| **Độ ưu tiên** | High |
| **Actor** | Farm Manager |
| **Tích hợp** | VeChainThor Blockchain |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Tên mùa vụ | String (255) | Có | Không rỗng |
| Loại sản phẩm | Enum | Có | Chọn từ danh mục hệ thống |
| Giống cây/vật nuôi | String (255) | Có | Không rỗng |
| Diện tích canh tác (ha) | Decimal | Có | > 0 |
| Ngày bắt đầu | Date | Có | ≥ ngày hiện tại |
| Ngày kết thúc dự kiến | Date | Không | > Ngày bắt đầu |
| Quy trình dự kiến | Array\<Object\> | Có | Ít nhất 1 bước quy trình |
| Mô tả | String (2000) | Không | — |

**Đầu ra:**
- Mùa vụ mới được tạo trong database
- Transaction hash từ VeChainThor
- Thông báo "Mùa vụ đã được tạo và ghi lên Blockchain thành công"

**Luồng chính:**
1. Farm Manager truy cập "Tạo mùa vụ mới"
2. Nhập thông tin mùa vụ: tên, loại sản phẩm, giống, diện tích, ngày bắt đầu
3. Nhập các bước quy trình dự kiến (bón lót, gieo trồng, bón thúc, phun thuốc, thu hoạch...)
4. Hệ thống validate dữ liệu
5. Hệ thống lưu vào MySQL database
6. **Hệ thống gọi Smart Contract trên VeChainThor để ghi thông tin mùa vụ**
7. Smart Contract trả về transaction hash
8. Hệ thống lưu transaction hash vào database, liên kết với mùa vụ
9. Hiển thị thông báo thành công kèm link xem giao dịch trên VeChain Explorer

**Luồng ngoại lệ:**
- E1: Giao dịch Blockchain thất bại → Hệ thống thử lại 3 lần, nếu vẫn thất bại → lưu vào hàng đợi (queue) để xử lý sau → Thông báo Farm Manager
- E2: Nông trại chưa được phê duyệt → "Nông trại chưa được Admin phê duyệt"
- E3: Gói dịch vụ đã hết hạn → "Vui lòng gia hạn gói dịch vụ"

**Quy tắc nghiệp vụ:**
- BR1: Dữ liệu ghi lên Blockchain là **bất biến** (immutable), không thể chỉnh sửa sau khi ghi
- BR2: Farm Manager phải có gói dịch vụ còn hiệu lực để tạo mùa vụ
- BR3: Nông trại phải ở trạng thái `APPROVED`

---

#### SRS-FM-009: Cập nhật quy trình mùa vụ (Ghi Blockchain)

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-009 |
| **Tham chiếu UR** | BICAP-15 |
| **Tên** | Cập nhật quy trình mùa vụ và ghi lên Blockchain |
| **Mô tả** | Hệ thống cho phép Farm Manager cập nhật các bước quy trình trong mùa vụ. **Mỗi lần cập nhật được ghi lên Blockchain** tạo nhật ký sản xuất bất biến |
| **Độ ưu tiên** | High |
| **Actor** | Farm Manager |
| **Tích hợp** | VeChainThor Blockchain |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã mùa vụ | UUID | Có | Mùa vụ tồn tại, trạng thái `IN_PROGRESS` |
| Tên bước quy trình | String (255) | Có | Không rỗng |
| Loại hoạt động | Enum | Có | `FERTILIZING`, `SPRAYING`, `WATERING`, `HARVESTING`, `OTHER` |
| Ngày thực hiện | DateTime | Có | ≤ ngày hiện tại |
| Vật tư sử dụng | Array\<Object\> | Không | Tên vật tư, số lượng, đơn vị |
| Hình ảnh minh chứng | Array\<File\> | Không | JPG/PNG, mỗi file tối đa 5MB, tối đa 10 ảnh |
| Ghi chú | String (2000) | Không | — |

**Đầu ra:**
- Bước quy trình mới được thêm vào timeline mùa vụ
- Transaction hash từ VeChainThor
- Thông báo thành công

**Quy tắc nghiệp vụ:**
- BR1: Chỉ cập nhật được mùa vụ có trạng thái `IN_PROGRESS`
- BR2: Mỗi lần cập nhật tạo một bản ghi mới trên Blockchain (không ghi đè)
- BR3: Dữ liệu IoT (nhiệt độ, độ ẩm, pH) tự động được ghi kèm nếu có cảm biến

---

#### SRS-FM-010: Xuất kho mùa vụ

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-010 |
| **Tham chiếu UR** | BICAP-16 |
| **Tên** | Xuất kho (export) một mùa vụ đã thu hoạch |
| **Mô tả** | Hệ thống cho phép Farm Manager thực hiện xuất kho cho mùa vụ đã thu hoạch |
| **Độ ưu tiên** | Medium |
| **Actor** | Farm Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã mùa vụ | UUID | Có | Mùa vụ trạng thái `HARVESTED` |
| Số lượng xuất | Decimal | Có | > 0, ≤ số lượng thu hoạch |
| Đơn vị | String | Có | kg, tấn, ... |
| Ngày xuất | DateTime | Có | ≤ ngày hiện tại |
| Kho xuất | String (255) | Có | Không rỗng |

---

#### SRS-FM-011: Tạo mã QR Code cho xuất kho (Ghi Blockchain)

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-011 |
| **Tham chiếu UR** | BICAP-17 |
| **Tên** | Tạo mã QR Code cho mỗi lần xuất kho và ghi lên Blockchain |
| **Mô tả** | Hệ thống tự động tạo mã QR Code cho mỗi lần xuất kho. QR chứa liên kết đến thông tin truy xuất nguồn gốc trên Blockchain |
| **Độ ưu tiên** | High |
| **Actor** | Farm Manager |
| **Tích hợp** | VeChainThor Blockchain |

**Đầu ra:**
- Mã QR Code dưới dạng hình ảnh (PNG/SVG)
- URL truy xuất nguồn gốc: `https://bicap.vn/trace/{transaction_hash}`
- Transaction hash trên VeChainThor

**Nội dung QR Code trỏ đến:**
| Thông tin | Mô tả |
|-----------|-------|
| Tên sản phẩm | Tên mùa vụ + giống |
| Trang trại | Tên, địa chỉ, chứng nhận |
| Quy trình mùa vụ | Timeline đầy đủ các bước canh tác |
| Vật tư sử dụng | Phân bón, thuốc BVTV |
| Dữ liệu IoT | Nhiệt độ, độ ẩm, pH trung bình |
| Hình ảnh | Gallery hình ảnh minh chứng |
| Ngày thu hoạch / xuất kho | Timestamp |
| Blockchain hash | Link xem trên VeChain Explorer |

**Quy tắc nghiệp vụ:**
- BR1: Mỗi lần xuất kho tạo **một mã QR duy nhất**
- BR2: Thông tin xuất kho được **ghi lên Blockchain** trước khi tạo QR
- BR3: QR Code có thể in ra dán lên sản phẩm / bao bì

---

#### SRS-FM-012: Đăng ký đẩy sản phẩm lên sàn giao dịch

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-012 |
| **Tham chiếu UR** | BICAP-18 |
| **Tên** | Đăng ký đẩy sản phẩm lên sàn giao dịch |
| **Mô tả** | Hệ thống cho phép Farm Manager đăng sản phẩm đã xuất kho lên sàn giao dịch để Retailer tìm kiếm và đặt mua |
| **Độ ưu tiên** | Medium |
| **Actor** | Farm Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã lô hàng (export) | UUID | Có | Lô hàng đã xuất kho |
| Tên sản phẩm | String (255) | Có | Không rỗng |
| Mô tả sản phẩm | String (2000) | Có | Tối thiểu 50 ký tự |
| Số lượng bán | Decimal | Có | > 0 |
| Đơn giá dự kiến | Decimal | Có | > 0 (VND) |
| Hình ảnh sản phẩm | Array\<File\> | Có | Tối thiểu 1 ảnh, tối đa 10 ảnh |
| Mã QR truy xuất | String | Tự động | Từ lô hàng xuất kho |

---

#### SRS-FM-013: Xem trạng thái đăng ký sàn giao dịch

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-013 |
| **Tham chiếu UR** | BICAP-19 |
| **Tên** | Xem trạng thái đăng ký đẩy sản phẩm lên sàn |
| **Mô tả** | Hệ thống hiển thị danh sách sản phẩm đã đăng ký lên sàn kèm trạng thái |
| **Độ ưu tiên** | Low |
| **Actor** | Farm Manager |

**Đầu ra:**
- Danh sách sản phẩm kèm trạng thái: `PENDING_REVIEW`, `ON_SALE`, `SOLD_OUT`, `REJECTED`
- Thống kê: số sản phẩm đang bán, số lượt xem, số yêu cầu mua

---

#### SRS-FM-014: Xử lý yêu cầu mua nông sản từ Retailer

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-014 |
| **Tham chiếu UR** | BICAP-20 |
| **Tên** | Xử lý yêu cầu mua nông sản từ Nhà bán lẻ |
| **Mô tả** | Hệ thống cho phép Farm Manager xem và xử lý (chấp nhận/từ chối) các yêu cầu mua hàng từ Retailer |
| **Độ ưu tiên** | High |
| **Actor** | Farm Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã yêu cầu mua | UUID | Có | Yêu cầu tồn tại, trạng thái `PENDING` |
| Hành động | Enum | Có | `ACCEPT`, `REJECT` |
| Lý do từ chối | String (1000) | Có (khi REJECT) | Không rỗng khi từ chối |

**Luồng chính:**
1. Farm Manager xem danh sách yêu cầu mua hàng (trạng thái `PENDING`)
2. Chọn yêu cầu để xem chi tiết: thông tin Retailer, sản phẩm, số lượng, giá đề xuất
3. Chấp nhận → Đơn hàng chuyển sang `ACCEPTED` → Retailer được thông báo → Chờ đặt cọc
4. Từ chối → Nhập lý do → Đơn chuyển sang `REJECTED` → Retailer được thông báo

**Quy tắc nghiệp vụ:**
- BR1: Khi chấp nhận, Retailer có 24h để đặt cọc, quá hạn thì đơn tự động hủy
- BR2: Farm Manager không thể chấp nhận nếu số lượng yêu cầu > số lượng tồn kho

---

#### SRS-FM-015: Xem thông tin Retailer đã ký hợp đồng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-015 |
| **Tham chiếu UR** | BICAP-21 |
| **Tên** | Xem thông tin Nhà bán lẻ đã ký hợp đồng |
| **Mô tả** | Hệ thống hiển thị thông tin chi tiết của các Retailer đã ký hợp đồng mua hàng |
| **Độ ưu tiên** | Medium |

---

#### SRS-FM-016: Xem quy trình vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-016 |
| **Tham chiếu UR** | BICAP-22 |
| **Tên** | Xem danh sách và chi tiết quy trình vận chuyển |
| **Mô tả** | Hệ thống hiển thị danh sách và chi tiết các đơn vận chuyển liên quan đến sản phẩm của Farm Manager |
| **Độ ưu tiên** | Medium |

---

#### SRS-FM-017: Xem báo cáo vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-017 |
| **Tham chiếu UR** | BICAP-23 |
| **Tên** | Xem báo cáo tổng hợp quy trình vận chuyển |
| **Mô tả** | Hệ thống hiển thị báo cáo tổng hợp về quá trình vận chuyển: số đơn hoàn thành, tỷ lệ đúng hạn, sự cố |
| **Độ ưu tiên** | Low |

---

#### SRS-FM-018: Nhận thông báo từ Retailer

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-018 |
| **Tham chiếu UR** | BICAP-24 |
| **Tên** | Nhận thông báo báo cáo từ Nhà bán lẻ |
| **Mô tả** | Hệ thống gửi thông báo real-time cho Farm Manager khi Retailer gửi báo cáo, yêu cầu hoặc phản hồi |
| **Độ ưu tiên** | Low |

**Loại thông báo:**
| Loại | Trigger | Kênh |
|------|---------|------|
| Yêu cầu mua hàng mới | Retailer tạo yêu cầu | In-app + Email |
| Retailer hủy đơn | Retailer hủy yêu cầu | In-app + Email |
| Báo cáo sản phẩm | Retailer gửi phản hồi | In-app |
| Thương lượng | Retailer gửi tin nhắn | In-app |

---

#### SRS-FM-019: Nhận thông báo từ Shipper

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-019 |
| **Tham chiếu UR** | BICAP-25 |
| **Tên** | Nhận thông báo từ Người vận chuyển |
| **Mô tả** | Hệ thống gửi thông báo real-time khi Shipper cập nhật trạng thái vận chuyển |
| **Độ ưu tiên** | Low |

---

#### SRS-FM-020: Nhận thông báo IoT

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-020 |
| **Tham chiếu UR** | BICAP-26 |
| **Tên** | Nhận thông báo nhiệt độ, độ ẩm, pH trong ngày |
| **Mô tả** | Hệ thống gửi thông báo tự động từ cảm biến IoT về các chỉ số môi trường |
| **Độ ưu tiên** | Medium |
| **Tích hợp** | IoT Sensors |

**Đặc tả chi tiết:**
| Chỉ số | Đơn vị | Tần suất đo | Ngưỡng cảnh báo |
|--------|--------|-------------|-----------------|
| Nhiệt độ | °C | Mỗi 30 phút | < 15°C hoặc > 40°C |
| Độ ẩm | % | Mỗi 30 phút | < 30% hoặc > 90% |
| Độ pH | pH | Mỗi 1 giờ | < 5.5 hoặc > 7.5 |

**Loại thông báo:**
- **Thông báo định kỳ:** Tổng hợp cuối ngày gửi qua In-app
- **Cảnh báo khẩn:** Khi chỉ số vượt ngưỡng → Push notification + In-app ngay lập tức

---

#### SRS-FM-021: Gửi báo cáo cho Admin

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-FM-021 |
| **Tham chiếu UR** | BICAP-27 |
| **Tên** | Gửi báo cáo (khiếu nại, phản hồi, sự cố) cho Admin |
| **Mô tả** | Hệ thống cho phép Farm Manager gửi báo cáo đến Admin hệ thống |
| **Độ ưu tiên** | Low |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Loại báo cáo | Enum | Có | `COMPLAINT`, `FEEDBACK`, `INCIDENT`, `OTHER` |
| Tiêu đề | String (255) | Có | Không rỗng |
| Nội dung | String (5000) | Có | Tối thiểu 20 ký tự |
| Hình ảnh đính kèm | Array\<File\> | Không | Tối đa 5 ảnh, mỗi ảnh ≤ 5MB |

---

### 4.3. Module Retailer Web App (EPIC-3)

> **Mô tả module:** Ứng dụng Web dành cho Nhà bán lẻ — tìm kiếm, đặt mua nông sản, theo dõi đơn hàng & vận chuyển.

---

#### SRS-RT-001: Đăng ký và Đăng nhập tài khoản

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-001 |
| **Tham chiếu UR** | BICAP-36 |
| **Tên** | Đăng ký và đăng nhập tài khoản Retailer |
| **Mô tả** | Tương tự SRS-FM-001 nhưng dành cho vai trò Retailer |
| **Độ ưu tiên** | High |
| **Actor** | Retailer |

> *Đặc tả chi tiết về đầu vào, đầu ra, luồng chính, luồng ngoại lệ tương tự SRS-FM-001, thay đổi role thành `RETAILER`.*

---

#### SRS-RT-002: Cập nhật thông tin cá nhân

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-002 |
| **Tham chiếu UR** | BICAP-37 |
| **Tên** | Cập nhật thông tin cá nhân Retailer |
| **Mô tả** | Tương tự SRS-FM-002 |
| **Độ ưu tiên** | Medium |

---

#### SRS-RT-003: Cập nhật Giấy phép kinh doanh

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-003 |
| **Tham chiếu UR** | BICAP-38 |
| **Tên** | Cập nhật Giấy phép kinh doanh và thông tin cửa hàng |
| **Mô tả** | Hệ thống cho phép Retailer tải lên giấy phép kinh doanh và cập nhật thông tin doanh nghiệp |
| **Độ ưu tiên** | Medium |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Tên cửa hàng / doanh nghiệp | String (255) | Có | Không rỗng |
| Địa chỉ | String (500) | Có | Không rỗng |
| Loại hình kinh doanh | Enum | Có | `RETAIL_STORE`, `WHOLESALE`, `SUPERMARKET`, `OTHER` |
| Giấy phép kinh doanh | File (PDF/Image) | Có | PDF/JPG/PNG, tối đa 10MB |

---

#### SRS-RT-004: Tìm kiếm nông sản trên sàn giao dịch

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-004 |
| **Tham chiếu UR** | BICAP-39 |
| **Tên** | Tìm kiếm nông sản trên sàn giao dịch |
| **Mô tả** | Hệ thống cho phép Retailer tìm kiếm nông sản theo nhiều tiêu chí |
| **Độ ưu tiên** | High |
| **Actor** | Retailer |

**Đầu vào — Bộ lọc tìm kiếm:**
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
|--------|--------------|----------|-------|
| Từ khóa | String (255) | Không | Tìm theo tên sản phẩm, trang trại |
| Loại sản phẩm | Enum | Không | Lọc theo danh mục (rau, củ, quả, ...) |
| Vùng miền | Enum | Không | Bắc, Trung, Nam + tỉnh/thành |
| Chứng nhận | Array\<Enum\> | Không | VietGAP, GlobalGAP, Organic, ... |
| Khoảng giá | Object {min, max} | Không | Min ≥ 0, Max > Min (VND) |
| Tình trạng | Enum | Không | `AVAILABLE`, `SOLD_OUT` |
| Sắp xếp | Enum | Không | `PRICE_ASC`, `PRICE_DESC`, `NEWEST`, `RATING` |

**Đầu ra:**
- Danh sách sản phẩm kèm: ảnh thumbnail, tên, giá, trang trại, vùng miền, chứng nhận
- Phân trang (20 items/page)
- Tổng số kết quả

---

#### SRS-RT-005: Xem chi tiết nông sản

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-005 |
| **Tham chiếu UR** | BICAP-40 |
| **Tên** | Xem chi tiết thông tin nông sản |
| **Mô tả** | Hệ thống hiển thị đầy đủ thông tin sản phẩm: mô tả, hình ảnh, giá, trang trại, lịch sử mùa vụ |
| **Độ ưu tiên** | Medium |

---

#### SRS-RT-006: Quét QR Code truy xuất nguồn gốc

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-006 |
| **Tham chiếu UR** | BICAP-41 |
| **Tên** | Quét mã QR Code để xem thông tin truy xuất nguồn gốc từ Blockchain |
| **Mô tả** | Hệ thống cho phép Retailer quét QR Code để xem toàn bộ thông tin truy xuất nguồn gốc được ghi trên VeChainThor |
| **Độ ưu tiên** | High |
| **Actor** | Retailer |
| **Tích hợp** | VeChainThor Blockchain, Camera |

**Luồng chính:**
1. Retailer mở chức năng "Quét QR" trên Web App
2. Hệ thống kích hoạt camera / cho phép upload ảnh QR
3. Hệ thống decode QR → lấy transaction hash / URL truy xuất
4. Hệ thống truy vấn Smart Contract trên VeChainThor bằng transaction hash
5. Hiển thị thông tin truy xuất: quy trình mùa vụ, phân bón/thuốc, ngày thu hoạch, chứng nhận, hình ảnh
6. Hiển thị badge xác thực "Đã xác minh trên Blockchain ✓"

---

#### SRS-RT-007: Tạo yêu cầu đặt mua nông sản

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-007 |
| **Tham chiếu UR** | BICAP-42 |
| **Tên** | Tạo yêu cầu đặt mua nông sản |
| **Mô tả** | Hệ thống cho phép Retailer tạo yêu cầu mua nông sản gửi đến Farm Manager |
| **Độ ưu tiên** | High |
| **Actor** | Retailer |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã sản phẩm | UUID | Có | Sản phẩm đang ON_SALE |
| Số lượng | Decimal | Có | > 0, ≤ số lượng còn |
| Giá đề xuất | Decimal | Có | > 0 (VND) |
| Thời gian giao hàng mong muốn | Date | Có | ≥ ngày hiện tại + 1 |
| Địa chỉ nhận hàng | String (500) | Có | Không rỗng |
| Ghi chú | String (2000) | Không | — |

**Luồng chính:**
1. Retailer xem chi tiết sản phẩm → Nhấn "Đặt mua"
2. Nhập thông tin: số lượng, giá đề xuất, thời gian giao, địa chỉ nhận
3. Hệ thống validate → Tạo yêu cầu mua hàng (trạng thái `PENDING`)
4. Gửi thông báo cho Farm Manager
5. Retailer có thể theo dõi trạng thái trong "Lịch sử đơn hàng"

---

#### SRS-RT-008: Thanh toán đặt cọc

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-008 |
| **Tham chiếu UR** | BICAP-43 |
| **Tên** | Thanh toán tiền đặt cọc mua nông sản |
| **Mô tả** | Hệ thống cho phép Retailer đặt cọc khi yêu cầu mua hàng được Farm Manager chấp nhận |
| **Độ ưu tiên** | High |
| **Actor** | Retailer |

**Quy tắc nghiệp vụ:**
- BR1: Tỷ lệ đặt cọc là **30%** tổng giá trị đơn hàng (có thể cấu hình bởi Admin)
- BR2: Retailer có **24 giờ** để đặt cọc sau khi đơn được chấp nhận
- BR3: Quá hạn đặt cọc → Đơn tự động hủy
- BR4: Hỗ trợ phương thức: chuyển khoản ngân hàng, ví điện tử

---

#### SRS-RT-009: Hủy yêu cầu đặt mua

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-009 |
| **Tham chiếu UR** | BICAP-44 |
| **Tên** | Hủy yêu cầu đặt mua nông sản |
| **Mô tả** | Retailer có thể hủy yêu cầu đặt mua khi đơn chưa được xử lý, kèm lý do hủy |
| **Độ ưu tiên** | Medium |

**Quy tắc nghiệp vụ:**
- BR1: Chỉ hủy được đơn ở trạng thái `PENDING` hoặc `ACCEPTED` (chưa đặt cọc)
- BR2: Đơn đã đặt cọc không thể hủy trực tiếp → Phải gửi yêu cầu hủy đến Admin
- BR3: Hủy bắt buộc nhập lý do

---

#### SRS-RT-010: Xem lịch sử đơn hàng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-010 |
| **Tham chiếu UR** | BICAP-45 |
| **Tên** | Xem danh sách lịch sử đơn hàng |
| **Độ ưu tiên** | Medium |

---

#### SRS-RT-011: Xem chi tiết đơn hàng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-011 |
| **Tham chiếu UR** | BICAP-46 |
| **Tên** | Xem chi tiết và trạng thái yêu cầu mua hàng |
| **Độ ưu tiên** | Medium |

---

#### SRS-RT-012: Nhận thông báo từ Farm Manager

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-012 |
| **Tham chiếu UR** | BICAP-47 |
| **Tên** | Nhận thông báo từ Quản lý Nông trại |
| **Độ ưu tiên** | Low |

---

#### SRS-RT-013: Gửi thông báo cho Farm Manager

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-013 |
| **Tham chiếu UR** | BICAP-48 |
| **Tên** | Gửi tin nhắn/thông báo đến Quản lý Nông trại |
| **Độ ưu tiên** | Low |

---

#### SRS-RT-014: Xem quy trình vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-014 |
| **Tham chiếu UR** | BICAP-49 |
| **Tên** | Xem và xem chi tiết quy trình vận chuyển |
| **Độ ưu tiên** | Medium |

---

#### SRS-RT-015: Nhận thông báo từ Shipper

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-015 |
| **Tham chiếu UR** | BICAP-50 |
| **Tên** | Nhận thông báo từ Người vận chuyển |
| **Độ ưu tiên** | Low |

---

#### SRS-RT-016: Xác nhận nhận hàng hoàn tất

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-016 |
| **Tham chiếu UR** | BICAP-51 |
| **Tên** | Xác nhận sản phẩm đã vận chuyển hoàn tất |
| **Mô tả** | Retailer xác nhận hàng đã được giao đầy đủ, đúng chất lượng để hoàn tất đơn hàng |
| **Độ ưu tiên** | High |
| **Actor** | Retailer |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã đơn vận chuyển | UUID | Có | Đơn ở trạng thái `DELIVERED` |
| Xác nhận | Boolean | Có | `true` = chấp nhận, `false` = khiếu nại |
| Đánh giá | Integer | Không | 1-5 sao |
| Nhận xét | String (1000) | Không | — |

**Quy tắc nghiệp vụ:**
- BR1: Retailer có **48 giờ** để xác nhận sau khi tài xế đánh dấu "đã giao"
- BR2: Quá hạn → Hệ thống tự động xác nhận
- BR3: Khi xác nhận hoàn tất → Thanh toán số tiền còn lại (70%) cho Farm Manager

---

#### SRS-RT-017: Tải ảnh xác nhận nhận hàng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-017 |
| **Tham chiếu UR** | BICAP-52 |
| **Tên** | Tải lên hình ảnh sản phẩm đã nhận |
| **Độ ưu tiên** | Medium |

---

#### SRS-RT-018: Gửi báo cáo cho Admin

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-RT-018 |
| **Tham chiếu UR** | BICAP-53 |
| **Tên** | Gửi báo cáo cho Admin |
| **Độ ưu tiên** | Low |

> *Đặc tả tương tự SRS-FM-021.*

---

### 4.4. Module Shipping Management Web App (EPIC-4)

> **Mô tả module:** Ứng dụng Web dành cho Quản lý vận chuyển — tạo lô hàng, quản lý phương tiện & tài xế.

---

#### SRS-SM-001: Xem đơn hàng thành công

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-001 |
| **Tham chiếu UR** | BICAP-54 |
| **Tên** | Xem các đơn hàng thành công giữa Retailer và Farm Manager |
| **Mô tả** | Hệ thống hiển thị danh sách đơn hàng đã thỏa thuận và đặt cọc thành công để tạo lô vận chuyển |
| **Độ ưu tiên** | High |
| **Actor** | Shipping Manager |

**Đầu ra:**
- Danh sách đơn hàng trạng thái `DEPOSIT_PAID`
- Mỗi đơn hiển thị: mã đơn, sản phẩm, số lượng, địa chỉ lấy hàng (Farm), địa chỉ giao hàng (Retailer)

---

#### SRS-SM-002: Tạo lô vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-002 |
| **Tham chiếu UR** | BICAP-55 |
| **Tên** | Tạo lô vận chuyển (shipment) cho đơn hàng thành công |
| **Mô tả** | Hệ thống cho phép Shipping Manager tạo lô vận chuyển, phân công tài xế và phương tiện |
| **Độ ưu tiên** | High |
| **Actor** | Shipping Manager |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã đơn hàng | UUID | Có | Đơn trạng thái `DEPOSIT_PAID` |
| Tài xế được phân công | UUID | Có | Tài xế ở trạng thái `AVAILABLE` |
| Phương tiện | UUID | Có | Phương tiện ở trạng thái `AVAILABLE` |
| Lộ trình | String (500) | Không | Mô tả lộ trình |
| Thời gian lấy hàng dự kiến | DateTime | Có | > ngày hiện tại |
| Thời gian giao hàng dự kiến | DateTime | Có | > Thời gian lấy hàng |

**Luồng chính:**
1. Shipping Manager chọn đơn hàng từ danh sách
2. Chọn tài xế từ danh sách tài xế khả dụng
3. Chọn phương tiện từ danh sách phương tiện khả dụng
4. Nhập thời gian lấy/giao hàng dự kiến
5. Hệ thống tạo shipment → Thông báo cho Farm Manager, Retailer, Tài xế
6. Tài xế và phương tiện chuyển trạng thái `ASSIGNED`

---

#### SRS-SM-003: Hủy lô vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-003 |
| **Tham chiếu UR** | BICAP-56 |
| **Tên** | Hủy lô vận chuyển đã tạo |
| **Độ ưu tiên** | Medium |

**Quy tắc nghiệp vụ:**
- BR1: Chỉ hủy được shipment ở trạng thái `CREATED` (chưa bắt đầu thực hiện)
- BR2: Bắt buộc nhập lý do hủy
- BR3: Thông báo cho Farm Manager, Retailer, Tài xế
- BR4: Tài xế và phương tiện chuyển lại `AVAILABLE`

---

#### SRS-SM-004: Theo dõi quy trình vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-004 |
| **Tham chiếu UR** | BICAP-57 |
| **Tên** | Theo dõi quy trình vận chuyển |
| **Độ ưu tiên** | Medium |

---

#### SRS-SM-005: Quản lý phương tiện vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-005 |
| **Tham chiếu UR** | BICAP-58 |
| **Tên** | CRUD phương tiện vận chuyển |
| **Mô tả** | Quản lý thông tin phương tiện: biển số, loại xe, tải trọng, tình trạng |
| **Độ ưu tiên** | Medium |
| **Actor** | Shipping Manager |

**Đầu vào (Tạo/Cập nhật):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Biển số xe | String (15) | Có | Định dạng biển số VN, duy nhất |
| Loại xe | Enum | Có | `TRUCK`, `VAN`, `REFRIGERATED_TRUCK`, `OTHER` |
| Tải trọng (tấn) | Decimal | Có | > 0 |
| Tình trạng | Enum | Có | `AVAILABLE`, `IN_USE`, `MAINTENANCE`, `RETIRED` |
| Hình ảnh xe | File (Image) | Không | JPG/PNG, tối đa 5MB |
| Ghi chú | String (500) | Không | — |

---

#### SRS-SM-006: Quản lý tài xế

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-006 |
| **Tham chiếu UR** | BICAP-59 |
| **Tên** | CRUD tài xế vận chuyển |
| **Mô tả** | Quản lý thông tin tài xế: họ tên, CCCD, bằng lái, SĐT, phương tiện phân công |
| **Độ ưu tiên** | Medium |
| **Actor** | Shipping Manager |

**Đầu vào (Tạo/Cập nhật):**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Họ và tên | String (255) | Có | Không rỗng |
| Số CCCD | String (12) | Có | 12 chữ số, duy nhất |
| Số bằng lái | String (20) | Có | Duy nhất |
| Hạng bằng lái | Enum | Có | `B2`, `C`, `D`, `E` |
| Số điện thoại | String (15) | Có | Định dạng VN |
| Phương tiện phân công | UUID | Không | ID phương tiện |
| Trạng thái | Enum | Có | `AVAILABLE`, `ASSIGNED`, `ON_LEAVE`, `TERMINATED` |

---

#### SRS-SM-007: Gửi báo cáo cho Admin

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-007 |
| **Tham chiếu UR** | BICAP-60 |
| **Tên** | Gửi báo cáo hoạt động cho Admin |
| **Độ ưu tiên** | Low |

---

#### SRS-SM-008: Gửi thông báo cho Farm Manager và Retailer

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-008 |
| **Tham chiếu UR** | BICAP-61 |
| **Tên** | Gửi thông báo cho các bên liên quan |
| **Độ ưu tiên** | Low |

---

#### SRS-SM-009: Xem báo cáo từ tài xế

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SM-009 |
| **Tham chiếu UR** | BICAP-62 |
| **Tên** | Xem báo cáo từ Tài xế vận chuyển |
| **Độ ưu tiên** | Medium |

---

### 4.5. Module Shipping Driver Mobile App (EPIC-5)

> **Mô tả module:** Ứng dụng Mobile dành cho Tài xế vận chuyển — cập nhật hành trình, xác nhận giao nhận.

---

#### SRS-SD-001: Xem danh sách và chi tiết chuyến hàng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SD-001 |
| **Tham chiếu UR** | BICAP-63 |
| **Tên** | Xem và xem chi tiết các chuyến hàng |
| **Mô tả** | Hệ thống hiển thị danh sách chuyến hàng được giao kèm trạng thái, thông tin hàng hóa, địa chỉ |
| **Độ ưu tiên** | High |
| **Actor** | Shipping Driver |
| **Nền tảng** | Mobile App (React Native) |

**Đầu ra:**
| Thông tin | Mô tả |
|-----------|-------|
| Danh sách chuyến hàng | Theo trạng thái: `PENDING_PICKUP`, `IN_TRANSIT`, `DELIVERED` |
| Chi tiết chuyến | Mã shipment, sản phẩm, số lượng, địa chỉ lấy/giao, SĐT liên hệ |
| Bản đồ | Hiển thị lộ trình trên bản đồ (Google Maps / Apple Maps) |

---

#### SRS-SD-002: Cập nhật quy trình vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SD-002 |
| **Tham chiếu UR** | BICAP-64 |
| **Tên** | Cập nhật quy trình vận chuyển |
| **Mô tả** | Tài xế cập nhật trạng thái vận chuyển kèm hình ảnh và ghi chú |
| **Độ ưu tiên** | High |
| **Actor** | Shipping Driver |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã shipment | UUID | Có | Shipment được giao cho tài xế |
| Trạng thái | Enum | Có | `PICKED_UP`, `IN_TRANSIT`, `INCIDENT`, `DELIVERED` |
| Hình ảnh | Array\<File\> | Không | JPG/PNG, mỗi ảnh ≤ 5MB |
| Ghi chú | String (1000) | Không | — |
| Vị trí GPS | Object {lat, lng} | Tự động | Lấy từ thiết bị |

---

#### SRS-SD-003: Quét QR Code khi lấy hàng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SD-003 |
| **Tham chiếu UR** | BICAP-65 |
| **Tên** | Quét QR Code khi đến nông trại lấy hàng |
| **Mô tả** | Tài xế quét QR Code của lô hàng để xác minh thông tin sản phẩm và ghi nhận vào hệ thống |
| **Độ ưu tiên** | High |
| **Tích hợp** | VeChainThor Blockchain, Camera |

**Luồng chính:**
1. Tài xế đến nông trại → Mở "Quét QR"
2. Quét QR Code của lô hàng
3. Hệ thống decode → Truy vấn Blockchain
4. Hiển thị thông tin sản phẩm: tên, số lượng, trang trại, chứng nhận
5. Tài xế so khớp thông tin với đơn hàng
6. Xác nhận thông tin chính xác → Ghi nhận vào hệ thống

---

#### SRS-SD-004: Xác nhận nhận hàng từ nông trại

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SD-004 |
| **Tham chiếu UR** | BICAP-66 |
| **Tên** | Xác nhận đã nhận đủ hàng từ nông trại |
| **Mô tả** | Tài xế xác nhận đã nhận đủ hàng → Shipment chuyển sang `IN_TRANSIT` |
| **Độ ưu tiên** | High |

---

#### SRS-SD-005: Xác nhận giao hàng cho Retailer

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SD-005 |
| **Tham chiếu UR** | BICAP-67 |
| **Tên** | Xác nhận đã giao hàng cho Nhà bán lẻ |
| **Mô tả** | Tài xế xác nhận đã giao hàng → Shipment chuyển sang `DELIVERED` → Thông báo các bên |
| **Độ ưu tiên** | High |

**Đầu vào:**
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc |
|--------|--------------|----------|-----------|
| Mã shipment | UUID | Có | Shipment trạng thái `IN_TRANSIT` |
| Hình ảnh giao hàng | Array\<File\> | Có | Tối thiểu 1 ảnh |
| Chữ ký người nhận | File (Image) | Không | Chữ ký điện tử |
| Ghi chú giao hàng | String (500) | Không | — |
| Vị trí GPS | Object {lat, lng} | Tự động | Xác nhận vị trí giao hàng |

---

#### SRS-SD-006: Gửi báo cáo cho Shipping Manager

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-SD-006 |
| **Tham chiếu UR** | BICAP-68 |
| **Tên** | Gửi báo cáo sự cố cho Shipping Manager |
| **Độ ưu tiên** | Low |

---

### 4.6. Module Guest App (EPIC-6)

> **Mô tả module:** Ứng dụng Web/Mobile dành cho Khách — tra cứu sản phẩm, xem nội dung giáo dục.

---

#### SRS-GS-001: Nhận thông báo chung

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-GS-001 |
| **Tham chiếu UR** | BICAP-69 |
| **Tên** | Nhận thông báo chung về nền tảng |
| **Mô tả** | Guest nhận thông báo: sản phẩm mới, bài viết giáo dục, sự kiện nông nghiệp bền vững |
| **Độ ưu tiên** | Low |
| **Actor** | Guest |

---

#### SRS-GS-002: Tìm kiếm và lọc sản phẩm

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-GS-002 |
| **Tham chiếu UR** | BICAP-70 |
| **Tên** | Tìm kiếm và lọc sản phẩm theo tiêu chí |
| **Mô tả** | Guest có thể tìm kiếm sản phẩm theo: nguồn gốc, loại, chứng nhận, tình trạng còn hàng |
| **Độ ưu tiên** | Medium |
| **Actor** | Guest |

> *Bộ lọc tương tự SRS-RT-004, nhưng Guest **không có quyền đặt mua** — chỉ xem thông tin.*

---

#### SRS-GS-003: Truy cập nội dung giáo dục

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-GS-003 |
| **Tham chiếu UR** | BICAP-71 |
| **Tên** | Truy cập bài viết, video, nội dung giáo dục |
| **Mô tả** | Guest có thể đọc bài viết, xem video về nông nghiệp, canh tác bền vững, an toàn thực phẩm |
| **Độ ưu tiên** | Low |
| **Actor** | Guest |

**Đầu ra:**
| Loại nội dung | Mô tả |
|---------------|-------|
| Bài viết | Danh sách bài viết kèm: thumbnail, tiêu đề, tóm tắt, tác giả, ngày đăng |
| Video | Danh sách video kèm: thumbnail, tiêu đề, thời lượng |
| Phân loại | Theo chủ đề: nông nghiệp hữu cơ, an toàn thực phẩm, công nghệ nông nghiệp, ... |
| Tìm kiếm | Tìm kiếm theo từ khóa, lọc theo chủ đề |

---

### 4.7. Module Backend Web API (EPIC-7)

> **Mô tả module:** API backend cho toàn hệ thống — xác thực, tích hợp blockchain, dữ liệu, thanh toán.

---

#### SRS-API-001: API Xác thực & Phân quyền (RBAC)

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-001 |
| **Tham chiếu UR** | BICAP-72 |
| **Tên** | API xác thực và phân quyền theo vai trò |
| **Độ ưu tiên** | High |

**Endpoints:**
| Method | Endpoint | Mô tả | Auth |
|--------|----------|-------|------|
| POST | `/api/auth/register` | Đăng ký tài khoản mới | Public |
| POST | `/api/auth/login` | Đăng nhập | Public |
| POST | `/api/auth/refresh-token` | Làm mới Access Token | Refresh Token |
| POST | `/api/auth/logout` | Đăng xuất | Bearer Token |
| POST | `/api/auth/forgot-password` | Quên mật khẩu | Public |
| POST | `/api/auth/reset-password` | Đặt lại mật khẩu | Reset Token |
| GET | `/api/auth/me` | Lấy thông tin user hiện tại | Bearer Token |

**Ma trận phân quyền (RBAC):**
| Tài nguyên | Admin | Farm Manager | Retailer | Shipping Mgr | Ship Driver | Guest |
|------------|-------|-------------|----------|--------------|-------------|-------|
| Admin Accounts | CRUD | — | — | — | — | — |
| Farm Registrations | Approve/Reject | Create/Read | — | — | — | — |
| Farming Seasons | Read | CRUD | Read | — | — | Read |
| Products (Trading) | Read/Manage | Create/Read | Read | — | — | Read |
| Orders | Read | Accept/Reject | CRUD | Read | — | — |
| Shipments | Read | Read | Read | CRUD | Read/Update | — |
| Reports | Read | Create | Create | Create | Create | — |
| Smart Contracts | CRUD | — | — | — | — | — |

---

#### SRS-API-002: API Quản lý Mùa vụ + Blockchain

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-002 |
| **Tham chiếu UR** | BICAP-73 |
| **Tên** | API CRUD mùa vụ và quy trình, tích hợp Blockchain |
| **Độ ưu tiên** | High |

**Endpoints:**
| Method | Endpoint | Mô tả | Blockchain |
|--------|----------|-------|------------|
| POST | `/api/farming-seasons` | Tạo mùa vụ mới | ✅ Ghi |
| GET | `/api/farming-seasons` | Danh sách mùa vụ | — |
| GET | `/api/farming-seasons/{id}` | Chi tiết mùa vụ | — |
| POST | `/api/farming-seasons/{id}/processes` | Thêm bước quy trình | ✅ Ghi |
| GET | `/api/farming-seasons/{id}/processes` | Danh sách quy trình | — |
| POST | `/api/farming-seasons/{id}/export` | Xuất kho | ✅ Ghi |
| GET | `/api/farming-seasons/{id}/qr-code` | Lấy QR Code | ✅ Đọc |

---

#### SRS-API-003: API Tích hợp VeChainThor

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-003 |
| **Tham chiếu UR** | BICAP-74 |
| **Tên** | API tích hợp VeChainThor: ghi/đọc giao dịch, QR Code |
| **Độ ưu tiên** | High |

**Endpoints:**
| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/blockchain/transactions` | Ghi giao dịch lên VeChainThor |
| GET | `/api/blockchain/transactions/{hash}` | Đọc giao dịch từ VeChainThor |
| POST | `/api/blockchain/qr-codes` | Tạo QR Code với dữ liệu Blockchain |
| GET | `/api/blockchain/qr-codes/{id}/verify` | Xác thực QR Code |
| GET | `/api/trace/{hash}` | Public endpoint truy xuất nguồn gốc |

---

#### SRS-API-004: API Quản lý Đơn hàng

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-004 |
| **Tham chiếu UR** | BICAP-75 |
| **Tên** | API quản lý vòng đời đơn hàng |
| **Độ ưu tiên** | High |

**Sơ đồ trạng thái đơn hàng:**
```
                                   ┌──────────────┐
                                   │   PENDING    │
                                   └──────┬───────┘
                              ┌───────────┼───────────┐
                              ▼                       ▼
                       ┌──────────┐           ┌──────────────┐
                       │ ACCEPTED │           │   REJECTED   │
                       └────┬─────┘           └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │ DEPOSIT_PAID │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  SHIPPING    │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  DELIVERED   │
                     └──────┬───────┘
                            │
                            ▼
                     ┌──────────────┐
                     │  COMPLETED   │
                     └──────────────┘
```

**Trạng thái hủy có thể xảy ra từ:** `PENDING`, `ACCEPTED` → `CANCELLED`

---

#### SRS-API-005: API Quản lý Vận chuyển

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-005 |
| **Tham chiếu UR** | BICAP-76 |
| **Tên** | API quản lý vận chuyển: tạo shipment, tracking |
| **Độ ưu tiên** | High |

**Sơ đồ trạng thái lô vận chuyển (Shipment):**
```
  ┌─────────┐     ┌────────────────┐     ┌────────────┐     ┌───────────┐     ┌───────────┐
  │ CREATED │────▶│ PENDING_PICKUP │────▶│ PICKED_UP  │────▶│IN_TRANSIT │────▶│ DELIVERED │
  └─────────┘     └────────────────┘     └────────────┘     └───────────┘     └───────────┘
       │
       ▼
  ┌───────────┐
  │ CANCELLED │
  └───────────┘
```

---

#### SRS-API-006: API Thông báo Real-time

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-006 |
| **Tham chiếu UR** | BICAP-77 |
| **Tên** | API thông báo real-time |
| **Độ ưu tiên** | Medium |

**Kênh thông báo:**
| Kênh | Công nghệ | Đối tượng |
|------|-----------|-----------|
| In-app Notification | WebSocket (SignalR) | Tất cả Web App |
| Push Notification | Firebase Cloud Messaging | Mobile App |
| Email | SMTP / SendGrid | Thông báo quan trọng |

---

#### SRS-API-007: API Thanh toán

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-007 |
| **Tham chiếu UR** | BICAP-78 |
| **Tên** | API tích hợp cổng thanh toán |
| **Độ ưu tiên** | High |

---

#### SRS-API-008: Thiết kế & Triển khai CSDL

| Thuộc tính | Chi tiết |
|------------|----------|
| **Mã yêu cầu** | SRS-API-008 |
| **Tham chiếu UR** | BICAP-79 |
| **Tên** | Thiết kế schema MySQL 5.7.41 và cấu hình Redis 8.6 |
| **Độ ưu tiên** | High |

---

---

## 5. Yêu cầu phi chức năng

### 5.1. Khả năng mở rộng (Scalability)

| Mã | Yêu cầu | Đặc tả chi tiết | Tiêu chí đo lường |
|----|---------|------------------|--------------------|
| **NFR-001** | Mở rộng linh hoạt (Horizontal Scaling) | Hệ thống phải mở rộng linh hoạt xử lý số lượng lớn người dùng. Sử dụng Docker container orchestration, Redis 8.6 cluster, load balancer. | Hệ thống tự động scale khi CPU > 70% hoặc RAM > 80% |
| **NFR-002** | Xử lý giao dịch Blockchain đồng thời | VeChainThor phải hỗ trợ xử lý nhiều giao dịch đồng thời khi dữ liệu IoT tăng hoặc khi có nhiều request truy xuất sản phẩm. | Xử lý tối thiểu 100 giao dịch/phút |

### 5.2. Bảo mật (Security)

| Mã | Yêu cầu | Đặc tả chi tiết | Tiêu chí đo lường |
|----|---------|------------------|--------------------|
| **NFR-003** | Tính minh bạch & bất biến dữ liệu | Dữ liệu truy xuất nguồn gốc ghi trên Blockchain phải bất biến (immutable). Sử dụng chuẩn mã hóa VeChainThor. | 100% dữ liệu trên Blockchain không thể chỉnh sửa sau khi ghi |
| **NFR-004** | Phân quyền RBAC | Hệ thống phân quyền dựa trên vai trò: Admin, Farm Manager, Retailer, Shipping Manager, Shipping Driver, Guest. Mỗi vai trò chỉ truy cập chức năng và dữ liệu phù hợp. | 0 lỗi unauthorized access trong security testing |
| **NFR-011** | Mã hóa dữ liệu & Lưu trữ Token | Mật khẩu dùng bcrypt (cost 12). Dữ liệu nhạy cảm mã hóa at-rest (AES-256 GCM). Traffic mạng mã hóa HTTPS/TLS 1.2+ và MQTTS 1.3. **JWT Access token bắt buộc lưu trong `httpOnly`, `SameSite=Strict`, `Secure` cookie (nghiêm cấm lưu trong `localStorage` để chống XSS).** | 100% API endpoints dùng HTTPS; Token chỉ nằm trong httpOnly cookie |
| **NFR-012** | Bảo vệ chống tấn công & Audit | Hệ thống phải chống được SQL Injection (JPA), XSS (Output Encoding + CSP), CSRF (SameSite + Anti-CSRF Token), Brute Force (Lockout 30 min sau 5 lần sai). Ghi vết Audit Log cho hành vi Admin/Sensitive operations. Redis phải bật AUTH + TLS. | Vượt qua OWASP Top 10 security checklist & Audit logging 100% admin actions |

### 5.3. Hiệu năng (Performance)

| Mã | Yêu cầu | Đặc tả chi tiết | Tiêu chí đo lường |
|----|---------|------------------|--------------------|
| **NFR-005** | Thời gian phản hồi | Các thao tác thông thường (xem danh sách, tìm kiếm, xem chi tiết) phải phản hồi nhanh trong tải bình thường. | Response time < 2 giây cho 95th percentile |
| **NFR-006** | Cache hiệu quả | Sử dụng Redis 8.6 cache giảm tải database, tăng tốc truy xuất dữ liệu thường xuyên. | Cache hit rate ≥ 80% |
| **NFR-013** | Người dùng đồng thời | Hệ thống phải xử lý ít nhất 500 người dùng đồng thời mà không giảm hiệu năng đáng kể. | Response time < 3 giây khi 500 concurrent users |
| **NFR-014** | Uptime | Hệ thống phải đạt uptime tối thiểu 99.5%. | Downtime ≤ 43.8 giờ/năm |

### 5.4. Khả năng sử dụng (Usability)

| Mã | Yêu cầu | Đặc tả chi tiết | Tiêu chí đo lường |
|----|---------|------------------|--------------------|
| **NFR-007** | Giao diện thân thiện | Giao diện dễ sử dụng, phù hợp với nông dân có hạn chế kỹ năng công nghệ. Sử dụng icon trực quan, font chữ rõ ràng, contrast tốt. | SUS (System Usability Scale) score ≥ 70 |
| **NFR-008** | Responsive Design | Web App hiển thị tốt trên Desktop (≥1024px) và Tablet (≥768px). Mobile App tương thích Android ≥ 8.0 và iOS ≥ 14.0. | Không có lỗi layout trên các thiết bị mục tiêu |

### 5.5. Khả năng tích hợp (Integration)

| Mã | Yêu cầu | Đặc tả chi tiết | Tiêu chí đo lường |
|----|---------|------------------|--------------------|
| **NFR-009** | Tích hợp IoT | Hệ thống tích hợp cảm biến IoT thu thập nhiệt độ, độ ẩm, pH tự động. Hỗ trợ giao thức MQTT/REST API. | Nhận dữ liệu IoT trong vòng 5 giây kể từ khi cảm biến gửi |
| **NFR-010** | Tích hợp cổng thanh toán | Hệ thống tích hợp cổng thanh toán trực tuyến xử lý mua gói dịch vụ và đặt cọc. | 100% giao dịch thanh toán được xử lý chính xác |

### 5.6. Khả năng bảo trì (Maintainability)

| Mã | Yêu cầu | Đặc tả chi tiết | Tiêu chí đo lường |
|----|---------|------------------|--------------------|
| **NFR-015** | Mã nguồn có cấu trúc | Mã nguồn tuân thủ coding convention, tổ chức theo module, có documentation. | Code review pass rate ≥ 90% |
| **NFR-016** | Logging & Monitoring | Hệ thống phải ghi log đầy đủ (error, warning, info) và hỗ trợ monitoring. | 100% errors được log với context đầy đủ |

---

## 6. Yêu cầu cơ sở dữ liệu

### 6.1. MySQL — Các bảng chính (Core Tables)

| STT | Bảng | Mô tả | Quan hệ chính |
|-----|------|-------|---------------|
| 1 | `Users` | Thông tin tài khoản người dùng | — |
| 2 | `Roles` | Vai trò người dùng (Admin, FM, RT, SM, SD, GS) | Users ↔ Roles (N:N) |
| 3 | `Permissions` | Quyền hạn trong hệ thống | Roles ↔ Permissions (N:N) |
| 4 | `Farms` | Thông tin nông trại | Farms → Users (N:1) |
| 5 | `FarmCertifications` | Chứng nhận, giấy phép nông trại | FarmCertifications → Farms (N:1) |
| 6 | `ServicePackages` | Gói dịch vụ | — |
| 7 | `Subscriptions` | Đăng ký gói dịch vụ của Farm | Subscriptions → Farms, ServicePackages |
| 8 | `FarmingSeasons` | Mùa vụ | FarmingSeasons → Farms (N:1) |
| 9 | `FarmingProcesses` | Các bước quy trình trong mùa vụ | FarmingProcesses → FarmingSeasons (N:1) |
| 10 | `Products` | Sản phẩm trên sàn giao dịch | Products → FarmingSeasons (N:1) |
| 11 | `ProductCategories` | Danh mục sản phẩm | Products → ProductCategories (N:1) |
| 12 | `Orders` | Đơn hàng (yêu cầu mua) | Orders → Products, Users (Retailer) |
| 13 | `Payments` | Giao dịch thanh toán | Payments → Orders, Subscriptions |
| 14 | `Shipments` | Lô vận chuyển | Shipments → Orders |
| 15 | `ShipmentTracking` | Lịch sử tracking vận chuyển | ShipmentTracking → Shipments (N:1) |
| 16 | `Vehicles` | Phương tiện vận chuyển | — |
| 17 | `Drivers` | Thông tin tài xế | Drivers → Users (1:1) |
| 18 | `Notifications` | Thông báo | Notifications → Users (N:1) |
| 19 | `Reports` | Báo cáo / Khiếu nại | Reports → Users (N:1) |
| 20 | `BlockchainTransactions` | Lưu transaction hash, metadata | BlockchainTransactions → FarmingSeasons, Products |
| 21 | `QRCodes` | Mã QR đã tạo | QRCodes → BlockchainTransactions (1:1) |
| 22 | `IoTData` | Dữ liệu cảm biến IoT | IoTData → Farms (N:1) |
| 23 | `Articles` | Bài viết giáo dục | — |
| 24 | `Media` | Hình ảnh, video đính kèm | Media → nhiều bảng (polymorphic) |

### 6.2. Redis 8.6 — Cấu trúc Cache

| Key Pattern | Mô tả | TTL |
|-------------|-------|-----|
| `user:{id}` | Thông tin user session | 15 phút |
| `farm:{id}` | Thông tin nông trại | 1 giờ |
| `product:list:{page}:{filters}` | Danh sách sản phẩm (cache kết quả tìm kiếm) | 5 phút |
| `product:{id}` | Chi tiết sản phẩm | 30 phút |
| `notification:{userId}:unread` | Số thông báo chưa đọc | 1 phút |
| `iot:{farmId}:latest` | Dữ liệu IoT mới nhất | 5 phút |
| `rate_limit:{ip}` | Rate limiting | 1 phút |

### 6.3. VeChainThor Blockchain — Dữ liệu lưu trữ

| Dữ liệu | Smart Contract | Mô tả |
|----------|---------------|-------|
| Thông tin mùa vụ | `FarmingSeasonContract` | Tên, giống, diện tích, ngày bắt đầu, farm ID |
| Quy trình sản xuất | `FarmingProcessContract` | Loại hoạt động, ngày, vật tư, hash hình ảnh (IPFS) |
| Xuất kho | `ExportContract` | Số lượng, ngày xuất, hash QR Code |
| Truy xuất nguồn gốc | `TraceabilityContract` | Tổng hợp toàn bộ vòng đời sản phẩm |

---

## 7. Ràng buộc thiết kế

### 7.1. Công nghệ bắt buộc

| Thành phần | Công nghệ | Phiên bản tối thiểu |
|-----------|-----------|---------------------|
| Backend Server | Java (Spring Boot 3.x) | JDK 21 |
| Persistence / ORM | Spring Data JPA (Hibernate) | Spring Data 3.x |
| Database | MySQL | 5.7.41 |
| Cache | Redis | 8.6 |
| Web Client | ReactJS / Next.js (TypeScript) | React 18+ / Next.js 13+ |
| Mobile App | React Native (TypeScript) | React Native 0.72+ |
| Blockchain Platform | VeChainThor | — |
| Smart Contract | Solidity | 0.8+ |
| Blockchain SDK / Tools | web3j / VeChain Java SDK, Connex.js / Thor REST API | — |
| Build Tool & Language | Java (JDK 21), TypeScript / JavaScript, Maven / Gradle | — |
| Infrastructure | AWS / Google Cloud | — |
| Containerization | Docker | 20+ |
| Container Orchestration | Docker Compose / Kubernetes | — |

### 7.2. Quy trình phát triển

- Áp dụng **Agile Scrum** với sprint 2 tuần
- Sử dụng **UML 2.0** cho mô hình hóa hệ thống
- Quản lý source code bằng **Git** (GitFlow branching strategy)
- Quản lý task trên **Jira** (đồng bộ với Epic/Story trong tài liệu này)
- Code review bắt buộc trước khi merge vào branch chính

### 7.3. Bộ tài liệu bắt buộc

| STT | Tài liệu | Mã Jira | Trạng thái |
|-----|----------|---------|------------|
| 1 | User Requirements Document | BICAP-91 | ✅ Hoàn thành (v2.0) |
| 2 | Software Requirement Specifications (SRS) | BICAP-92 | 📝 Tài liệu này |
| 3 | Architecture Design | BICAP-93 | ✅ Hoàn thành (v1.0) |
| 4 | Detail Design | BICAP-94 | ✅ Hoàn thành (v1.0) |
| 5 | System Implementation Document | BICAP-95 | ⏳ Chưa bắt đầu |
| 6 | Testing Document | BICAP-96 | ⏳ Chưa bắt đầu |
| 7 | Installation Guide | BICAP-97 | ⏳ Chưa bắt đầu |
| 8 | User Manual | BICAP-98 | ⏳ Chưa bắt đầu |

---

## 8. Ma trận truy xuất yêu cầu

### 8.1. User Requirements → SRS Traceability

| User Requirement (UR) | SRS Requirement | Module | Độ ưu tiên |
|-----------------------|-----------------|--------|------------|
| BICAP-1 | SRS-ADM-001 | Admin | High |
| BICAP-3 | SRS-ADM-002 | Admin | High |
| BICAP-4 | SRS-ADM-003 | Admin | Medium |
| BICAP-5 | SRS-ADM-004 | Admin | Medium |
| BICAP-6 | SRS-ADM-005 | Admin | High |
| BICAP-7 | SRS-FM-001 | Farm Management | High |
| BICAP-8 | SRS-FM-002 | Farm Management | Medium |
| BICAP-9 | SRS-FM-003 | Farm Management | Medium |
| BICAP-10 | SRS-FM-004 | Farm Management | High |
| BICAP-11 | SRS-FM-005 | Farm Management | High |
| BICAP-12 | SRS-FM-006 | Farm Management | Medium |
| BICAP-13 | SRS-FM-007 | Farm Management | Medium |
| BICAP-14 | SRS-FM-008 | Farm Management | High |
| BICAP-15 | SRS-FM-009 | Farm Management | High |
| BICAP-16 | SRS-FM-010 | Farm Management | Medium |
| BICAP-17 | SRS-FM-011 | Farm Management | High |
| BICAP-18 | SRS-FM-012 | Farm Management | Medium |
| BICAP-19 | SRS-FM-013 | Farm Management | Low |
| BICAP-20 | SRS-FM-014 | Farm Management | High |
| BICAP-21 | SRS-FM-015 | Farm Management | Medium |
| BICAP-22 | SRS-FM-016 | Farm Management | Medium |
| BICAP-23 | SRS-FM-017 | Farm Management | Low |
| BICAP-24 | SRS-FM-018 | Farm Management | Low |
| BICAP-25 | SRS-FM-019 | Farm Management | Low |
| BICAP-26 | SRS-FM-020 | Farm Management | Medium |
| BICAP-27 | SRS-FM-021 | Farm Management | Low |
| BICAP-36 | SRS-RT-001 | Retailer | High |
| BICAP-37 | SRS-RT-002 | Retailer | Medium |
| BICAP-38 | SRS-RT-003 | Retailer | Medium |
| BICAP-39 | SRS-RT-004 | Retailer | High |
| BICAP-40 | SRS-RT-005 | Retailer | Medium |
| BICAP-41 | SRS-RT-006 | Retailer | High |
| BICAP-42 | SRS-RT-007 | Retailer | High |
| BICAP-43 | SRS-RT-008 | Retailer | High |
| BICAP-44 | SRS-RT-009 | Retailer | Medium |
| BICAP-45 | SRS-RT-010 | Retailer | Medium |
| BICAP-46 | SRS-RT-011 | Retailer | Medium |
| BICAP-47 | SRS-RT-012 | Retailer | Low |
| BICAP-48 | SRS-RT-013 | Retailer | Low |
| BICAP-49 | SRS-RT-014 | Retailer | Medium |
| BICAP-50 | SRS-RT-015 | Retailer | Low |
| BICAP-51 | SRS-RT-016 | Retailer | High |
| BICAP-52 | SRS-RT-017 | Retailer | Medium |
| BICAP-53 | SRS-RT-018 | Retailer | Low |
| BICAP-54 | SRS-SM-001 | Shipping Management | High |
| BICAP-55 | SRS-SM-002 | Shipping Management | High |
| BICAP-56 | SRS-SM-003 | Shipping Management | Medium |
| BICAP-57 | SRS-SM-004 | Shipping Management | Medium |
| BICAP-58 | SRS-SM-005 | Shipping Management | Medium |
| BICAP-59 | SRS-SM-006 | Shipping Management | Medium |
| BICAP-60 | SRS-SM-007 | Shipping Management | Low |
| BICAP-61 | SRS-SM-008 | Shipping Management | Low |
| BICAP-62 | SRS-SM-009 | Shipping Management | Medium |
| BICAP-63 | SRS-SD-001 | Shipping Driver | High |
| BICAP-64 | SRS-SD-002 | Shipping Driver | High |
| BICAP-65 | SRS-SD-003 | Shipping Driver | High |
| BICAP-66 | SRS-SD-004 | Shipping Driver | High |
| BICAP-67 | SRS-SD-005 | Shipping Driver | High |
| BICAP-68 | SRS-SD-006 | Shipping Driver | Low |
| BICAP-69 | SRS-GS-001 | Guest | Low |
| BICAP-70 | SRS-GS-002 | Guest | Medium |
| BICAP-71 | SRS-GS-003 | Guest | Low |
| BICAP-72 | SRS-API-001 | Backend API | High |
| BICAP-73 | SRS-API-002 | Backend API | High |
| BICAP-74 | SRS-API-003 | Backend API | High |
| BICAP-75 | SRS-API-004 | Backend API | High |
| BICAP-76 | SRS-API-005 | Backend API | High |
| BICAP-77 | SRS-API-006 | Backend API | Medium |
| BICAP-78 | SRS-API-007 | Backend API | High |
| BICAP-79 | SRS-API-008 | Backend API | High |

### 8.2. Thống kê SRS theo Module

| Module | Tổng yêu cầu | High | Medium | Low |
|--------|-------------|------|--------|-----|
| Admin Web App | 5 | 3 | 2 | 0 |
| Farm Management Web App | 21 | 8 | 8 | 5 |
| Retailer Web App | 18 | 7 | 6 | 5 |
| Shipping Management Web App | 9 | 2 | 5 | 2 |
| Shipping Driver Mobile App | 6 | 5 | 0 | 1 |
| Guest App | 3 | 0 | 1 | 2 |
| Backend Web API | 8 | 7 | 1 | 0 |
| **Tổng yêu cầu chức năng** | **70** | **32** | **23** | **15** |
| Yêu cầu phi chức năng | 16 | — | — | — |
| **TỔNG CỘNG** | **86** | — | — | — |

---

## 9. Phụ lục

### 9.1. Danh mục API Endpoints tổng hợp

| Module | Base Path | Số Endpoints (ước tính) |
|--------|-----------|------------------------|
| Authentication | `/api/auth/*` | 7 |
| Admin | `/api/admin/*` | 12 |
| Farm Management | `/api/farms/*` | 25 |
| Farming Seasons | `/api/farming-seasons/*` | 10 |
| Products / Trading | `/api/products/*` | 8 |
| Orders | `/api/orders/*` | 10 |
| Retailer | `/api/retailers/*` | 8 |
| Shipping | `/api/shipments/*` | 12 |
| Vehicles | `/api/vehicles/*` | 5 |
| Drivers | `/api/drivers/*` | 5 |
| Blockchain | `/api/blockchain/*` | 5 |
| Notifications | `/api/notifications/*` | 5 |
| Reports | `/api/reports/*` | 5 |
| Payments | `/api/payments/*` | 5 |
| IoT | `/api/iot/*` | 3 |
| Articles / Content | `/api/articles/*` | 5 |
| **Tổng** | | **~130** |

### 9.2. Quy ước mã yêu cầu

| Tiền tố | Ý nghĩa | Ví dụ |
|---------|---------|-------|
| `SRS-ADM-xxx` | Yêu cầu chức năng module Admin | SRS-ADM-001 |
| `SRS-FM-xxx` | Yêu cầu chức năng module Farm Management | SRS-FM-008 |
| `SRS-RT-xxx` | Yêu cầu chức năng module Retailer | SRS-RT-004 |
| `SRS-SM-xxx` | Yêu cầu chức năng module Shipping Management | SRS-SM-002 |
| `SRS-SD-xxx` | Yêu cầu chức năng module Shipping Driver | SRS-SD-003 |
| `SRS-GS-xxx` | Yêu cầu chức năng module Guest | SRS-GS-002 |
| `SRS-API-xxx` | Yêu cầu chức năng Backend API | SRS-API-001 |
| `NFR-xxx` | Yêu cầu phi chức năng | NFR-005 |

### 9.3. Lịch sử phiên bản tài liệu

| Phiên bản | Ngày | Người thay đổi | Mô tả thay đổi |
|-----------|------|----------------|-----------------|
| 1.0 | 23/07/2026 | Team BICAP | Phiên bản đầu tiên — đặc tả đầy đủ từ User Requirements v2.0 |
| 1.1 | 23/07/2026 | Team BICAP | Cập nhật Backend: Java (Spring Boot 3.x) & Frontend: React Ecosystem (ReactJS/Next.js cho Web, React Native cho Mobile) |

---

> **Ghi chú cuối:** Tài liệu SRS này được xây dựng dựa trên User Requirements Document v2.0 và Requirement Document của dự án BICAP. Mọi thay đổi về yêu cầu phải được cập nhật đồng thời trên cả User Requirements và SRS, đồng bộ với Jira Board.
