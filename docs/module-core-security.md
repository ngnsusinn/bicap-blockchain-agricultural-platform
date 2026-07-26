# Detail Design: Core Security & RBAC (BICAP-72)

## 1. Thông tin chung
- **Mã Epic:** EPIC-7 (Backend Web API)
- **Mã Yêu cầu:** BICAP-72 (API xác thực & phân quyền người dùng theo vai trò)
- **Người thực hiện:** Đội phát triển Core
- **Mục tiêu:** Xây dựng nền tảng cốt lõi (Core Security) sử dụng Spring Security, JWT và Role-based Access Control (RBAC) để hỗ trợ phân quyền cho toàn bộ hệ thống BICAP. Các module Đăng nhập/Đăng ký sẽ sử dụng lại lõi này.

## 2. Kiến trúc & Công nghệ sử dụng
- **Framework:** Spring Boot 3.3.0, Spring Security, Spring Data JPA.
- **Xác thực (Authentication):** Stateless Session, JSON Web Token (JWT).
- **Phân quyền (Authorization):** Kích hoạt `@EnableMethodSecurity` hỗ trợ annotation `@PreAuthorize("hasRole('...')")`.
- **Database:** MySQL (lưu trữ thực thể `User`).

## 3. Cấu trúc thư mục (Domain-Driven Design)
Toàn bộ mã nguồn liên quan được đặt tại base package `vn.courses.ut.edu.javaprogramming.bicap`:

```text
vn.courses.ut.edu.javaprogramming.bicap/
├── entity/
│   ├── User.java                       # Ánh xạ bảng Users dưới Database (implements UserDetails)
│   ├── Role.java                       # Thực thể Vai trò (SUPER_ADMIN, FARM_MANAGER...)
│   └── Permission.java                 # Thực thể Quyền hạn (MANAGE_USERS, READ_LOGS...)
│
├── repository/
│   └── UserRepository.java             # JPA Repository thao tác với Users
│
├── common/
│   └── security/                       # Tiện ích JWT & Filters
│       ├── CustomUserDetailsService.java # Nạp dữ liệu User từ DB vào Spring Security
│       ├── JwtAuthenticationFilter.java # Filter chặn HTTP Request xác thực JWT qua Header
│       └── JwtTokenProvider.java        # Class tiện ích sinh và giải mã Token
│
└── config/
    └── SecurityConfig.java             # Cấu hình Spring Security chính
```

## 4. Chi tiết các thành phần (Components)

### 4.1. Enum `Role`
Hỗ trợ đầy đủ các vai trò chính của hệ thống BICAP theo đặc tả SRS:

**Admin Roles (SRS-ADM-001):**
- `SUPER_ADMIN`: Siêu quản trị — có quyền tạo/xóa tài khoản Admin khác.
- `ADMIN`: Quản trị viên hệ thống.
- `MODERATOR`: Kiểm duyệt viên.

**Functional Roles (SRS-API-001):**
- `FARM_MANAGER`: Quản lý nông trại.
- `RETAILER`: Nhà bán lẻ.
- `SHIPPING_MGR`: Quản lý vận chuyển.
- `SHIP_DRIVER`: Tài xế giao hàng.
- `GUEST`: Khách.

**Lưu ý:** Theo đặc tả của SRS, một người dùng (`User`) có thể sở hữu cùng lúc nhiều vai trò (N:N). Vì vậy, trong `User.java` thuộc tính `roles` đã được thiết kế dưới dạng tập hợp (`Set<Role>`) với Annotation `@ElementCollection`.


### 4.2. `JwtTokenProvider`
Là hạt nhân sinh và giải mã JWT token.
- Sử dụng thuật toán `HS256`.
- Secret key và Expiration time được nạp từ biến môi trường thông qua `application.properties`.
- Các Developer phụ trách tính năng Đăng nhập (Login) sẽ dùng phương thức `generateToken(Authentication)` của class này để trả về token cho Client.

### 4.3. `JwtAuthenticationFilter`
Chạy tự động (OncePerRequest) trước mọi request.
- Quét Header `Authorization: Bearer <token>`.
- Xác thực chữ ký JWT bằng `JwtTokenProvider`.
- Nếu hợp lệ, đưa thông tin tài khoản và Role tương ứng vào `SecurityContextHolder`, giúp ứng dụng nhận diện người dùng đang thao tác.

### 4.4. `SecurityConfig`
- **Stateless:** Không lưu Session trên Server.
- **CSRF:** Bị vô hiệu hóa vì ứng dụng giao tiếp hoàn toàn qua REST API.
- **Public Endpoints:** Cấu hình mở sẵn (permitAll) cho các pattern `/api/auth/**` (đăng nhập, đăng ký) và `/api/public/**`. Các request khác đều yêu cầu phải có Token hợp lệ.

## 5. Hướng dẫn tích hợp cho Team
Các thành viên đảm nhận task **Đăng nhập / Đăng ký (BICAP-7, BICAP-36)** cần thực hiện:
1. Tạo các DTO (ví dụ `LoginRequest`, `RegisterRequest`) và Controller tương ứng tại thư mục `vn.courses.ut.edu.javaprogramming.bicap.controller`.
2. Khi User đăng nhập thành công, sử dụng logic sau để trả Token:
   ```java
   Authentication authentication = authenticationManager.authenticate(...);
   SecurityContextHolder.getContext().setAuthentication(authentication);
   String jwt = jwtTokenProvider.generateToken(authentication);
   // Trả jwt về cho Client
   ```
3. Ở bất kỳ Controller nào yêu cầu phân quyền, chỉ cần dùng annotation: 
   ```java
   @PreAuthorize("hasRole('ADMIN')") // Hoặc FARM_MANAGER, RETAILER...
   ```

## 6. Biến môi trường (.env)
Bắt buộc bổ sung các tham số sau vào cấu hình chạy:
```env
DB_URL=jdbc:mysql://localhost:3306/bicap_db?...
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=your_super_secret_key...
JWT_EXPIRATION_MS=86400000
```
