# BICAP – Blockchain Agricultural Platform
## Comprehensive Database Schema & Complete Test Flow

---

## PHẦN 1: DATABASE TABLES — BẢNG NÀO LƯU THÔNG TIN GÌ?

### 1.1 Core Identity & RBAC

#### 📋 `users`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính, tự tăng |
| email | VARCHAR (UNIQUE, NOT NULL) | Email đăng nhập (dùng làm username) |
| password | VARCHAR (NOT NULL) | Mật khẩu đã bcrypt |
| full_name | VARCHAR | Họ tên đầy đủ |
| phone | VARCHAR | Số điện thoại |
| status | ENUM(UserStatus) | PENDING_VERIFICATION / ACTIVE / INACTIVE / SUSPENDED |
| avatar_url | VARCHAR | URL ảnh đại diện |
| address | VARCHAR (500) | Địa chỉ |
| failed_login_attempts | INT | Số lần đăng nhập thất bại |
| locked_until | DATETIME | Thời gian lock tài khoản |
| created_at | DATETIME | Ngày tạo |
| **roles** (ManyToMany → `user_roles`) | Set<Role> | Vai trò (SUPER_ADMIN, ADMIN, FARM_MANAGER, RETAILER, SHIPPING_MGR, SHIP_DRIVER, GUEST) |
| **permissions** (ManyToMany → `user_permissions`) | Set<Permission> | Quyền bổ sung do SUPER_ADMIN gán |

#### 📋 `roles`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| name | VARCHAR (UNIQUE) | Tên vai trò |
| description | VARCHAR | Mô tả |
| **permissions** (ManyToMany → `role_permissions`) | Set<Permission> | Tập quyền của vai trò |

#### 📋 `permissions`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| code | VARCHAR (UNIQUE) | Mã quyền (VD: ADMIN_CREATE, ADMIN_READ...) |
| description | VARCHAR | Mô tả |

#### 📋 `user_roles` (junction table)
| Column | Type | Mô tả |
|--------|------|--------|
| user_id | BIGINT (FK → users) | ID người dùng |
| role_id | BIGINT (FK → roles) | ID vai trò |

#### 📋 `user_permissions` (junction table)
| Column | Type | Mô tả |
|--------|------|--------|
| user_id | BIGINT (FK → users) | ID người dùng |
| permission_id | BIGINT (FK → permissions) | ID quyền |

#### 📋 `role_permissions` (junction table)
| Column | Type | Mô tả |
|--------|------|--------|
| role_id | BIGINT (FK → roles) | ID vai trò |
| permission_id | BIGINT (FK → permissions) | ID quyền |

---

### 1.2 Farm & Agriculture

#### 📋 `farms`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| user_id | BIGINT (NOT NULL) | Owner (Farm Manager) — FK → users |
| name | VARCHAR (UNIQUE, NOT NULL) | Tên nông trại |
| address | VARCHAR (NOT NULL) | Địa chỉ |
| area | DOUBLE | Diện tích (ha) |
| gps_lat | DOUBLE | Vĩ độ GPS |
| gps_lng | DOUBLE | Kinh độ GPS |
| description | VARCHAR (2000) | Mô tả chi tiết |
| product_types | VARCHAR (500) | Loại sản phẩm (CSV: "rau, củ, quả") |
| admin_notes | VARCHAR (2000) | Ghi chú admin nội bộ |
| status | ENUM(FarmStatus) | PENDING / APPROVED / REJECTED / SUSPENDED / INACTIVE |
| created_at | DATETIME | Ngày tạo |
| updated_at | DATETIME | Ngày cập nhật |

#### 📋 `farm_certifications`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| farm_id | BIGINT (NOT NULL) | FK → farms |
| type | VARCHAR | Loại chứng nhận (license, organic, VietGAP...) |
| file_url | VARCHAR (NOT NULL) | URL file chứng nhận |
| expiry_date | DATE | Ngày hết hạn |
| created_at | DATETIME | Ngày tạo |

#### 📋 `farming_seasons`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| farm_id | BIGINT (NOT NULL) | FK → farms |
| name | VARCHAR (NOT NULL) | Tên mùa vụ |
| product_type | VARCHAR (NOT NULL, 100) | Loại sản phẩm |
| variety | VARCHAR (NOT NULL, 100) | Giống cây |
| area | DOUBLE (NOT NULL) | Diện tích |
| start_date | DATE (NOT NULL) | Ngày bắt đầu |
| end_date | DATE | Ngày kết thúc |
| status | VARCHAR (NOT NULL) | IN_PROGRESS / HARVESTED / CANCELLED |
| harvested_quantity | DECIMAL(16,2) | Sản lượng thu hoạch |
| harvest_unit | VARCHAR (30) | Đơn vị thu hoạch |
| tx_hash | VARCHAR (66) | Hash blockchain |
| created_at | DATETIME | Ngày tạo |

#### 📋 `farming_processes`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| season_id | BIGINT (NOT NULL) | FK → farming_seasons |
| process_type | VARCHAR (NOT NULL, 100) | SOIL_PREP / SEEDING / FERTILIZATION / PEST_CONTROL / HARVESTING |
| execution_date | DATE (NOT NULL) | Ngày thực hiện |
| materials | TEXT | Vật tư (JSON string) |
| images | TEXT | Ảnh (JSON array URLs) |
| notes | TEXT | Ghi chú |
| tx_hash | VARCHAR (66) | Hash blockchain |
| created_at | DATETIME | Ngày tạo |

#### 📋 `categories`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| name | VARCHAR (UNIQUE, NOT NULL, 100) | Tên danh mục |
| description | VARCHAR (500) | Mô tả |
| icon | VARCHAR (10) | Emoji icon UI |
| created_at | DATETIME | Ngày tạo |
| updated_at | DATETIME | Ngày cập nhật |

---

### 1.3 Products & Marketplace

#### 📋 `products`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| season_id | BIGINT (NOT NULL) | FK → farming_seasons |
| export_id | BIGINT | SeasonExport nguồn (BICAP-18) |
| category_id | BIGINT (NOT NULL) | FK → categories |
| name | VARCHAR (NOT NULL) | Tên sản phẩm |
| description | VARCHAR (2000) | Mô tả |
| images | TEXT | JSON array ảnh (1-10 ảnh) |
| price | DECIMAL(12,2) | Giá |
| quantity | DOUBLE | Số lượng |
| qr_code_id | BIGINT | ID QR code |
| status | VARCHAR (NOT NULL) | ACTIVE / INACTIVE / PENDING_REVIEW |
| created_at | DATETIME | Ngày tạo |

