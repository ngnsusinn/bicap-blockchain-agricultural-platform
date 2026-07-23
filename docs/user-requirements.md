# TÀI LIỆU YÊU CẦU NGƯỜI DÙNG (USER REQUIREMENTS DOCUMENT)

## DỰ ÁN: TÍCH HỢP BLOCKCHAIN TRONG SẢN XUẤT NÔNG SẢN SẠCH (BICAP)

| Thông tin | Chi tiết |
|---|---|
| **Tên dự án (EN)** | Blockchain Integration in Clean Agricultural Production |
| **Tên dự án (VN)** | Tích hợp Blockchain trong sản xuất nông sản sạch |
| **Viết tắt** | BICAP |
| **Phiên bản tài liệu** | 2.0 |
| **Ngày tạo** | 23/07/2026 |
| **Trạng thái** | Bản nháp (Draft) |

---

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Phạm vi dự án](#2-phạm-vi-dự-án)
3. [Các bên liên quan và vai trò người dùng](#3-các-bên-liên-quan-và-vai-trò-người-dùng)
4. [Yêu cầu chức năng người dùng](#4-yêu-cầu-chức-năng-người-dùng)
5. [Yêu cầu Backend Web API](#5-yêu-cầu-backend-web-api)
6. [Yêu cầu Build – Deploy – Test](#6-yêu-cầu-build--deploy--test)
7. [Yêu cầu tài liệu dự án](#7-yêu-cầu-tài-liệu-dự-án)
8. [Yêu cầu phi chức năng](#8-yêu-cầu-phi-chức-năng)
9. [Ràng buộc hệ thống](#9-ràng-buộc-hệ-thống)
10. [Phụ lục](#10-phụ-lục)

---

## 1. Giới thiệu

### 1.1. Mục đích tài liệu

Tài liệu này mô tả chi tiết các yêu cầu của người dùng (User Requirements) cho hệ thống BICAP — nền tảng tích hợp Blockchain trong sản xuất nông sản sạch. Tài liệu được sử dụng làm cơ sở cho việc phân tích, thiết kế và phát triển hệ thống, đồng thời được đồng bộ với cấu trúc quản lý công việc trên Jira.

### 1.2. Đối tượng đọc

- Nhóm phát triển phần mềm (Developers)
- Nhóm kiểm thử (Testers)
- Quản lý dự án (Project Manager)
- Các bên liên quan (Stakeholders)
- Giảng viên hướng dẫn

### 1.3. Bối cảnh dự án

Dự án BICAP được triển khai nhằm đáp ứng nhu cầu ngày càng tăng trong nước về các sản phẩm nông sản sạch và có khả năng truy xuất nguồn gốc. Nhiều trang trại và hợp tác xã nông nghiệp vừa và nhỏ tại Việt Nam gặp khó khăn trong việc:

- Giám sát và quản lý quy trình sản xuất
- Đáp ứng các tiêu chuẩn an toàn thực phẩm
- Cung cấp thông tin minh bạch về quy trình sản xuất cho người tiêu dùng

Dự án tận dụng công nghệ Blockchain để cung cấp giải pháp theo dõi nguồn gốc nông sản **từ trang trại đến bàn ăn (farm-to-table)**, giúp tăng tính minh bạch, xây dựng niềm tin cho người tiêu dùng và góp phần phát triển bền vững ngành nông nghiệp Việt Nam.

### 1.4. Các giải pháp đề xuất

| STT | Giải pháp | Mô tả |
|-----|-----------|-------|
| 1 | Tối ưu giám sát sản xuất | Hệ thống giám sát quy trình sản xuất nông nghiệp theo thời gian thực |
| 2 | Truy xuất nguồn gốc Blockchain | Sử dụng Blockchain để ghi nhận và truy xuất toàn bộ vòng đời sản phẩm |
| 3 | Mã QR truy xuất | Cung cấp mã QR để người tiêu dùng kiểm tra nguồn gốc sản phẩm |
| 4 | Phân tích và dự báo | Tối ưu chi phí và hiệu quả thông qua phân tích dữ liệu và dự báo |
| 5 | Kết nối trang trại — nhà bán lẻ | Sàn giao dịch kết nối trực tiếp trang trại với nhà phân phối bán lẻ |

---

## 2. Phạm vi dự án

### 2.1. Cấu trúc Epic trên Jira

| Epic ID | Epic Name | Mô tả | Nền tảng |
|---------|-----------|-------|----------|
| EPIC-1 | Admin Web App | Ứng dụng Web dành cho Admin: quản lý tài khoản, duyệt nông trại, quản lý sản phẩm & smart contract | Web App |
| EPIC-2 | Farm Management Web App | Ứng dụng Web dành cho Quản lý Nông trại: đăng ký, quản lý mùa vụ, giao dịch với Retailer, theo dõi vận chuyển | Web App |
| EPIC-3 | Retailer Web App | Ứng dụng Web dành cho Nhà bán lẻ: tìm kiếm, đặt mua nông sản, theo dõi đơn hàng & vận chuyển | Web App |
| EPIC-4 | Shipping Management Web App | Ứng dụng Web dành cho Quản lý vận chuyển: tạo lô hàng, quản lý phương tiện & tài xế | Web App |
| EPIC-5 | Shipping Driver Mobile App | Ứng dụng Mobile dành cho Tài xế vận chuyển: cập nhật hành trình, xác nhận giao nhận | Mobile App |
| EPIC-6 | Guest App | Ứng dụng Web/Mobile dành cho Khách: tra cứu, tìm kiếm sản phẩm, nội dung giáo dục | Web / Mobile App |
| EPIC-7 | Backend Web API | API backend cho toàn hệ thống: xác thực, tích hợp blockchain VeChainThor, dữ liệu, thanh toán | Backend API |
| EPIC-8 | Build – Deploy – Test | Hạ tầng, triển khai và kiểm thử toàn hệ thống | DevOps / QA |
| EPIC-9 | Project Documentation | Tài liệu dự án: SRS, thiết kế, kiểm thử, hướng dẫn cài đặt & sử dụng | Documentation |

### 2.2. Các sản phẩm phần mềm

| STT | Sản phẩm | Nền tảng | Người dùng |
|-----|----------|----------|------------|
| 1 | Web App quản trị hệ thống | Web Application | Admin |
| 2 | Web App quản lý trang trại | Web Application | Farm Manager (Chủ trang trại) |
| 3 | Web App nhà bán lẻ | Web Application | Retailer (Nhà bán lẻ) |
| 4 | Web App quản lý vận chuyển | Web Application | Shipping Manager (Quản lý vận chuyển) |
| 5 | Mobile App tài xế vận chuyển | Mobile Application | Shipping Driver (Tài xế) |
| 6 | Mobile App / Web App khách | Mobile / Web Application | Guest (Khách) |
| 7 | Web API hệ thống | Backend API | Tất cả các module |

### 2.3. Sơ đồ tổng quan hệ thống

```
┌──────────────────────────────────────────────────────────────────┐
│                        HỆ THỐNG BICAP                           │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │  Admin   │  │   Farm   │  │ Retailer │  │    Shipping      │ │
│  │ Web App  │  │ Web App  │  │ Web App  │  │  Manager Web App │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───────┬──────────┘ │
│       │              │              │                │            │
│       └──────────────┴──────────────┴────────────────┘            │
│                              │                                    │
│                      ┌───────┴───────┐                           │
│                      │   Web API     │                           │
│                      │ (Java Spring  │                           │
│                      │    Boot)      │                           │
│                      └───────┬───────┘                           │
│                              │                                    │
│              ┌───────────────┼───────────────┐                   │
│              │               │               │                   │
│       ┌──────┴──────┐ ┌─────┴─────┐ ┌───────┴───────┐          │
│       │ MySQL 5.7.41│ │ Redis 8.6 │ │  VeChainThor  │          │
│       │  Database   │ │   Cache   │ │  Blockchain   │          │
│       └─────────────┘ └───────────┘ └───────────────┘          │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐                             │
│  │  Ship Driver │  │    Guest     │                             │
│  │  Mobile App  │  │ Mobile/Web   │                             │
│  └──────────────┘  └──────────────┘                             │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Các bên liên quan và vai trò người dùng

### 3.1. Bảng vai trò người dùng (User Roles)

| Vai trò | Viết tắt | Component (Jira) | Mô tả | Nền tảng |
|---------|----------|-------------------|-------|----------|
| **Admin** | ADM | Admin | Quản trị viên hệ thống, quản lý toàn bộ nền tảng, phê duyệt trang trại, quản lý smart contract | Web App |
| **Farm Manager** | FM | Farm Management | Chủ trang trại hoặc hợp tác xã nông nghiệp, quản lý mùa vụ, quy trình sản xuất và xuất kho | Web App |
| **Retailer** | RT | Retailer | Nhà bán lẻ / nhà phân phối, tìm kiếm và đặt mua nông sản từ sàn giao dịch | Web App |
| **Shipping Manager** | SM | Shipping Management | Quản lý vận chuyển, tạo và điều phối các đơn vận chuyển | Web App |
| **Shipping Driver** | SD | Shipping Driver | Tài xế vận chuyển, thực hiện giao hàng và cập nhật trạng thái vận chuyển | Mobile App |
| **Guest** | GS | Guest | Khách truy cập, tra cứu sản phẩm, đọc nội dung giáo dục về nông nghiệp | Web / Mobile App |
| **Backend/API** | API | Backend/API | Nhóm phát triển backend, xây dựng API cho toàn hệ thống | — |
| **DevOps/QA** | DQ | DevOps/QA | Nhóm hạ tầng, triển khai và kiểm thử | — |
| **Documentation** | DOC | Documentation | Nhóm biên soạn tài liệu dự án | — |

---

## 4. Yêu cầu chức năng người dùng

### 4.1. EPIC-1: Admin Web App

> **Mô tả Epic:** Ứng dụng Web dành cho Admin: quản lý tài khoản, duyệt nông trại, quản lý sản phẩm & smart contract.

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-1** | Tạo, xem, sửa, xóa tài khoản admin khác; phân quyền, vai trò | Là Admin, tôi muốn tạo, xem, chỉnh sửa và xóa các tài khoản Admin khác, đồng thời phân quyền (role & permission) để phân công quản lý hệ thống. | Admin | High | web-app |
| **BICAP-3** | Xem, phê duyệt hoặc từ chối đăng ký nông trại mới | Là Admin, tôi muốn xem danh sách các nông trại mới đăng ký, xem xét hồ sơ (giấy phép kinh doanh, chứng nhận) và phê duyệt hoặc từ chối để đảm bảo tính hợp pháp. | Admin | High | web-app |
| **BICAP-4** | Quản lý thông tin chi tiết nông trại (chứng nhận, liên hệ, vị trí) | Là Admin, tôi muốn truy cập và quản lý thông tin chi tiết của tất cả nông trại trên hệ thống, bao gồm: chứng nhận, thông tin liên hệ, vị trí, trạng thái hoạt động. | Admin | Medium | web-app |
| **BICAP-5** | Giám sát toàn bộ sản phẩm trên nền tảng; quản lý danh mục, mô tả, đảm bảo tính chính xác dữ liệu | Là Admin, tôi muốn giám sát tất cả sản phẩm đăng ký trên nền tảng, quản lý danh mục sản phẩm, mô tả và đảm bảo tính chính xác của dữ liệu. | Admin | Medium | web-app |
| **BICAP-6** | Triển khai, cập nhật, quản lý Smart Contract để đảm bảo tính chính xác, minh bạch dữ liệu truy xuất trên blockchain | Là Admin, tôi muốn triển khai (deploy), cập nhật và quản lý các Smart Contract trên Blockchain VeChainThor để duy trì tính chính xác và minh bạch của dữ liệu truy xuất nguồn gốc. | Admin | High | web-app, blockchain |

---

### 4.2. EPIC-2: Farm Management Web App

> **Mô tả Epic:** Ứng dụng Web dành cho Quản lý Nông trại: đăng ký, quản lý mùa vụ, giao dịch với Retailer, theo dõi vận chuyển.

#### 4.2.1. Quản lý tài khoản & hồ sơ

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-7** | Đăng ký, đăng nhập tài khoản | Là chủ trang trại, tôi muốn đăng ký tài khoản mới và đăng nhập vào hệ thống bằng email/số điện thoại và mật khẩu để truy cập các dịch vụ quản lý trang trại. | Farm Management | High | web-app |
| **BICAP-8** | Cập nhật thông tin cá nhân chủ sở hữu | Là chủ trang trại, tôi muốn cập nhật thông tin cá nhân (họ tên, số điện thoại, địa chỉ, ảnh đại diện) để thông tin của tôi luôn chính xác trên hệ thống. | Farm Management | Medium | web-app |
| **BICAP-9** | Cập nhật Giấy phép kinh doanh và thông tin nông trại | Là chủ trang trại, tôi muốn tải lên Giấy phép kinh doanh và cập nhật thông tin nông trại (tên, địa chỉ, diện tích, loại cây trồng/vật nuôi, chứng nhận) để hệ thống và Admin có thể xác minh tính hợp lệ. | Farm Management | Medium | web-app |

#### 4.2.2. Quản lý gói dịch vụ

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-10** | Mua gói dịch vụ sử dụng | Là chủ trang trại, tôi muốn xem danh sách các gói dịch vụ có sẵn và chọn mua gói phù hợp để sử dụng các tính năng nâng cao của hệ thống. | Farm Management | High | web-app |
| **BICAP-11** | Thanh toán khi mua gói dịch vụ | Là chủ trang trại, tôi muốn thực hiện thanh toán gói dịch vụ đã chọn thông qua các phương thức thanh toán trực tuyến (chuyển khoản ngân hàng, ví điện tử) để kích hoạt gói dịch vụ. | Farm Management | High | web-app |

#### 4.2.3. Quản lý mùa vụ (Farming Seasons)

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-12** | Xem các quy trình của mùa vụ | Là chủ trang trại, tôi muốn xem danh sách tất cả các quy trình mùa vụ (farming seasons) kèm theo trạng thái (đang canh tác, đã thu hoạch, đã xuất kho) để có cái nhìn tổng quan về hoạt động sản xuất. | Farm Management | Medium | web-app |
| **BICAP-13** | Xem chi tiết mùa vụ | Là chủ trang trại, tôi muốn xem chi tiết một mùa vụ bao gồm: thông tin giống cây/vật nuôi, ngày bắt đầu, ngày kết thúc dự kiến, các quy trình đã thực hiện, phân bón/thuốc đã sử dụng, hình ảnh minh chứng để theo dõi tiến trình sản xuất. | Farm Management | Medium | web-app |
| **BICAP-14** | Tạo mùa vụ mới (lưu vào blockchain) | Là chủ trang trại, tôi muốn tạo một mùa vụ mới với các thông tin: loại sản phẩm, giống, diện tích canh tác, ngày bắt đầu, quy trình dự kiến. **Thông tin mùa vụ sẽ được ghi nhận lên Blockchain** để đảm bảo tính minh bạch và không thể chỉnh sửa. | Farm Management | High | web-app, blockchain |
| **BICAP-15** | Cập nhật quy trình mùa vụ (lưu vào blockchain) | Là chủ trang trại, tôi muốn cập nhật các bước quy trình trong mùa vụ (bón phân, phun thuốc, tưới nước, thu hoạch...) kèm ngày thực hiện, hình ảnh, ghi chú. **Mỗi lần cập nhật sẽ được ghi nhận lên Blockchain** để tạo nhật ký sản xuất bất biến. | Farm Management | High | web-app, blockchain |
| **BICAP-16** | Xuất (export) một mùa vụ | Là chủ trang trại, tôi muốn thực hiện xuất kho cho một mùa vụ đã thu hoạch, bao gồm: số lượng xuất, ngày xuất, kho xuất. | Farm Management | Medium | web-app |
| **BICAP-17** | Tạo mã QR Code cho mỗi lần xuất mùa vụ (lưu vào blockchain) | Là chủ trang trại, tôi muốn hệ thống tự động tạo mã QR Code cho mỗi lần xuất kho. Mã QR chứa liên kết đến toàn bộ thông tin truy xuất nguồn gốc của lô hàng (quy trình mùa vụ, hình ảnh, chứng nhận). **Thông tin xuất kho được ghi nhận lên Blockchain.** | Farm Management | High | web-app, blockchain |

#### 4.2.4. Sàn giao dịch (Trading Floor)

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-18** | Đăng ký đẩy sản phẩm lên sàn giao dịch | Là chủ trang trại, tôi muốn đăng ký đưa sản phẩm đã xuất kho lên sàn giao dịch để các nhà bán lẻ có thể tìm kiếm và đặt mua. Thông tin bao gồm: tên sản phẩm, mô tả, số lượng, giá dự kiến, hình ảnh, mã QR. | Farm Management | Medium | web-app |
| **BICAP-19** | Xem trạng thái đăng ký đẩy lên sàn giao dịch | Là chủ trang trại, tôi muốn xem danh sách các sản phẩm đã đăng ký đẩy lên sàn kèm trạng thái (chờ duyệt, đang bán, đã bán hết, bị từ chối) để quản lý hoạt động bán hàng. | Farm Management | Low | web-app |

#### 4.2.5. Quản lý đơn hàng & vận chuyển

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-20** | Xử lý yêu cầu mua nông sản từ Nhà bán lẻ | Là chủ trang trại, tôi muốn xem và xử lý (chấp nhận/từ chối) các yêu cầu mua nông sản từ Nhà bán lẻ. Khi chấp nhận, đơn hàng chuyển sang trạng thái chờ vận chuyển. | Farm Management | High | web-app |
| **BICAP-21** | Xem thông tin Nhà bán lẻ đã ký hợp đồng | Là chủ trang trại, tôi muốn xem thông tin chi tiết của các Nhà bán lẻ đã ký hợp đồng (tên, địa chỉ, giấy phép kinh doanh, lịch sử giao dịch) để đánh giá đối tác. | Farm Management | Medium | web-app |
| **BICAP-22** | Xem và xem chi tiết quy trình vận chuyển | Là chủ trang trại, tôi muốn xem danh sách và chi tiết các đơn vận chuyển liên quan đến sản phẩm của mình, bao gồm: trạng thái, tài xế, vị trí hiện tại, thời gian dự kiến giao hàng. | Farm Management | Medium | web-app |
| **BICAP-23** | Xem báo cáo quy trình vận chuyển | Là chủ trang trại, tôi muốn xem báo cáo tổng hợp về quá trình vận chuyển (số đơn hoàn thành, tỷ lệ giao hàng đúng hạn, sự cố) để đánh giá chất lượng dịch vụ vận chuyển. | Farm Management | Low | web-app |

#### 4.2.6. Thông báo & Báo cáo

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-24** | Nhận thông báo báo cáo từ Nhà bán lẻ | Là chủ trang trại, tôi muốn nhận thông báo khi Nhà bán lẻ gửi báo cáo về sản phẩm, yêu cầu mua hàng hoặc phản hồi để kịp thời xử lý. | Farm Management | Low | web-app |
| **BICAP-25** | Nhận thông báo báo cáo từ Người vận chuyển (Shipper) | Là chủ trang trại, tôi muốn nhận thông báo khi Người vận chuyển (Shipper) gửi báo cáo về trạng thái vận chuyển (đã lấy hàng, đang giao, đã giao, sự cố) để theo dõi tiến trình giao hàng. | Farm Management | Low | web-app |
| **BICAP-26** | Nhận thông báo nhiệt độ, độ ẩm, độ pH trong ngày | Là chủ trang trại, tôi muốn nhận thông báo tự động từ hệ thống cảm biến IoT về các chỉ số nhiệt độ, độ ẩm, pH trong ngày để kịp thời điều chỉnh quy trình canh tác. | Farm Management | Medium | web-app, iot, notification |
| **BICAP-27** | Gửi báo cáo cho Admin | Là chủ trang trại, tôi muốn gửi báo cáo (khiếu nại, phản hồi, sự cố) cho Admin hệ thống để được hỗ trợ giải quyết. | Farm Management | Low | web-app |

---

### 4.3. EPIC-3: Retailer Web App

> **Mô tả Epic:** Ứng dụng Web dành cho Nhà bán lẻ: tìm kiếm, đặt mua nông sản, theo dõi đơn hàng & vận chuyển.

#### 4.3.1. Quản lý tài khoản & hồ sơ

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-36** | Đăng ký, đăng nhập tài khoản | Là nhà bán lẻ, tôi muốn đăng ký tài khoản mới và đăng nhập vào hệ thống bằng email/số điện thoại và mật khẩu để tham gia mua nông sản trên sàn giao dịch. | Retailer | High | web-app |
| **BICAP-37** | Cập nhật thông tin cá nhân chủ sở hữu | Là nhà bán lẻ, tôi muốn cập nhật thông tin cá nhân (họ tên, số điện thoại, địa chỉ, ảnh đại diện) để thông tin luôn chính xác. | Retailer | Medium | web-app |
| **BICAP-38** | Cập nhật Giấy phép kinh doanh và thông tin | Là nhà bán lẻ, tôi muốn tải lên Giấy phép kinh doanh và cập nhật thông tin cửa hàng/doanh nghiệp (tên, địa chỉ, loại hình kinh doanh) để hệ thống xác minh. | Retailer | Medium | web-app |

#### 4.3.2. Tìm kiếm & Mua hàng trên sàn

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-39** | Tìm kiếm nông sản trên sàn giao dịch | Là nhà bán lẻ, tôi muốn tìm kiếm nông sản trên sàn giao dịch theo các tiêu chí: tên sản phẩm, loại sản phẩm, vùng miền, chứng nhận, khoảng giá để tìm được sản phẩm phù hợp nhu cầu kinh doanh. | Retailer | High | web-app |
| **BICAP-40** | Xem chi tiết nông sản | Là nhà bán lẻ, tôi muốn xem chi tiết thông tin nông sản bao gồm: mô tả, hình ảnh, giá, số lượng còn, thông tin trang trại, chứng nhận, lịch sử mùa vụ để đánh giá chất lượng trước khi đặt mua. | Retailer | Medium | web-app |
| **BICAP-41** | Quét mã QR Code để lấy thông tin quy trình mùa vụ | Là nhà bán lẻ, tôi muốn quét mã QR Code của sản phẩm để xem toàn bộ thông tin truy xuất nguồn gốc từ Blockchain: quy trình mùa vụ, phân bón/thuốc sử dụng, ngày thu hoạch, chứng nhận. | Retailer | High | web-app, blockchain |
| **BICAP-42** | Tạo yêu cầu đặt mua nông sản | Là nhà bán lẻ, tôi muốn tạo yêu cầu đặt mua nông sản với thông tin: sản phẩm, số lượng, giá đề xuất, thời gian giao hàng mong muốn, địa chỉ nhận hàng để gửi đến chủ trang trại xem xét. | Retailer | High | web-app |
| **BICAP-43** | Thanh toán tiền đặt cọc mua nông sản | Là nhà bán lẻ, tôi muốn thực hiện thanh toán đặt cọc (theo tỷ lệ quy định) khi yêu cầu mua hàng được chấp nhận để xác nhận đơn hàng. | Retailer | High | web-app |
| **BICAP-44** | Hủy yêu cầu đặt mua nông sản | Là nhà bán lẻ, tôi muốn hủy yêu cầu đặt mua nông sản (khi đơn chưa được xử lý) kèm lý do hủy. Hệ thống cần thông báo cho chủ trang trại về việc hủy đơn. | Retailer | Medium | web-app |

#### 4.3.3. Quản lý đơn hàng

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-45** | Xem lịch sử đơn hàng | Là nhà bán lẻ, tôi muốn xem danh sách lịch sử tất cả các đơn hàng đã tạo kèm trạng thái (chờ xử lý, đã chấp nhận, đang vận chuyển, hoàn thành, đã hủy) để quản lý hoạt động mua hàng. | Retailer | Medium | web-app |
| **BICAP-46** | Xem chi tiết và trạng thái yêu cầu mua hàng | Là nhà bán lẻ, tôi muốn xem chi tiết đơn hàng bao gồm: thông tin sản phẩm, số lượng, giá, trạng thái xử lý, thông tin trang trại, tiến trình vận chuyển. | Retailer | Medium | web-app |

#### 4.3.4. Thông báo

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-47** | Nhận thông báo từ Quản lý Nông trại | Là nhà bán lẻ, tôi muốn nhận thông báo khi Quản lý Nông trại phản hồi yêu cầu mua hàng (chấp nhận/từ chối), cập nhật trạng thái đơn hàng. | Retailer | Low | web-app |
| **BICAP-48** | Gửi thông báo cho Quản lý Nông trại | Là nhà bán lẻ, tôi muốn gửi tin nhắn/thông báo đến Quản lý Nông trại để trao đổi về đơn hàng, thương lượng giá cả hoặc thời gian giao hàng. | Retailer | Low | web-app |

#### 4.3.5. Vận chuyển & Nhận hàng

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-49** | Xem và xem chi tiết quy trình vận chuyển | Là nhà bán lẻ, tôi muốn xem danh sách và chi tiết tiến trình vận chuyển của các đơn hàng (vị trí, trạng thái, tài xế, thời gian dự kiến) để chuẩn bị nhận hàng. | Retailer | Medium | web-app |
| **BICAP-50** | Nhận thông báo từ Người vận chuyển | Là nhà bán lẻ, tôi muốn nhận thông báo từ Người vận chuyển khi có cập nhật về trạng thái vận chuyển (đang giao, sắp đến, đã giao) để chuẩn bị nhận hàng. | Retailer | Low | web-app |
| **BICAP-51** | Xác nhận sản phẩm đã vận chuyển hoàn tất | Là nhà bán lẻ, tôi muốn xác nhận rằng hàng hóa đã được giao đầy đủ và đúng chất lượng để hoàn tất đơn hàng. | Retailer | High | web-app |
| **BICAP-52** | Tải lên hình ảnh sản phẩm đã vận chuyển hoàn tất | Là nhà bán lẻ, tôi muốn chụp và tải lên hình ảnh hàng hóa đã nhận được để làm bằng chứng giao nhận và phục vụ giải quyết tranh chấp (nếu có). | Retailer | Medium | web-app |

#### 4.3.6. Báo cáo

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-53** | Gửi báo cáo cho Admin | Là nhà bán lẻ, tôi muốn gửi báo cáo (khiếu nại chất lượng sản phẩm, sự cố vận chuyển, phản hồi) cho Admin hệ thống để được hỗ trợ. | Retailer | Low | web-app |

---

### 4.4. EPIC-4: Shipping Management Web App

> **Mô tả Epic:** Ứng dụng Web dành cho Quản lý vận chuyển: tạo lô hàng, quản lý phương tiện & tài xế.

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-54** | Xem các đơn hàng thành công giữa Nhà bán lẻ và Quản lý Nông trại | Là quản lý vận chuyển, tôi muốn xem danh sách các đơn hàng đã thỏa thuận thành công giữa Nhà bán lẻ và Quản lý Nông trại để tạo đơn vận chuyển tương ứng. | Shipping Management | High | web-app |
| **BICAP-55** | Tạo lô vận chuyển cho mỗi đơn hàng thành công | Là quản lý vận chuyển, tôi muốn tạo lô vận chuyển (shipment) cho mỗi đơn hàng thành công, bao gồm: chọn tài xế, chọn phương tiện, lộ trình, thời gian lấy/giao hàng dự kiến. | Shipping Management | High | web-app |
| **BICAP-56** | Hủy lô vận chuyển đã tạo | Là quản lý vận chuyển, tôi muốn hủy lô vận chuyển đã tạo (khi chưa bắt đầu thực hiện) kèm lý do hủy và thông báo cho các bên liên quan. | Shipping Management | Medium | web-app |
| **BICAP-57** | Xem quy trình vận chuyển | Là quản lý vận chuyển, tôi muốn theo dõi quy trình vận chuyển của tất cả các đơn hàng (trạng thái, vị trí tài xế, thời gian thực tế vs dự kiến) để đảm bảo giao hàng đúng hạn. | Shipping Management | Medium | web-app |
| **BICAP-58** | Quản lý phương tiện vận chuyển (Tạo, Cập nhật, Xóa, Xem) | Là quản lý vận chuyển, tôi muốn thêm mới, xem, cập nhật và xóa thông tin phương tiện vận chuyển (biển số, loại xe, tải trọng, tình trạng) để quản lý đội xe hiệu quả. | Shipping Management | Medium | web-app |
| **BICAP-59** | Quản lý tài xế vận chuyển (Tạo, Cập nhật, Xóa, Xem) | Là quản lý vận chuyển, tôi muốn thêm mới, xem, cập nhật và xóa thông tin tài xế (họ tên, CCCD, bằng lái, số điện thoại, phương tiện được phân công) để quản lý nhân sự. | Shipping Management | Medium | web-app |
| **BICAP-60** | Gửi báo cáo cho Admin | Là quản lý vận chuyển, tôi muốn gửi báo cáo hoạt động (tổng kết vận chuyển, sự cố, đề xuất) cho Admin hệ thống. | Shipping Management | Low | web-app |
| **BICAP-61** | Gửi thông báo cho Quản lý Nông trại và Nhà bán lẻ | Là quản lý vận chuyển, tôi muốn gửi thông báo cho Quản lý Nông trại và Nhà bán lẻ về tình trạng vận chuyển (lịch lấy/giao hàng, thay đổi, sự cố). | Shipping Management | Low | web-app |
| **BICAP-62** | Xem báo cáo từ Tài xế vận chuyển | Là quản lý vận chuyển, tôi muốn xem các báo cáo từ Tài xế vận chuyển (sự cố, khiếu nại, phản hồi) để xử lý kịp thời và đảm bảo chất lượng dịch vụ. | Shipping Management | Medium | web-app |

---

### 4.5. EPIC-5: Shipping Driver Mobile App

> **Mô tả Epic:** Ứng dụng Mobile dành cho Tài xế vận chuyển: cập nhật hành trình, xác nhận giao nhận.

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-63** | Xem và xem chi tiết các chuyến hàng của mình | Là tài xế, tôi muốn xem danh sách và chi tiết các chuyến hàng được giao cho mình kèm theo trạng thái (chờ lấy hàng, đang vận chuyển, đã giao) và thông tin hàng hóa, địa chỉ lấy/giao hàng. | Shipping Driver | High | mobile-app |
| **BICAP-64** | Cập nhật quy trình vận chuyển | Là tài xế, tôi muốn cập nhật quy trình vận chuyển (đã lấy hàng, đang di chuyển, sự cố, đã giao) kèm hình ảnh và ghi chú để các bên liên quan theo dõi. | Shipping Driver | High | mobile-app |
| **BICAP-65** | Quét mã QR Code để theo dõi sản phẩm khi về đến nông trại | Là tài xế, tôi muốn quét mã QR Code của lô hàng khi đến nông trại lấy hàng để xác minh thông tin sản phẩm và ghi nhận vào hệ thống. | Shipping Driver | High | mobile-app, blockchain |
| **BICAP-66** | Xác nhận đã nhận sản phẩm hoàn tất | Là tài xế, tôi muốn xác nhận đã nhận đủ hàng từ nông trại để hệ thống chuyển trạng thái đơn vận chuyển sang "đang vận chuyển". | Shipping Driver | High | mobile-app |
| **BICAP-67** | Xác nhận đã giao sản phẩm hoàn tất cho nhà bán lẻ | Là tài xế, tôi muốn xác nhận đã giao hàng cho nhà bán lẻ để hệ thống chuyển trạng thái đơn vận chuyển sang "đã giao" và thông báo cho các bên. | Shipping Driver | High | mobile-app |
| **BICAP-68** | Gửi báo cáo cho Shipping Manager | Là tài xế, tôi muốn gửi báo cáo (sự cố giao thông, hư hỏng hàng hóa, khiếu nại) cho Shipping Manager kèm hình ảnh và mô tả để được hỗ trợ xử lý. | Shipping Driver | Low | mobile-app |

---

### 4.6. EPIC-6: Guest App

> **Mô tả Epic:** Ứng dụng Web/Mobile dành cho Khách: tra cứu, tìm kiếm sản phẩm, nội dung giáo dục.

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-69** | Nhận thông báo chung về nền tảng (sản phẩm mới, bài viết, sự kiện) | Là khách truy cập, tôi muốn nhận các thông báo chung về nền tảng như: sản phẩm mới, bài viết giáo dục, sự kiện liên quan đến nông nghiệp bền vững để cập nhật thông tin. | Guest | Low | web-app, mobile-app, notification |
| **BICAP-70** | Tìm kiếm, lọc sản phẩm theo tiêu chí (nguồn gốc, loại sản phẩm, chứng nhận, tình trạng còn hàng) | Là khách truy cập, tôi muốn tìm kiếm và lọc sản phẩm theo các tiêu chí: nguồn gốc (vùng miền), loại sản phẩm, chứng nhận, tình trạng còn hàng để dễ dàng tìm sản phẩm quan tâm. | Guest | Medium | web-app, mobile-app |
| **BICAP-71** | Truy cập bài viết, video, nội dung giáo dục về nông nghiệp bền vững, an toàn thực phẩm | Là khách truy cập, tôi muốn đọc các bài viết, xem video và nội dung giáo dục liên quan đến nông nghiệp, canh tác bền vững, an toàn thực phẩm để nâng cao hiểu biết. | Guest | Low | web-app, mobile-app |

---

## 5. Yêu cầu Backend Web API

### EPIC-7: Backend Web API

> **Mô tả Epic:** API backend cho toàn hệ thống: xác thực, tích hợp blockchain VeChainThor, dữ liệu, thanh toán. (Các story dưới đây được suy ra từ công nghệ & yêu cầu phi chức năng.)

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-72** | API xác thực & phân quyền người dùng theo vai trò (Admin, Farm, Retailer, Shipping Manager, Ship Driver, Guest) | Xây dựng API xác thực (authentication) và phân quyền (authorization) dựa trên vai trò (RBAC) cho tất cả các loại người dùng trong hệ thống. | Backend/API | High | api |
| **BICAP-73** | API quản lý mùa vụ (farming season): CRUD, cập nhật quy trình, lưu dữ liệu vào blockchain | Xây dựng API CRUD cho mùa vụ và quy trình sản xuất, tích hợp ghi dữ liệu lên VeChainThor Blockchain khi tạo/cập nhật mùa vụ. | Backend/API | High | api, blockchain |
| **BICAP-74** | API tích hợp VeChainThor: ghi/đọc giao dịch, tạo & xác thực QR Code | Xây dựng API tích hợp trực tiếp với VeChainThor Blockchain để ghi/đọc giao dịch, tạo và xác thực mã QR Code cho truy xuất nguồn gốc. | Backend/API | High | api, blockchain |
| **BICAP-75** | API quản lý đơn hàng (đặt mua, đặt cọc, hủy đơn, xác nhận giao nhận) | Xây dựng API quản lý toàn bộ vòng đời đơn hàng: tạo yêu cầu mua, xử lý đặt cọc, hủy đơn, xác nhận giao nhận giữa Farm và Retailer. | Backend/API | High | api |
| **BICAP-76** | API quản lý vận chuyển (tạo shipment, cập nhật tiến trình, tracking) | Xây dựng API quản lý vận chuyển: tạo lô hàng, phân công tài xế, cập nhật tiến trình, theo dõi vị trí giao hàng. | Backend/API | High | api |
| **BICAP-77** | API thông báo (notification) real-time: báo cáo, IoT (nhiệt độ/độ ẩm/pH), trạng thái đơn hàng | Xây dựng API thông báo real-time hỗ trợ push notification cho báo cáo, dữ liệu IoT (nhiệt độ, độ ẩm, pH) và cập nhật trạng thái đơn hàng. | Backend/API | Medium | api, notification |
| **BICAP-78** | API thanh toán (mua gói dịch vụ, đặt cọc) | Xây dựng API tích hợp cổng thanh toán để xử lý thanh toán mua gói dịch vụ và đặt cọc đơn hàng. | Backend/API | High | api |
| **BICAP-79** | Thiết kế & triển khai cơ sở dữ liệu (MySQL 5.7.41, Redis 8.6) | Thiết kế schema cơ sở dữ liệu trên MySQL 5.7.41 và cấu hình Redis 8.6 cache cho hệ thống, đảm bảo hiệu năng và tính nhất quán dữ liệu. | Backend/API | High | api, database |
| **BICAP-80** | Đảm bảo blockchain xử lý nhiều giao dịch đồng thời khi dữ liệu IoT/yêu cầu tăng cao (yêu cầu phi chức năng) | Đảm bảo hệ thống blockchain VeChainThor có khả năng xử lý nhiều giao dịch đồng thời khi khối lượng dữ liệu IoT tăng hoặc khi có nhiều yêu cầu truy xuất thông tin sản phẩm cùng lúc. | Backend/API | High | api, blockchain, nfr |
| **BICAP-81** | Bảo mật dữ liệu & phân quyền truy cập theo chuẩn mã hóa VeChainThor (yêu cầu phi chức năng) | Triển khai bảo mật dữ liệu và phân quyền truy cập theo chuẩn mã hóa của VeChainThor, đảm bảo tính minh bạch và bất biến của dữ liệu truy xuất nguồn gốc. | Backend/API | High | api, security, nfr |

---

## 6. Yêu cầu Build – Deploy – Test

### EPIC-8: Build – Deploy – Test

> **Mô tả Epic:** Hạ tầng, triển khai và kiểm thử toàn hệ thống.

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-82** | Cấu hình hạ tầng mở rộng linh hoạt: AWS/Google Cloud, Docker, Redis 8.6 (yêu cầu phi chức năng) | Cấu hình hạ tầng cloud (AWS/Google Cloud), container hóa bằng Docker, cài đặt Redis 8.6 cache để hệ thống có khả năng mở rộng linh hoạt (horizontal scaling). | DevOps/QA | High | devops, nfr |
| **BICAP-83** | Thiết lập CI/CD pipeline | Thiết lập pipeline CI/CD (Continuous Integration / Continuous Deployment) cho toàn bộ hệ thống để tự động hóa quá trình build, test và deploy. | DevOps/QA | Medium | devops |
| **BICAP-84** | Triển khai Web App: Admin, Farm Management, Retailer, Shipping Management | Triển khai (deploy) tất cả các ứng dụng Web (Admin, Farm Management, Retailer, Shipping Management) lên môi trường production/staging. | DevOps/QA | High | devops |
| **BICAP-85** | Triển khai Mobile App: Shipping Driver, Guest | Triển khai (deploy) các ứng dụng Mobile (Shipping Driver, Guest) lên các store hoặc môi trường phân phối. | DevOps/QA | High | devops |
| **BICAP-86** | Kiểm thử chức năng (functional testing) từng module | Thực hiện kiểm thử chức năng cho từng module riêng lẻ (Admin, Farm, Retailer, Shipping Management, Shipping Driver, Guest) để đảm bảo tính đúng đắn. | DevOps/QA | High | qa |
| **BICAP-87** | Kiểm thử tích hợp (integration testing) toàn hệ thống | Thực hiện kiểm thử tích hợp để đảm bảo tất cả các module hoạt động chính xác khi kết hợp với nhau, bao gồm tích hợp Blockchain và API. | DevOps/QA | High | qa |
| **BICAP-88** | Kiểm thử hiệu năng & khả năng chịu tải (load/performance testing) | Thực hiện kiểm thử hiệu năng và khả năng chịu tải để đảm bảo hệ thống đáp ứng yêu cầu phi chức năng về thời gian phản hồi và số lượng người dùng đồng thời. | DevOps/QA | Medium | qa, nfr |
| **BICAP-89** | Kiểm thử bảo mật blockchain & phân quyền truy cập | Thực hiện kiểm thử bảo mật cho hệ thống blockchain và cơ chế phân quyền truy cập (RBAC) để đảm bảo dữ liệu được bảo vệ đúng cách. | DevOps/QA | High | qa, security |
| **BICAP-90** | User Acceptance Testing (UAT) | Thực hiện kiểm thử chấp nhận người dùng (UAT) với các stakeholders và người dùng cuối để xác nhận hệ thống đáp ứng yêu cầu nghiệp vụ. | DevOps/QA | Medium | qa |

---

## 7. Yêu cầu tài liệu dự án

### EPIC-9: Project Documentation

> **Mô tả Epic:** Tài liệu dự án: SRS, thiết kế, kiểm thử, hướng dẫn cài đặt & sử dụng.

| Mã YC | Tên yêu cầu (Jira Summary) | Mô tả chi tiết | Component | Độ ưu tiên | Labels |
|-------|---------------------------|-----------------|-----------|------------|--------|
| **BICAP-91** | User Requirements | Biên soạn tài liệu yêu cầu người dùng (User Requirements Document) mô tả chi tiết các yêu cầu từ phía người dùng cho toàn bộ hệ thống. | Documentation | Medium | docs |
| **BICAP-92** | Software Requirement Specifications (SRS) | Biên soạn tài liệu đặc tả yêu cầu phần mềm (SRS) chi tiết hóa các yêu cầu chức năng và phi chức năng thành đặc tả kỹ thuật. | Documentation | High | docs |
| **BICAP-93** | Architecture Design | Biên soạn tài liệu thiết kế kiến trúc hệ thống, bao gồm kiến trúc tổng thể, các component, tích hợp blockchain, và hạ tầng. | Documentation | High | docs |
| **BICAP-94** | Detail Design | Biên soạn tài liệu thiết kế chi tiết cho từng module, bao gồm class diagram, sequence diagram, database schema. | Documentation | High | docs |
| **BICAP-95** | System Implementation Document | Biên soạn tài liệu mô tả quá trình triển khai hệ thống, công nghệ sử dụng, cấu hình và quy trình phát triển. | Documentation | Medium | docs |
| **BICAP-96** | Testing Document | Biên soạn tài liệu kiểm thử bao gồm: test plan, test cases, test results cho tất cả các loại kiểm thử. | Documentation | Medium | docs |
| **BICAP-97** | Installation Guide | Biên soạn hướng dẫn cài đặt hệ thống, bao gồm yêu cầu phần cứng/phần mềm, các bước cài đặt và cấu hình. | Documentation | Medium | docs |
| **BICAP-98** | User Manual | Biên soạn hướng dẫn sử dụng cho từng vai trò người dùng (Admin, Farm Manager, Retailer, Shipping Manager, Shipping Driver, Guest). | Documentation | Medium | docs |

---

## 8. Yêu cầu phi chức năng

> **Ghi chú:** Các yêu cầu phi chức năng dưới đây được tham chiếu chéo (cross-referenced) trong các story của EPIC-7 (Backend Web API) và EPIC-8 (Build – Deploy – Test) trên Jira, được đánh dấu bằng label `nfr`.

### 8.1. Khả năng mở rộng (Scalability)

| Mã YC | Yêu cầu | Mô tả chi tiết | Jira Cross-ref |
|-------|---------|-----------------|----------------|
| **NFR-001** | Mở rộng linh hoạt | Hệ thống phải có khả năng mở rộng linh hoạt để xử lý số lượng lớn người dùng và truy vấn dữ liệu từ nhiều nguồn khác nhau. Các thành phần như AWS/Google Cloud, Docker và Redis 8.6 phải được cấu hình để hỗ trợ scaling liền mạch (horizontal scaling). | BDT-UR-001 |
| **NFR-002** | Xử lý giao dịch đồng thời trên Blockchain | Blockchain VeChainThor phải hỗ trợ xử lý nhiều giao dịch đồng thời khi khối lượng dữ liệu IoT tăng hoặc khi có nhiều yêu cầu truy xuất thông tin sản phẩm cùng lúc. | API-UR-009 |

### 8.2. Bảo mật (Security)

| Mã YC | Yêu cầu | Mô tả chi tiết | Jira Cross-ref |
|-------|---------|-----------------|----------------|
| **NFR-003** | Tính minh bạch & bất biến dữ liệu | Blockchain phải đảm bảo tính minh bạch và bất biến (immutability) của dữ liệu truy xuất nguồn gốc sản phẩm. Sử dụng các chuẩn mã hóa của VeChainThor để bảo vệ thông tin. | API-UR-010 |
| **NFR-004** | Phân quyền truy cập | Hệ thống phải hạn chế quyền truy cập dựa trên vai trò (Role-Based Access Control): Admin, Retailer, Transporter, Farm Manager, Guest — mỗi vai trò chỉ được truy cập các chức năng và dữ liệu phù hợp. | API-UR-001, BDT-UR-008 |

### 8.3. Hiệu năng (Performance)

| Mã YC | Yêu cầu | Mô tả chi tiết | Jira Cross-ref |
|-------|---------|-----------------|----------------|
| **NFR-005** | Thời gian phản hồi | Các thao tác thông thường (xem danh sách, tìm kiếm, xem chi tiết) phải có thời gian phản hồi dưới 2 giây trong điều kiện tải bình thường. | BDT-UR-007 |
| **NFR-006** | Cache hiệu quả | Sử dụng Redis 8.6 cache để giảm tải database và tăng tốc truy xuất dữ liệu thường xuyên được truy cập. | API-UR-008 |

### 8.4. Khả năng sử dụng (Usability)

| Mã YC | Yêu cầu | Mô tả chi tiết | Jira Cross-ref |
|-------|---------|-----------------|----------------|
| **NFR-007** | Giao diện thân thiện | Giao diện người dùng phải thân thiện, dễ sử dụng, phù hợp với nhiều đối tượng người dùng (bao gồm nông dân có hạn chế về kỹ năng công nghệ). | BDT-UR-009 |
| **NFR-008** | Responsive Design | Web App phải hiển thị tốt trên nhiều kích thước màn hình (desktop, tablet). Mobile App phải tương thích với cả Android và iOS. | BDT-UR-003, BDT-UR-004 |

### 8.5. Khả năng tích hợp (Integration)

| Mã YC | Yêu cầu | Mô tả chi tiết | Jira Cross-ref |
|-------|---------|-----------------|----------------|
| **NFR-009** | Tích hợp IoT | Hệ thống phải tích hợp với các cảm biến IoT để thu thập dữ liệu nhiệt độ, độ ẩm, pH tự động và ghi nhận lên hệ thống. | API-UR-006, FM-UR-020 |
| **NFR-010** | Tích hợp cổng thanh toán | Hệ thống phải tích hợp với cổng thanh toán trực tuyến để xử lý thanh toán gói dịch vụ và đặt cọc đơn hàng. | API-UR-007 |

---

## 9. Ràng buộc hệ thống

### 9.1. Công nghệ bắt buộc

| Thành phần | Công nghệ |
|-----------|-----------|
| **Backend / Server-side** | Java (Spring Boot 3.x) |
| **Database** | MySQL 5.7.41, Redis 8.6 |
| **Web Client** | ReactJS / Next.js (TypeScript) |
| **Mobile App** | React Native (TypeScript) |
| **Blockchain Platform** | VeChainThor |
| **Smart Contract Language** | Solidity |
| **Blockchain Dev Tools** | VeChain ToolChain, VeChain Sync, VeChain Stats |
| **Scripting / Build** | Java (JDK 21), TypeScript / JavaScript, Maven / Gradle |
| **Infrastructure** | AWS / Google Cloud, Docker |

### 9.2. Quy trình phát triển

- Áp dụng quy trình phát triển phần mềm chuẩn
- Sử dụng UML 2.0 trong mô hình hóa hệ thống
- Bộ tài liệu đầy đủ gồm: User Requirements, Software Requirement Specifications, Architecture Design, Detail Design, System Implementation, Testing Document, Installation Guide, Source Code, Deployable Packages

---

## 10. Phụ lục

### 10.1. Ma trận truy xuất yêu cầu — Jira Epic (Traceability Matrix)

| Epic ID | Epic Name | Số lượng Story |
|---------|-----------|------------|
| EPIC-1 | Admin Web App | 5 |
| EPIC-2 | Farm Management Web App | 21 |
| EPIC-3 | Retailer Web App | 18 |
| EPIC-4 | Shipping Management Web App | 9 |
| EPIC-5 | Shipping Driver Mobile App | 6 |
| EPIC-6 | Guest App | 3 |
| EPIC-7 | Backend Web API | 10 |
| EPIC-8 | Build – Deploy – Test | 9 |
| EPIC-9 | Project Documentation | 8 |
| | | **Tổng Story** | **89** |

### 10.2. Bảng tổng hợp theo Priority

| Priority | Số lượng Story |
|----------|----------------|
| High | 46 |
| Medium | 29 |
| Low | 14 |
| **Tổng** | **89** |

### 10.3. Bảng tổng hợp theo Labels

| Label | Mô tả | Số lượng Story liên quan |
|-------|-------|--------------------------|
| web-app | Story liên quan đến ứng dụng Web | 53 |
| mobile-app | Story liên quan đến ứng dụng Mobile | 9 |
| blockchain | Story có tương tác với Blockchain VeChainThor | 11 |
| api | Story liên quan đến Backend API | 10 |
| devops | Story liên quan đến hạ tầng và triển khai | 4 |
| qa | Story liên quan đến kiểm thử | 5 |
| docs | Story liên quan đến tài liệu | 8 |
| notification | Story liên quan đến thông báo | 3 |
| iot | Story liên quan đến IoT | 1 |
| security | Story liên quan đến bảo mật | 2 |
| nfr | Story triển khai yêu cầu phi chức năng | 4 |
| database | Story liên quan đến cơ sở dữ liệu | 1 |

### 10.4. Quy ước ký hiệu

| Ký hiệu | Ý nghĩa |
|---------|---------|
| **(Blockchain)** | Yêu cầu có tương tác ghi/đọc dữ liệu trên Blockchain VeChainThor |
| **CRUD** | Create (Tạo), Read (Đọc), Update (Cập nhật), Delete (Xóa) |
| **IoT** | Internet of Things — thiết bị cảm biến kết nối internet |
| **RBAC** | Role-Based Access Control — phân quyền dựa trên vai trò |
| **CI/CD** | Continuous Integration / Continuous Deployment |
| **UAT** | User Acceptance Testing — kiểm thử chấp nhận người dùng |
| **NFR** | Non-Functional Requirement — yêu cầu phi chức năng |

### 10.5. Bảng chú giải thuật ngữ

| Thuật ngữ | Giải thích |
|-----------|-----------|
| **Farming Season (Mùa vụ)** | Một chu kỳ sản xuất nông nghiệp từ khi gieo trồng đến khi thu hoạch |
| **Trading Floor (Sàn giao dịch)** | Nền tảng trực tuyến kết nối trang trại với nhà bán lẻ để mua bán nông sản |
| **Smart Contract** | Hợp đồng thông minh tự động thực thi trên Blockchain |
| **QR Code** | Mã phản hồi nhanh (Quick Response Code) chứa thông tin truy xuất nguồn gốc |
| **VeChainThor** | Nền tảng Blockchain công cộng dành cho doanh nghiệp, tập trung vào quản lý chuỗi cung ứng |
| **Truy xuất nguồn gốc** | Khả năng theo dõi nguồn gốc và quá trình sản xuất của sản phẩm |
| **Shipment (Lô vận chuyển)** | Đơn vị vận chuyển hàng hóa từ nông trại đến nhà bán lẻ |

---

> **Ghi chú**: Tài liệu này là phiên bản 2.0, đã được đồng bộ với cấu trúc Epic/Story trên Jira (file BICAP_Jira_Import.csv). Mọi thay đổi trên Jira cần được cập nhật lại vào tài liệu này.