#### 📋 `season_exports`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| farm_id | BIGINT (NOT NULL) | FK → farms |
| season_id | BIGINT (NOT NULL) | FK → farming_seasons |
| quantity | DECIMAL(18,2) (NOT NULL) | Số lượng xuất |
| unit | VARCHAR (30, NOT NULL) | Đơn vị |
| export_date | DATE (NOT NULL) | Ngày xuất |
| warehouse | VARCHAR (255, NOT NULL) | Kho xuất |
| status | ENUM(ExportStatus) | BLOCKCHAIN_PENDING / READY / BLOCKCHAIN_FAILED / QR_FAILED |
| transaction_hash | VARCHAR (66) | Hash giao dịch VeChain |
| trace_hash | VARCHAR (66) | Hash truy xuất |
| chain_mode | VARCHAR (10) | LIVE / MOCK |
| qr_image | TEXT (Lob) | Ảnh QR code |
| idempotency_key | VARCHAR (100, NOT NULL) | Key chống trùng lặp |
| created_by | BIGINT (NOT NULL) | Người tạo |
| created_at | DATETIME | Ngày tạo |

#### 📋 `exports` (legacy)
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| season_id | BIGINT (NOT NULL) | FK → farming_seasons |
| export_date | DATE (NOT NULL) | Ngày xuất |
| quantity | DOUBLE (NOT NULL) | Số lượng |
| destination | VARCHAR (NOT NULL) | Điểm đến |
| tx_hash | VARCHAR (66) | Hash blockchain |
| created_at | DATETIME | Ngày tạo |

---

### 1.4 Orders & Payments

#### 📋 `orders`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| product_id | BIGINT | FK → products |
| retailer_id | BIGINT | FK → users (Nhà bán lẻ) |
| quantity | DOUBLE | Số lượng |
| price | DECIMAL | Giá |
| status | VARCHAR | PENDING/ACCEPTED/REJECTED/DEPOSIT_PAID/IN_TRANSIT/SHIPPING/CANCELLED/DELIVERED/COMPLETED |
| delivery_addr | VARCHAR | Địa chỉ giao hàng |
| desired_delivery_date | DATE | Ngày giao mong muốn |
| notes | VARCHAR (2000) | Ghi chú |
| accepted_at | DATETIME | Thời gian chấp nhận |
| deposit_rate | DOUBLE | Tỷ lệ cọc (mặc định 0.3) |
| deposit_code | VARCHAR (UNIQUE) | Mã cọc |
| deposit_amount | DECIMAL | Số tiền cọc |
| reject_reason | VARCHAR (1000) | Lý do từ chối (Farm Manager) |
| cancelled_reason | VARCHAR (1000) | Lý do hủy (Retailer) |
| cancel_requested_at | DATETIME | Thời gian yêu cầu hủy |
| delivered_at | DATETIME | Thời gian giao hàng (Farm Manager) |
| completed_at | DATETIME | Thời gian xác nhận nhận (Retailer) |
| completion_rating | INT | Đánh giá 1-5 |
| completion_comment | VARCHAR (1000) | Bình luận hoàn thành |
| delivery_images | VARCHAR (4000) | JSON ảnh giao hàng |
| created_at | DATETIME | Ngày tạo |

#### 📋 `payments`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| order_id | BIGINT | FK → orders |
| subscription_id | BIGINT | FK → subscriptions |
| amount | DECIMAL | Số tiền |
| method | ENUM(PaymentMethod) | BANK_TRANSFER / SEPAY / VNPAY |
| status | ENUM(PaymentStatus) | PENDING / COMPLETED / FAILED / REFUNDED |
| tx_ref | VARCHAR (UNIQUE) | Mã tham chiếu |
| created_at | DATETIME | Ngày tạo |

---

### 1.5 Shipping & Logistics

#### 📋 `shipments`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| order_id | BIGINT (UNIQUE, NOT NULL) | FK → orders (1-1) |
| driver_id | BIGINT | FK → drivers |
| vehicle_id | BIGINT | FK → vehicles |
| status | VARCHAR (NOT NULL) | PICKING_UP / IN_TRANSIT / DELIVERED / RETURNED |
| pickup_time | DATETIME | Thời gian lấy hàng |
| delivery_time | DATETIME | Thời gian giao |
| route_summary | VARCHAR (500) | Tóm tắt lộ trình |
| created_at | DATETIME | Ngày tạo |

#### 📋 `shipment_tracking`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| shipment_id | BIGINT (NOT NULL) | FK → shipments |
| status | VARCHAR (NOT NULL, 50) | Trạng thái checkpoint |
| gps_lat | DOUBLE (NOT NULL) | Vĩ độ GPS |
| gps_lng | DOUBLE (NOT NULL) | Kinh độ GPS |
| images | TEXT (JSON) | Ảnh checkpoint |
| notes | TEXT | Ghi chú |
| timestamp | DATETIME | Thời gian |

#### 📋 `drivers`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| user_id | BIGINT (UNIQUE, NOT NULL) | FK → users |
| citizen_id | VARCHAR (20, UNIQUE) | CCCD |
| license_number | VARCHAR (30, UNIQUE) | Bằng lái |
| vehicle_id | BIGINT | Xe hiện tại |
| status | VARCHAR | IDLE / ON_TRIP / OFFLINE |
| created_at | DATETIME | Ngày tạo |

#### 📋 `vehicles`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| license_plate | VARCHAR (20, UNIQUE) | Biển số |
| type | VARCHAR (50) | Loại xe |
| capacity | DOUBLE | Sức chứa |
| status | VARCHAR | AVAILABLE / IN_USE / MAINTENANCE |
| created_at | DATETIME | Ngày tạo |

---

### 1.6 Retailer Profile

#### 📋 `retailer_business_profiles`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| user_id | BIGINT (UNIQUE, NOT NULL) | FK → users (OneToOne) |
| business_name | VARCHAR | Tên doanh nghiệp |
| address | VARCHAR (500) | Địa chỉ |
| business_type | ENUM(BusinessType) | RETAIL_STORE / WHOLESALE / SUPERMARKET / OTHER |
| license_url | VARCHAR (1000) | URL giấy phép |

---

### 1.7 Blockchain

#### 📋 `smart_contracts`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| name | VARCHAR (100) | Tên contract |
| address | VARCHAR (42) | Địa chỉ VeChain |
| bytecode | LONGTEXT | Bytecode |
| abi | LONGTEXT | ABI |
| environment | VARCHAR | TESTNET / MAINNET |
| status | VARCHAR | PENDING / DEPLOYED / ACTIVE / INACTIVE / FAILED |
| version | VARCHAR | Phiên bản |
| tx_hash | VARCHAR (66) | Hash giao dịch |
| created_at | DATETIME | Ngày tạo |
| updated_at | DATETIME | Ngày cập nhật |

#### 📋 `blockchain_transactions`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| entity_type | VARCHAR (50) | SEASON / PROCESS / QR / EXPORT / CONTRACT |
| entity_id | BIGINT | ID entity liên quan |
| tx_hash | VARCHAR (66, UNIQUE) | Hash giao dịch |
| contract_address | VARCHAR (42) | Địa chỉ contract |
| status | VARCHAR | PENDING / CONFIRMED / FAILED |
| retry_count | INT | Số lần retry |
| idempotency_key | VARCHAR (100, UNIQUE) | Key chống trùng |
| created_at | DATETIME | Ngày tạo |

---

### 1.8 Subscriptions & Packages

#### 📋 `service_packages`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| name | VARCHAR | Tên gói |
| description | VARCHAR | Mô tả |
| price | DECIMAL | Giá |
| duration_days | INT | Thời gian (ngày) |
| features | TEXT (JSON) | Tính năng |
| status | VARCHAR | Trạng thái |
| created_at | DATETIME | Ngày tạo |

#### 📋 `subscriptions`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| farm_id | BIGINT (NOT NULL) | FK → farms |
| package_id | BIGINT (NOT NULL) | FK → service_packages |
| payment_code | VARCHAR | Mã thanh toán |
| start_date | DATE | Ngày bắt đầu |
| end_date | DATE | Ngày kết thúc |
| status | ENUM(SubscriptionStatus) | PENDING_PAYMENT / ACTIVE / EXPIRED / CANCELLED |
| request_status | ENUM(SubscriptionRequestStatus) | PENDING / APPROVED / REJECTED |
| created_at | DATETIME | Ngày tạo |

---

### 1.9 Notifications & Reports

#### 📋 `notifications`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| user_id | BIGINT | FK → users (null = Announcement toàn hệ thống) |
| type | VARCHAR | Loại thông báo |
| title | VARCHAR | Tiêu đề |
| content | VARCHAR | Nội dung |
| channel | VARCHAR | IN_APP / ... |
| is_read | BOOLEAN | Đã đọc |
| is_system | BOOLEAN | Là thông báo hệ thống |
| created_at | DATETIME | Ngày tạo |

#### 📋 `reports`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| reporter_id | BIGINT (NOT NULL) | FK → users (người gửi) |
| reporter_role | VARCHAR (30) | Vai trò tại thời điểm gửi |
| type | VARCHAR (20) | COMPLAINT / FEEDBACK / INCIDENT / OTHER |
| subject | VARCHAR (200) | Tiêu đề |
| content | VARCHAR (4000) | Nội dung |
| related_order_id | BIGINT | Đơn hàng liên quan |
| status | VARCHAR (20) | OPEN / IN_PROGRESS / RESOLVED / REJECTED |
| admin_response | VARCHAR (4000) | Phản hồi Admin |
| handled_by_id | BIGINT | Admin xử lý |
| handled_at | DATETIME | Thời gian xử lý |
| created_at | DATETIME | Ngày tạo |
| updated_at | DATETIME | Ngày cập nhật |

---

### 1.10 IoT & Educational

#### 📋 `iot_data`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| farm_id | BIGINT (NOT NULL) | FK → farms |
| temperature | DOUBLE | Nhiệt độ |
| humidity | DOUBLE | Độ ẩm |
| ph | DOUBLE | Độ pH |
| measured_at | DATETIME (NOT NULL) | Thời gian đo |

#### 📋 `educational_contents`
| Column | Type | Mô tả |
|--------|------|--------|
| id | BIGINT (PK) | Khóa chính |
| title | VARCHAR (200) | Tiêu đề |
| summary | VARCHAR (500) | Tóm tắt |
| content | TEXT | Nội dung |
| type | VARCHAR | ARTICLE / VIDEO |
| video_url | VARCHAR (500) | URL video |
| cover_image_url | VARCHAR (500) | Ảnh cover |
| tags | VARCHAR (300) | Tags |
| status | VARCHAR | PUBLISHED / DRAFT |
| published_at | DATETIME | Thời gian xuất bản |
| created_at | DATETIME | Ngày tạo |

---

### 1.11 Summary: Complete Table List (32 Bảng)

| # | Table | Domain |
|---|-------|--------|
| 1 | users | Identity |
| 2 | roles | RBAC |
| 3 | permissions | RBAC |
| 4 | user_roles | RBAC (junction) |
| 5 | user_permissions | RBAC (junction) |
| 6 | role_permissions | RBAC (junction) |
| 7 | farms | Farm |
| 8 | farm_certifications | Farm |
| 9 | farming_seasons | Farming |
| 10 | farming_processes | Farming |
| 11 | categories | Product |
| 12 | products | Product |
| 13 | season_exports | Export |
| 14 | exports (legacy) | Export |
| 15 | orders | Order/Payment |
| 16 | payments | Payment |
| 17 | shipments | Shipping |
| 18 | shipment_tracking | Shipping |
| 19 | drivers | Shipping |
| 20 | vehicles | Shipping |
| 21 | retailer_business_profiles | Retailer |
| 22 | smart_contracts | Blockchain |
| 23 | blockchain_transactions | Blockchain |
| 24 | service_packages | Subscription |
| 25 | subscriptions | Subscription |
| 26 | notifications | Notification |
| 27 | reports | Report |
| 28 | iot_data | IoT |
| 29 | educational_contents | Education |

---

## PHẦN 2: COMPLETE TEST FLOW — TỪNG USER TYPE

### 2.1 User Types & Credentials (từ DatabaseSeeder)

| Role | Email | Password | Mô tả |
|------|-------|----------|--------|
| **SUPER_ADMIN** | superadmin@bicap.com | Superadmin@2026 | Quản trị tối cao |
| **ADMIN** | admin@bicap.com | Adminpassword@2026 | Quản trị viên |
| **MODERATOR** | moderator@bicap.com | Moderator@2026 | Chỉ đọc admin |
| **FARM_MANAGER** | farm@bicap.com | Farmpassword@2026 | Chủ nông trại 1 |
| **FARM_MANAGER** | farm@bicap.vn | Farmpassword@2026 | Chủ nông trại 2 |
| **RETAILER** | retailer@bicap.com | Retailpassword@2026 | Nhà bán lẻ 1 |
| **RETAILER** | retail@bicap.com | Retailpassword@2026 | Nhà bán lẻ 2 |
| **SHIPPING_MGR** | shipping_mgr@bicap.com | Shipping@2026 | Quản lý vận chuyển |
| **SHIP_DRIVER** | driver@bicap.com | Driver@2026 | Tài xế 1 |
| **SHIP_DRIVER** | driver2@bicap.com | Driver@2026 | Tài xế 2 |
| **GUEST** | Không cần đăng nhập | — | Khách truy cập |

---

### 2.2 TEST FLOW — GUEST (Khách truy cập)

```
✅ CÓ THỰC HIỆN (không cần đăng nhập):
─────────────────────────────────────────
1. GET /api/public/products              → Xem danh sách sản phẩm ACTIVE (public catalogue)
2. GET /api/public/products/{id}         → Xem chi tiết sản phẩm (public)
3. GET /api/public/products/trace/{hash} → Truy xuất nguồn gốc (public)
4. GET /api/public/education             → Xem bài viết/khóa học (PUBLISHED)
5. GET /api/public/education/{id}        → Xem chi tiết bài viết (PUBLISHED)
6. GET /api/categories                   → Xem danh mục sản phẩm
7. GET /api/service-packages             → Xem gói dịch vụ ACTIVE
8. GET /api/notifications                → Xem thông báo hệ thống (is_system=true)
9. GET /api/trace/{hash}                 → Truy xuất QR code
10. GET /api/marketplace/products/trace/{hash} → Truy xuất sản phẩm
11. GET / (root)                          → Load web app shell
12. GET /admin/**                         → Load admin dashboard shell (redirect login)
13. GET /trace/{hash}                     → Load trace page shell

❌ KHÔNG THỰC HIỆN (cần xác thực):
─────────────────────────────────────
- Mọi API /api/** khác (auth, farm, order, shipping, reports, etc.)
```

---

### 2.3 TEST FLOW — FARM_MANAGER (Chủ nông trại)

**Login:** `POST /api/farm/login` hoặc `POST /api/auth/farm/login`
**Header:** `Authorization: Bearer <JWT>`

```
PHẦN A: AUTH & PROFILE
─────────────────────────
1. POST /api/farm/login → JWT token
2. GET /api/profile → Xem thông tin cá nhân
3. PUT /api/profile → Cập nhật hồ sơ (avatar, fullName, phone, address)
4. POST /api/profile/change-password → Đổi mật khẩu

PHẦN B: FARM REGISTRATION (BICAP-7 / BICAP-3)
───────────────────────────────────────────────
5. POST /api/farms/register → Đăng ký nông trại mới (trạng thái PENDING)
6. GET /api/farms/my → Xem các nông trại của mình
7. GET /api/farms/{farmId} → Xem chi tiết nông trại
8. PUT /api/farms/{farmId} → Cập nhật thông tin nông trại
9. POST /api/farms/{farmId}/certifications (multipart) → Tải lên giấy phép
10. GET /api/farms/{farmId}/certifications → Xem danh sách chứng nhận

PHẦN C: SEASON & PRODUCT (BICAP-18/19)
─────────────────────────────────────────
11. POST /api/farms/{farmId}/seasons → Tạo mùa vụ mới
12. GET /api/farms/{farmId}/seasons → Xem danh sách mùa vụ
13. GET /api/farms/{farmId}/seasons/{seasonId} → Xem chi tiết mùa vụ (kèm processes + exports)
14. PUT /api/farms/{farmId}/seasons/{seasonId} → Cập nhật mùa vụ
15. PATCH /api/farms/{farmId}/seasons/{seasonId}/status → Cập nhật trạng thái mùa vụ
16. POST /api/seasons/{seasonId}/processes → Thêm bước canh tác
17. GET /api/seasons/{seasonId}/processes → Xem danh sách bước canh tác
18. GET /api/seasons/{seasonId}/processes/{processId} → Xem chi tiết bước
19. PUT /api/seasons/{seasonId}/processes/{processId} → Cập nhật bước canh tác
20. POST /api/seasons/{seasonId}/exports → Tạo lô xuất kho (phải HARVESTED)
21. GET /api/seasons/{seasonId}/exports → Xem danh sách lô xuất
22. GET /api/seasons/{seasonId}/exports/{exportId} → Xem chi tiết lô xuất

PHẦN D: TRADING FLOOR (Sàn giao dịch - BICAP-18/19)
──────────────────────────────────────────────────────
23. POST /api/farms/{farmId}/marketplace/products (multipart) → Đẩy sản phẩm lên sàn
24. GET /api/farms/{farmId}/marketplace/products → Xem sản phẩm đã đẩy lên sàn
25. GET /api/categories → Xem danh mục cho form đăng ký

PHẦN E: SEASON EXPORT (BICAP-75)
──────────────────────────────────
26. POST /api/farms/{farmId}/seasons/{seasonId}/export → Tạo xuất kho + QR + blockchain
27. GET /api/farms/{farmId}/exports → Xem danh sách xuất kho
28. GET /api/trace/{hash} → Truy xuất theo trace hash (public)

PHẦN F: ORDERS (Yêu cầu mua - BICAP-20)
─────────────────────────────────────────
29. GET /api/orders → Xem danh sách yêu cầu mua trên nông trại
30. GET /api/orders/{id} → Xem chi tiết yêu cầu mua
31. PUT /api/orders/{id}/accept → Chấp nhận yêu cầu mua
32. PUT /api/orders/{id}/reject → Từ chối yêu cầu mua (bắt buộc nhập lý do)

PHẦN G: SHIPMENT (BICAP-22/23)
─────────────────────────────────
33. GET /api/farms/{farmId}/shipments → Xem vận chuyển của nông trại
34. GET /api/farms/{farmId}/shipments/summary → Xem tổng quan vận chuyển
35. GET /api/farms/{farmId}/shipments/{shipmentId} → Xem chi tiết vận chuyển

PHẦN H: RETAILER PARTNERS (BICAP-21)
──────────────────────────────────────
36. GET /api/retailers → Xem danh sách nhà bán lẻ đã giao dịch
37. GET /api/retailers/{id} → Xem chi tiết đối tác + lịch sử giao dịch

PHẦN I: IOT DATA (BICAP-14)
─────────────────────────────
38. POST /api/iot/sensors → Nhận dữ liệu cảm biến (nhiệt độ, độ ẩm, pH)

PHẦN J: REPORTS (BICAP-27)
────────────────────────────
39. POST /api/reports → Gửi báo cáo cho Admin
40. GET /api/reports/my → Xem báo cáo đã gửi

PHẦN K: SUBSCRIPTIONS
──────────────────────
41. GET /api/subscriptions/my → Xem đăng ký của mình
42. GET /api/subscriptions/farm/{farmId} → Xem đăng ký theo nông trại
43. PUT /api/subscriptions/{id}/cancel → Hủy đăng ký
44. PUT /api/subscriptions/current/cancel → Hủy đăng ký hiện tại

PHẦN L: NOTIFICATIONS
───────────────────────
45. GET /api/notifications → Xem thông báo cá nhân
46. GET /api/notifications/unread-count → Đếm chưa đọc
47. PUT /api/notifications/{id}/read → Đánh dấu đã đọc
48. PUT /api/notifications/read-all → Đánh dấu tất cả đã đọc
49. GET /api/notifications/stream → SSE stream (realtime)
```

---

### 2.4 TEST FLOW — RETAILER (Nhà bán lẻ)

**Login:** `POST /api/retailer/login` hoặc `POST /api/auth/retailer/login`
**Header:** `Authorization: Bearer <JWT>`

```
PHẦN A: AUTH & PROFILE
─────────────────────────
1. POST /api/retailer/login → JWT token
2. POST /api/retailer/refresh → Làm mới token
3. GET /api/retailer/profile → Xem profile cá nhân
4. PUT /api/retailer/profile (multipart) → Cập nhật profile
5. GET /api/retailer/business-profile → Xem hồ sơ doanh nghiệp (404 nếu chưa có)
6. POST /api/retailer/documents (multipart) → Tạo/cập nhật hồ sơ doanh nghiệp
7. PUT /api/retailer/business-profile (multipart) → Alias tạo hồ sơ doanh nghiệp

PHẦN B: MARKETPLACE (BICAP-39/40/41)
──────────────────────────────────────
8. GET /api/marketplace/products → Tìm kiếm sản phẩm (keyword, category, region, price...)
9. GET /api/marketplace/products/{id} → Xem chi tiết sản phẩm
10. GET /api/marketplace/products/trace/{hash} → Truy xuất nguồn gốc

PHẦN C: ORDERS (BICAP-75/76)
──────────────────────────────
11. POST /api/orders → Đặt đơn hàng mới (PENDING)
12. GET /api/orders/my → Xem danh sách đơn hàng của mình
13. GET /api/orders/my/{id} → Xem chi tiết đơn hàng
14. PUT /api/orders/{id}/cancel → Hủy đơn (chỉ PENDING/ACCEPTED)
15. PUT /api/orders/{id}/complete → Xác nhận nhận hàng (DELIVERED → COMPLETED)
16. POST /api/orders/{id}/delivery-images (multipart) → Tải ảnh xác nhận giao hàng

PHẦN D: SHIPMENT TRACKING (BICAP-49)
─────────────────────────────────────
17. GET /api/retailer/shipments → Xem lô vận chuyển liên quan đến đơn hàng
18. GET /api/retailer/shipments/{id} → Xem chi tiết lô vận chuyển + GPS

PHẦN E: DEPOSIT (BICAP-75)
────────────────────────────
19. POST /api/orders/deposit → Tạo cọc thanh toán

PHẦN F: NOTIFICATIONS
───────────────────────
20. GET /api/notifications → Xem thông báo cá nhân + hệ thống
21. GET /api/notifications/unread-count → Đếm chưa đọc
22. PUT /api/notifications/{id}/read → Đánh dấu đã đọc
23. PUT /api/notifications/read-all → Đánh dấu tất cả đã đọc
24. GET /api/notifications/stream → SSE stream

PHẦN G: REPORTS
─────────────────
25. POST /api/reports → Gửi báo cáo/phản hồi cho Admin
26. GET /api/reports/my → Xem báo cáo đã gửi

PHẦN H: NOTIFY FARM MANAGER (BICAP-48)
─────────────────────────────────────────
27. POST /api/retailer/notify-farm → Gửi tin nhắn cho Farm Manager qua đơn hàng

PHẦN I: SUBSCRIPTIONS
──────────────────────
28. POST /api/subscriptions/purchase → Mua gói đăng ký nông trại
29. GET /api/subscriptions/my → Xem đăng ký của mình
```

---

### 2.5 TEST FLOW — SHIPPING_MGR (Quản lý vận chuyển)

**Login:** `POST /api/shipping/login`
**Header:** `Authorization: Bearer <JWT>`

```
PHẦN A: ORDERS (Read-only)
───────────────────────────
1. GET /api/shipping/orders/ready-to-ship → Đơn DEPOSIT_PAID chờ vận chuyển
2. GET /api/shipping/orders/completed → Đơn đã COMPLETED
3. GET /api/shipping/driver-reports?shipmentId= → Báo cáo tài xế (BICAP-62)
4. GET /api/shipping/driver-users → Người dùng SHIP_DRIVER chưa có profile

PHẦN B: SHIPMENTS (BICAP-76)
──────────────────────────────
5. GET /api/shipping/shipments → Xem danh sách vận chuyển
6. GET /api/shipping/shipments/{id} → Xem chi tiết + GPS tracking
7. POST /api/shipping/shipments → Tạo vận chuyển từ DEPOSIT_PAID order
8. PUT /api/shipping/shipments/{id}/cancel → Hủy vận chuyển (chỉ PICKING_UP)

PHẦN C: VEHICLES
──────────────────
9. GET /api/shipping/vehicles → Xem danh sách xe
10. POST /api/shipping/vehicles → Thêm xe mới
11. PUT /api/shipping/vehicles/{id} → Cập nhật xe
12. DELETE /api/shipping/vehicles/{id} → Xóa xe

PHẦN D: DRIVERS
─────────────────
13. GET /api/shipping/drivers → Xem danh sách tài xế
14. POST /api/shipping/drivers → Thêm tài xế mới
15. PUT /api/shipping/drivers/{id} → Cập nhật tài xế
16. DELETE /api/shipping/drivers/{id} → Xóa tài xế
17. PUT /api/shipping/drivers/{id}/assign?vehicleId= → Gán xe cho tài xế

PHẦN E: REPORTS
─────────────────
18. POST /api/reports → Gửi báo cáo cho Admin
19. GET /api/reports/my → Xem báo cáo đã gửi

PHẦN F: NOTIFICATIONS
───────────────────────
20. GET /api/notifications → Xem thông báo cá nhân
21. GET /api/notifications/unread-count → Đếm chưa đọc
22. PUT /api/notifications/{id}/read → Đánh dấu đã đọc
23. PUT /api/notifications/read-all → Đánh dấu tất cả đã đọc
24. GET /api/notifications/stream → SSE stream
```

---

### 2.6 TEST FLOW — SHIP_DRIVER (Tài xế)

**Login:** `POST /api/driver/login`
**Header:** `Authorization: Bearer <JWT>`

```
PHẦN A: MY SHIPMENTS
─────────────────────
1. GET /api/driver/shipments → Xem đơn hàng đang vận chuyển của mình
2. GET /api/driver/shipments/{id} → Xem chi tiết + GPS tracking

PHẦN B: TRACKING
──────────────────
3. POST /api/driver/shipments/{id}/tracking → Thêm GPS checkpoint

PHẦN C: PICKUP & DELIVERY
───────────────────────────
4. POST /api/driver/shipments/{id}/pickup → Xác nhận lấy hàng (PICKING_UP → IN_TRANSIT)
5. POST /api/driver/shipments/{id}/deliver → Xác nhận giao hàng (IN_TRANSIT → DELIVERED)

PHẦN D: INCIDENT REPORTS
─────────────────────────
6. POST /api/driver/reports → Báo cáo sự cố/trễ/hư hỏng

PHẦN E: REPORTS (BICAP-27)
────────────────────────────
7. POST /api/reports → Gửi báo cáo cho Admin
8. GET /api/reports/my → Xem báo cáo đã gửi

PHẦN F: NOTIFICATIONS
───────────────────────
9. GET /api/notifications → Xem thông báo cá nhân
10. GET /api/notifications/unread-count → Đếm chưa đọc
11. PUT /api/notifications/{id}/read → Đánh dấu đã đọc
12. PUT /api/notifications/read-all → Đánh dấu tất cả đã đọc
13. GET /api/notifications/stream → SSE stream
```

---

### 2.7 TEST FLOW — ADMIN (Quản trị viên)

**Login:** `POST /api/admin/login`
**Headers:** 
- `Authorization: Bearer <JWT>` (cho portal endpoints)
- `X-Actor-Email: admin@bicap.com` (cho admin endpoints)

```
PHẦN A: ADMIN ACCOUNT MANAGEMENT
───────────────────────────────────
1. GET /api/admins → Xem danh sách admin
2. GET /api/admins?status=ACTIVE&role=ADMIN → Lọc theo trạng thái/vai trò
3. GET /api/admins/{id} → Xem chi tiết admin
4. POST /api/admins → Tạo admin mới
5. PUT /api/admins/{id} → Cập nhật admin
6. DELETE /api/admins/{id} → Xóa admin
7. GET /api/admins/permissions → Xem catalogue permissions

PHẦN B: ADMIN DASHBOARD
─────────────────────────
8. GET /api/admin/dashboard → Xem thống kê tổng quan

PHẦN C: FARM APPROVAL (BICAP-3)
─────────────────────────────────
9. GET /api/admin/farms → Xem danh sách nông trại chờ duyệt
10. GET /api/admin/farms?status=PENDING → Lọc theo trạng thái
11. GET /api/admin/farms/{id} → Xem chi tiết nông trại
12. GET /api/admin/farms/stats → Thống kê theo trạng thái
13. PUT /api/admin/farms/{id}/approve → Duyệt nông trại
14. PUT /api/admin/farms/{id}/reject → Từ chối nông trại (bắt buộc nhập lý do)
15. PUT /api/admin/farms/{id}/status → Cập nhật trạng thái (SUSPENDED, INACTIVE...)
16. PUT /api/admin/farms/{id}/notes → Cập nhật ghi chú admin

PHẦN D: PRODUCT MANAGEMENT (BICAP-5)
───────────────────────────────────────
17. GET /api/admin/products → Xem danh sách sản phẩm (admin monitoring)
18. GET /api/admin/products/stats → Thống kê sản phẩm
19. GET /api/admin/products/{id} → Xem chi tiết sản phẩm
20. PUT /api/admin/products/{id}/status → Cập nhật trạng thái sản phẩm

PHẦN E: CATEGORY MANAGEMENT
─────────────────────────────
21. GET /api/admin/products/categories → Xem danh mục
22. GET /api/admin/products/categories/{id} → Xem chi tiết danh mục
23. POST /api/admin/products/categories → Tạo danh mục mới
24. PUT /api/admin/products/categories/{id} → Cập nhật danh mục
25. DELETE /api/admin/products/categories/{id} → Xóa danh mục

PHẦN F: ANNOUNCEMENTS (BICAP-69)
───────────────────────────────────
26. POST /api/admin/announcements → Tạo thông báo hệ thống (guest đọc được)

PHÀN G: BLOCKCHAIN (BICAP-6/80)
─────────────────────────────────
27. GET /api/admin/contracts → Xem danh sách smart contract
28. GET /api/admin/contracts/{id} → Xem chi tiết contract
29. GET /api/admin/contracts/blockchain-status → Kiểm tra chế độ live/mock
30. PUT /api/admin/contracts/{id} → Cập nhật metadata contract
31. PUT /api/admin/contracts/{id}/status → Cập nhật trạng thái contract
32. POST /api/admin/contracts/deploy → Triển khai contract mới (SUPER_ADMIN)
33. GET /api/blockchain/transactions → Xem danh sách giao dịch blockchain
34. POST /api/blockchain/transactions/{id}/retry → Retry giao dịch thất bại

PHẦN H: REPORTS (Admin)
─────────────────────────
35. GET /api/reports/admin → Xem tất cả báo cáo
36. GET /api/reports/admin?status=OPEN → Lọc theo trạng thái
37. GET /api/reports/admin/stats → Thống kê báo cáo
38. GET /api/reports/admin/{id} → Xem chi tiết báo cáo
39. PUT /api/reports/admin/{id}/handle → Xử lý báo cáo (đóng/mở + phản hồi)

PHẦN I: SUBSCRIPTIONS (Admin)
───────────────────────────────
40. GET /api/subscriptions/admin/requests → Xem yêu cầu đăng ký
41. PUT /api/subscriptions/admin/requests/{id}/approve → Duyệt yêu cầu
42. PUT /api/subscriptions/admin/requests/{id}/reject → Từ chối yêu cầu
43. GET /api/subscriptions/payment-status/{paymentCode} → Kiểm tra trạng thái thanh toán

PHẦN J: SERVICE PACKAGES (Admin)
──────────────────────────────────
44. GET /api/service-packages/admin/all → Xem tất cả gói (ACTIVE + INACTIVE)
45. POST /api/service-packages/admin → Tạo gói mới
46. PUT /api/service-packages/admin/{id} → Cập nhật gói
47. DELETE /api/service-packages/admin/{id} → Xóa gói (soft delete)

PHẦN K: NOTIFICATIONS (Admin)
────────────────────────────────
48. POST /api/notifications/broadcast → Phát thông báo đến nhiều người

PHẦN L: EXPORT (Admin - F12 fix)
──────────────────────────────────
49. GET /api/seasons/{seasonId}/exports → Xem exports (admin-view chỉ đọc)

PHẦN M: REPORTS (Admin view - BICAP-53/60/68)
───────────────────────────────────────────────
50. POST /api/reports → Tạo báo cáo (admin, BICAP-53)
51. PUT /api/reports/admin/{id}/handle → Xử lý báo cáo (BICAP-60, BICAP-68)
```

---

### 2.8 TEST FLOW — SUPER_ADMIN (Siêu quản trị viên)

**Login:** `POST /api/admin/login`
**Headers:** 
- `Authorization: Bearer <JWT>`
- `X-Actor-Email: superadmin@bicap.com`

```
TẤT CẢ CÁC ENDPOINT CỦA ADMIN +:
────────────────────────────────────
1. POST /api/admins → Tạo admin (SUPER_ADMIN chỉ)
2. DELETE /api/admins/{id} → Xóa admin (SUPER_ADMIN chỉ)
3. POST /api/admin/contracts/deploy → Triển khai contract (SUPER_ADMIN chỉ)
4. POST /api/admin/contracts/deploy với mọi tham số → Deploy contract đầy đủ

PHẦN B: PERMISSIONS ASSIGNMENT (F2)
──────────────────────────────────────
5. PUT /api/admins/{id} → Gán permissions trực tiếp cho user (per account)
6. GET /api/admins/permissions → Xem catalogue permissions để gán

PHẦN C: BLOCKCHAIN DEPLOY
───────────────────────────
7. POST /api/admin/contracts/deploy → Deploy smart contract mới
   Body: {name, bytecode, abi, environment, version}

PHẦN D: FULL ACCESS TO ALL MODULES
─────────────────────────────────────
→ Test all endpoints from ADMIN flow + FARM_MANAGER + RETAILER + SHIPPING_MGR + DRIVER flows
  (SUPER_ADMIN can access everything)
```

---

### 2.9 TEST FLOW — MODERATOR (Chỉ đọc)

**Login:** `POST /api/admin/login`
**Headers:** 
- `Authorization: Bearer <JWT>`
- `X-Actor-Email: moderator@bicap.com`

```
PHẦN A: READ-ONLY ADMIN VIEWS
─────────────────────────────────
1. GET /api/admins → Xem danh sách admin (read-only)
2. GET /api/admin/farms → Xem danh sách nông trại (read-only)
3. GET /api/admin/farms/{id} → Xem chi tiết nông trại (read-only)
4. GET /api/admin/farms/stats → Xem thống kê (read-only)
5. GET /api/admin/products → Xem sản phẩm (read-only)
6. GET /api/admin/products/stats → Xem thống kê (read-only)
7. GET /api/admin/products/{id} → Xem chi tiết (read-only)
8. GET /api/admin/contracts → Xem contract (read-only)
9. GET /api/admin/contracts/{id} → Xem chi tiết (read-only)
10. GET /api/admin/contracts/blockchain-status → Xem trạng thái blockchain
11. GET /api/blockchain/transactions → Xem giao dịch blockchain (read-only)
12. GET /api/admin/dashboard → Xem dashboard (read-only)
13. GET /api/reports/admin → Xem báo cáo (read-only)
14. GET /api/reports/admin/{id} → Xem chi tiết báo cáo (read-only)
15. GET /api/subscriptions/admin/requests → Xem yêu cầu đăng ký (read-only)
16. GET /api/service-packages/admin/all → Xem gói dịch vụ (read-only)
17. GET /api/seasons/{seasonId}/exports → Xem exports (read-only)
18. GET /api/shipping/orders/ready-to-ship → Đơn chờ vận chuyển (read-only)
19. GET /api/shipping/orders/completed → Đã hoàn thành (read-only)
20. GET /api/shipping/driver-reports → Báo cáo tài xế (read-only)
21. GET /api/shipping/driver-users → Tài xế chưa có profile (read-only)
22. GET /api/shipping/shipments → Xem vận chuyển (read-only)
23. GET /api/shipping/shipments/{id} → Chi tiết vận chuyển (read-only)
24. GET /api/shipping/vehicles → Xem xe (read-only)
25. GET /api/shipping/drivers → Xem tài xế (read-only)
26. GET /api/notifications → Xem thông báo
27. GET /api/notifications/unread-count → Đếm chưa đọc
28. GET /api/notifications/stream → SSE stream

❌ KHÔNG THỰC HIỆN (MODERATOR không có quyền ghi):
────────────────────────────────────────────────────
- PUT/POST/DELETE trên mọi endpoint quản trị
- approve/reject farms, orders
- Tạo/sửa/xóa sản phẩm, danh mục
- Tạo/sửa/xóa contract
- Tạo/sửa/xóa gói dịch vụ
- Xử lý báo cáo
- ...
```

---

## PHẦN 3: TOÀN BỘ KẾT NỐI ENDPOINT THEO VAI

### 3.1 Tổng hợp endpoints theo nhóm user

| Endpoint | GUEST | FARM_MGR | RETAILER | SHIP_MGR | DRIVER | ADMIN | SUPER_ADMIN | MODERATOR |
|----------|-------|----------|----------|----------|--------|-------|-------------|-----------|
| **Auth** | | | | | | | | |
| POST /api/auth/login | ✗ | ✓ (farm) | ✓ (retailer) | ✓ (shipping) | ✓ (driver) | ✓ (admin) | ✓ | ✓ |
| POST /api/auth/register | ✓ | | ✓ | | | | | |
| GET /api/profile | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Farm** | | | | | | | | |
| POST /api/farms/register | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET /api/farms/my | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET/PUT /api/farms/{id} | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| POST/GET /api/farms/{id}/certifications | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET /api/admin/farms* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓(read) |
| PUT /api/admin/farms/{id}/approve/reject/status/notes | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| **Farming** | | | | | | | | |
| POST/GET/PUT/PATCH /api/farms/{farmId}/seasons** | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| POST/GET/PUT /api/seasons/{seasonId}/processes | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| POST/GET /api/seasons/{seasonId}/exports | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET /api/seasons/{seasonId}/exports (ExportController) | ✗ | ✓ | ✗ | ✗ | ✗ | ✓(read) | ✓(read) | ✓(read) |
| **Product/Marketplace** | | | | | | | | |
| GET /api/public/products | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| GET /api/marketplace/products | ✗ | ✗ | ✓ | ✗ | ✗ | ✓(admin) | ✓ | ✗ |
| GET/POST /api/farms/{farmId}/marketplace/products | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET /api/admin/products* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓(read) |
| PUT /api/admin/products/{id}/status | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| GET/POST/PUT/DELETE /api/admin/products/categories* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| GET /api/categories | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Order** | | | | | | | | |
| GET/PUT /api/orders (FM requests) | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| POST/GET/PUT /api/orders (Retailer) | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET /api/shipping/orders/ready-to-ship/completed | ✗ | ✗ | ✗ | ✓ | ✗ | ✓ | ✓ | ✓ |
| **Shipment** | | | | | | | | |
| GET/POST/PUT/DELETE /api/shipping/* | ✗ | ✗ | ✗ | ✓ | ✗ | ✓ | ✓ | ✗ |
| GET/POST /api/driver/* | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ |
| GET /api/farms/{farmId}/shipments | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET/PUT /api/retailer/shipments | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| **Blockchain** | | | | | | | | |
| GET/PUT/POST /api/admin/contracts* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓(read) |
| GET/PUT /api/blockchain/transactions* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✓ |
| **Payment/Subscription** | | | | | | | | |
| POST /api/subscriptions/purchase | ✗ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET/PUT /api/subscriptions/my* | ✗ | ✓ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET/PUT /api/subscriptions/admin/* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| GET /api/service-packages | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| GET/POST/PUT/DELETE /api/service-packages/admin* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| **Notification** | | | | | | | | |
| GET /api/notifications | ✗(guest) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| POST /api/notifications/broadcast | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| POST /api/admin/announcements | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗ |
| GET /api/notifications/stream | ✗(guest) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Report** | | | | | | | | |
| POST/GET /api/reports* | ✗ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| GET/PUT /api/reports/admin* | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✓ | ✗(read) |
| **IOT** | | | | | | | | |
| POST /api/iot/sensors | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| **Retailer Profile** | | | | | | | | |
| GET/PUT /api/retailer/profile | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET/POST/PUT /api/retailer/business-profile | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| **Trading Floor** | | | | | | | | |
| POST /api/farms/{farmId}/marketplace/products | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET /api/farms/{farmId}/marketplace/products | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| GET /api/categories (TradingFloor) | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| **Notify Farm** | | | | | | | | |
| POST /api/retailer/notify-farm | ✗ | ✗ | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| **Sepay** | | | | | | | | |
| POST /api/public/sepay/webhook | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

* = admin endpoints require X-Actor-Email header
** = Farm Manager own farms only

---

## PHẦN 4: BẢNG DỮ LIỆU SEED (Dữ liệu mẫu ban đầu)

### Users (12 accounts):
- superadmin@bicap.com / Superadmin@2026 → SUPER_ADMIN
- admin@bicap.com / Adminpassword@2026 → ADMIN
- moderator@bicap.com / Moderator@2026 → MODERATOR
- farm@bicap.com / Farmpassword@2026 → FARM_MANAGER (Trang Trại BICAP)
- farm@bicap.vn / Farmpassword@2026 → FARM_MANAGER (HTX Nông Sản Sạch)
- retailer@bicap.com / Retailpassword@2026 → RETAILER (Nhà Bán Lẻ BICAP)
- retail@bicap.com / Retailpassword@2026 → RETAILER (Nhà Bán Lẻ Short)
- shipping_mgr@bicap.com / Shipping@2026 → SHIPPING_MGR
- driver@bicap.com / Driver@2026 → SHIP_DRIVER
- driver2@bicap.com / Driver@2026 → SHIP_DRIVER

### Farms (4):
1. "Trang Trại Xanh Đồng Nai" — FARM_MANAGER PENDING
2. "HTX Nông Sản Sạch Lâm Đồng" — FARM_MANAGER PENDING
3. "Trang Trại Hữu Cơ Sông Hồng" — FARM_MANAGER APPROVED (có subscription)
4. "Vườn Sạch Tiền Giang" — FARM_MANAGER REJECTED

### Categories (7): Rau ăn lá, Củ quả, Trái cây, Lúa gạo, Thủy hải sản, Thịt - Trứng - Sữa, Khác

### Service Packages (3): Gói Cơ Bản (500K), Gói Chuyên Nghiệp (1.5M), Gói Doanh Nghiệp (5M)

### Seeded Orders (BICAP-75 test data): 2 orders for "Cải xanh hữu cơ BICAP" product (DEPOSIT_PAID + IN_TRANSIT)

### Educational Content (4): 2 articles + 2 videos (PUBLISHED)

### Notifications: 2 system announcements (guest-visible)

---

## PHẦN 5: QUAN TRỌNG — APPROVAL POLICY CHANGE

> **"The approval policy changed from 'ask' to 'never' (changed by the user)."**

Điều này ảnh hưởng đến:
1. **`FarmApprovalController`** — khi duyệt/trừ nông trại, nếu policy = "never", hệ thống sẽ tự động duyệt (APPROVE) mà không cần ask confirmation
2. **`SubscriptionController`** — approve/reject subscription requests tự động nếu policy = "never"
3. Bất kỳ flow nào liên quan đến "ask for approval" → sẽ tự động pass

Trong test flow, cần kiểm tra:
- ✅ Farm đăng ký → tự động APPROVED (không cần chờ admin duyệt)
- ✅ Subscription request → tự động APPROVED
- ✅ Order approve → tự động ACCEPTED
- ✅ All admin "approve" endpoints → cần verify behavior thay đổi

---

*Generated: Comprehensive analysis of BICAP Blockchain Agricultural Platform v2.4.2*
*Tables: 32 | Controllers: 36 | Services: 39 | User Roles: 7 | Endpoints: 200+*
