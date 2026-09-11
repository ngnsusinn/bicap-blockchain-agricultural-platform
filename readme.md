<div align="center">

# 🌾 BICAP — Blockchain Agricultural Platform

**Tích hợp Blockchain trong sản xuất nông sản sạch**

Nền tảng truy xuất nguồn gốc nông sản trên blockchain VeChainThor, kết nối **Nông trại — Nhà bán lẻ — Vận chuyển — Người tiêu dùng**.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)
![VeChain](https://img.shields.io/badge/VeChainThor-testnet-15BDFF)

</div>

---

## 📑 Mục lục

- [Giới thiệu](#-giới-thiệu)
- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Cấu trúc repository](#-cấu-trúc-repository)
- [Yêu cầu môi trường](#-yêu-cầu-môi-trường)
- [Bắt đầu nhanh](#-bắt-đầu-nhanh)
- [Cấu hình & biến môi trường](#-cấu-hình--biến-môi-trường)
- [Kiểm thử & CI](#-kiểm-thử--ci)
- [Tích hợp Blockchain (VeChainThor)](#-tích-hợp-blockchain-vechainthor)
- [Tài khoản test](#-tài-khoản-test)
- [Tài liệu dự án](#-tài-liệu-dự-án)

---

## ✨ Giới thiệu

BICAP là hệ thống quản lý chuỗi cung ứng nông sản sạch, ghi nhận dữ liệu canh tác, thu hoạch, vận chuyển và tiêu thụ lên blockchain công khai **VeChainThor** để đảm bảo tính minh bạch và truy xuất nguồn gốc.

Toàn bộ giao diện người dùng nằm trong **một ứng dụng web duy nhất** (`web/`), chia khu vực theo endpoint:

| Endpoint | Khu vực | Vai trò |
| --- | --- | --- |
| `/` | Farm / Retailer / Shipping / Guest portal | `FARM_MANAGER`, `RETAILER`, `SHIPPING_MGR`, khách |
| `/trace/{hash}` | Trang truy xuất công khai | Công khai |
| `/admin` | Bảng điều khiển quản trị | `SUPER_ADMIN`, `ADMIN`, `MODERATOR` |

---

## 🎯 Tính năng chính

- **Quản lý nông trại** — hồ sơ nông trại, chứng nhận, mùa vụ, nhật ký canh tác, giám sát IoT (nhiệt độ/độ ẩm/pH).
- **Xuất kho & QR truy xuất** — sinh mã QR gắn với giao dịch blockchain.
- **Sàn giao dịch** — nông trại đăng bán, nhà bán lẻ đặt mua, đặt cọc, hợp đồng thông minh.
- **Vận chuyển** — tạo lô hàng, phân công tài xế/phương tiện, cập nhật hành trình, tracking realtime.
- **Quản trị hệ thống** — phê duyệt nông trại, quản lý tài khoản admin & phân quyền RBAC, giám sát sản phẩm, smart contract, báo cáo người dùng.
- **Thông báo realtime** cho mọi vai trò; nội dung giáo dục và tìm kiếm sản phẩm cho khách.

---

## 🏗️ Kiến trúc hệ thống

```
┌──────────────────────────────────────────────────────────────┐
│  web/  — MỘT ứng dụng React + TypeScript + Vite              │
│                                                              │
│   "/"        → Portal (Farm · Retailer · Shipping · Guest)   │
│   "/admin"   → Admin dashboard                               │
│                                                              │
│   Dùng chung: session (localStorage), design system, build   │
└───────────────────────────┬──────────────────────────────────┘
                            │ REST /api/**  (JWT Bearer)
┌───────────────────────────▼──────────────────────────────────┐
│  backend/  — Spring Boot 3.3 (Java 21)                       │
│   Controller → Service → Repository (JPA)                    │
│   Security: JWT + RBAC · Rate limit · Redis cache            │
└───────┬───────────────────────────────┬──────────────────────┘
        │                               │
   MySQL / H2 (JPA)              VeChainThor testnet
                                 (mock ở môi trường dev/CI)
```

- Backend Spring Boot phục vụ cả API (`/api/**`) **và** static shell của app web khi đã build (single-port, mặc định `:8080`).
- Khi dev, app web chạy riêng ở Vite `:5174` và gọi API qua CORS tới `:8080`.
- Session đăng nhập dùng chung cho portal và admin (một `accessToken` + `currentUser` trong `localStorage`).

---

## 📁 Cấu trúc repository

```
bicap-blockchain-agricultural-platform/
├── backend/                     # Spring Boot API (Maven)
│   ├── pom.xml
│   └── src/
│       ├── main/java/vn/courses/ut/edu/javaprogramming/bicap/
│       │   ├── controller/  service/  repository/  entity/  dto/
│       │   ├── config/      common/   exception/
│       ├── main/resources/application.properties
│       └── test/            # Unit + integration tests
│
├── web/                         # Ứng dụng web gộp chung (portal + admin)
│   ├── index.html  vite.config.ts  package.json  .oxlintrc.json
│   ├── public/
│   └── src/
│       ├── App.tsx              # Router theo endpoint: /admin vs phần còn lại
│       ├── main.tsx  index.css  # Design system dùng chung
│       ├── shared/session.ts    # Session + API base dùng chung
│       ├── portal/              # Cổng Farm / Retailer / Shipping / Guest
│       │   ├── PortalApp.tsx  components/  pages/  utils/
│       └── admin/               # Bảng điều khiển /admin
│           ├── AdminApp.tsx  components/  utils/  types.ts
│
├── dev/                         # Công cụ phát triển (không thuộc build)
│   ├── blockchain/contracts/    # Traceability.sol
│   ├── loadtest/                # k6 + Node load test
│   └── tools/                   # Tiện ích Java sinh ví/khoá, probe giao dịch
│
├── docs/                        # Tài liệu thiết kế, SQL, hướng dẫn
├── .github/workflows/ci.yml     # CI: web (lint/test/build) + backend (test/package)
├── .env.example                 # Mẫu biến môi trường
└── readme.md
```

---

## 🧰 Yêu cầu môi trường

| Thành phần | Phiên bản | Ghi chú |
| --- | --- | --- |
| JDK | 21+ | Bắt buộc cho backend |
| Maven | 3.9+ | Dự án không kèm Maven Wrapper |
| Node.js | **22.22.2+** (hoặc 24.15+) | Cho `web/` — jsdom 30/undici 8 không hỗ trợ Node 20 |
| npm | 10+ | |
| MySQL | 8.x | Tùy chọn — mặc định backend dùng H2 in-memory |
| Redis | 7+ | Tùy chọn — tự fallback cache in-memory |

---

## 🚀 Bắt đầu nhanh

### 1. Backend (`backend/`)

```bash
cd backend

# (Tùy chọn) nạp biến môi trường từ file .env ở thư mục gốc
#   JWT_SECRET, SEPAY_API_KEY … — nếu không đặt, backend dùng giá trị mặc định dev.

mvn spring-boot:run
# → API tại http://localhost:8080
```

Mặc định backend chạy với **H2 in-memory** (`jdbc:h2:mem:bicap_db`) nên có thể khởi động ngay mà không cần MySQL. Dữ liệu mẫu được seed tự động bởi `DatabaseSeeder`.

Chạy test backend:

```bash
cd backend
mvn test
```

Đóng gói JAR:

```bash
cd backend
mvn clean package -DskipTests     # → backend/target/*.jar
java -jar target/bicap-blockchain-agricultural-platform-0.0.1-SNAPSHOT.jar
```

### 2. Web app — chế độ dev (`web/`)

```bash
cd web
npm install

# Portal:  http://localhost:5174/
# Admin:   http://localhost:5174/admin
npm run dev
```

Trong chế độ dev, app gọi API tại `http://localhost:8080` (CORS đã cấu hình cho `http://localhost:5174`). Nếu backend chạy ở địa chỉ khác, tạo `web/.env` với `VITE_API_BASE_URL=http://<host>:<port>`.

Các lệnh khác:

```bash
npm run lint        # oxlint
npm test            # vitest run
npm run test:watch  # vitest watch
npm run build       # tsc -b && vite build → web/dist
npm run preview     # xem thử bản build
```

### 3. Chạy tất cả trên một port (`:8080`)

Build app web rồi copy vào static resources của backend, sau đó chạy backend:

```bash
# Bước 1 — build web
cd web
npm install
npm run build

# Bước 2 — copy bundle vào backend (rm -rf trước để tránh file cũ)
cd ..
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r web/dist/. backend/src/main/resources/static/

# Bước 3 — chạy backend
cd backend
mvn spring-boot:run
# Portal:  http://localhost:8080/
# Admin:   http://localhost:8080/admin
```

> `backend/src/main/resources/static/` được `.gitignore` — đây là sản phẩm build, không commit.
> Trên Windows dùng `rmdir /s /q` + `xcopy /E /I` tương đương.

---

## ⚙️ Cấu hình & biến môi trường

Sao chép `.env.example` thành `.env` và điền giá trị. Các biến quan trọng:

| Biến | Mặc định | Mô tả |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Cổng backend |
| `SPRING_DATASOURCE_URL` | H2 in-memory | JDBC URL (MySQL ở production) |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `sa` / rỗng | Thông tin DB |
| `DDL_AUTO` | `update` (dev) | Production nên dùng `validate` + migration |
| `JWT_SECRET` | rỗng → random mỗi lần chạy (dev) | **Bắt buộc ở production** — `openssl rand -base64 48`. Giá trị template/đã từng public trong repo sẽ bị **từ chối khởi động** (xem mục Bảo mật) |
| `FRONTEND_URL` | `http://localhost:5174` | Dùng để sinh link xác thực email & QR trace |
| `UPLOAD_DIR` | `uploads` | Thư mục lưu file tải lên |
| `SEPAY_API_KEY` | rỗng → random mỗi lần chạy (dev) | Khoá cổng thanh toán Sepay; giá trị template bị từ chối khởi động |
| `SPRING_REDIS_*` | localhost | Redis cache (tự fallback in-memory nếu không kết nối được) |
| `BLOCKCHAIN_MODE` | `mock` | `mock` (dev/CI) hoặc `live` (ký & broadcast thật) |
| `BLOCKCHAIN_EXPORT_MODE` | `vechain` | `vechain` (neo lô xuất kho qua `BlockchainService`) hoặc `local` (stub SHA-256, chỉ dùng khi dev) |
| `BLOCKCHAIN_PRIVATE_KEY` | rỗng | Khoá riêng ví ký giao dịch khi `live` |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Build-time của `web/` — origin backend |

### 🔐 Bảo mật triển khai (quan trọng)

- Backend **không còn ship secret mặc định**. Khi `JWT_SECRET`/`SEPAY_API_KEY` để trống, `SecureSecretInitializer`
  sinh secret ngẫu nhiên cho **riêng tiến trình đang chạy** (token sẽ hết hiệu lực sau khi restart).
- Ở profile `prod`/`production`, để trống các biến này là **lỗi khởi động**.
- Bất kỳ giá trị nào từng được công bố trong repo (`test-jwt-secret-key-*`, `test-sepay-api-key`) hoặc
  còn nguyên dạng template (`replace-with-…`, `your_…`) đều bị **từ chối khởi động**.
- `/api/admin/**` (bao gồm danh sách sản phẩm) yêu cầu xác thực — khách vãng lai dùng
  `/api/public/products` và `/api/public/education`.
- Guest chỉ nhận được **thông báo hệ thống** (`is_system = true`) qua `GET /api/notifications`.

---

## 🧪 Kiểm thử & CI

CI (`.github/workflows/ci.yml`) gồm 2 job:

| Job | Nội dung |
| --- | --- |
| `web-ci` | Node 22 · `npm ci` → `npm run lint` → `npm test` → `npm run build` (thư mục `web/`) |
| `backend-ci` | JDK 21 · `mvn clean test` → `mvn package -DskipTests`; upload JAR (thư mục `backend/`) |

Chạy cục bộ:

```bash
cd web && npm test
cd backend && mvn test
```

---

## ⛓️ Tích hợp Blockchain (VeChainThor)

- `BLOCKCHAIN_MODE=mock` (mặc định) — hash được mô phỏng, không cần mạng; dùng cho dev và CI.
- `BLOCKCHAIN_MODE=live` — giao dịch legacy type-0 được RLP-encode, ký secp256k1 (RFC 6979,
  low-s) và broadcast lên node; `BlockchainMaintenanceJob` xác nhận receipt mỗi 15s và tự
  retry tối đa 3 lần. Export trả `chainMode = "LIVE"` (mock trả `"MOCK"`).
- Lô xuất kho + mã QR được neo qua `VeChainExportBlockchainGateway` (mặc định);
  `BLOCKCHAIN_EXPORT_MODE=local` chỉ là stub SHA-256 dùng khi dev.
- Hợp đồng mẫu: `dev/blockchain/contracts/Traceability.sol`.
- Trang truy xuất công khai: `/trace/{hash}`.

### Bật broadcast thật (testnet)

```bash
# 1. Sinh ví signer (ghi BLOCKCHAIN_PRIVATE_KEY vào ./.env, chỉ in ra địa chỉ)
BC=$(find ~/.m2/repository/org/bouncycastle -name 'bcprov-jdk18on-*.jar' | head -1)
java -cp "backend/target/classes:$BC" dev/tools/WalletGen.java

# 2. Nạp VTHO (gas) cho địa chỉ vừa in tại https://faucet.vecha.in
#    Kiểm tra:  curl -s https://testnet.vechain.org/accounts/<diaChi>

# 3. Bật live trong .env  (BLOCKCHAIN_MODE=live, BLOCKCHAIN_NODE_URL=https://testnet.vechain.org)
#    rồi chạy — LƯU Ý: app KHÔNG tự đọc .env, phải export:
cd backend && set -a && source ../.env && set +a && mvn spring-boot:run
```

Tra giao dịch: `https://explore-testnet.vechain.org/transactions/<txHash>`.
Trạng thái on-chain trong app: `GET /api/admin/contracts/blockchain-status` và
`GET /api/blockchain/transactions` (admin, kèm header `X-Actor-Email`).

---

## 🔑 Tài khoản test

Được seed sẵn bởi `DatabaseSeeder` / `DatabaseSeederTest`:

| Vai trò | Email | Mật khẩu | Đăng nhập tại |
| --- | --- | --- | --- |
| Super Admin | `superadmin@bicap.com` | `Superadmin@2026` | `/admin` |
| Admin | `admin@bicap.com` | `Adminpassword@2026` | `/admin` |
| Moderator | `moderator@bicap.com` | `Moderator@2026` | `/admin` |
| Farm Manager | `farm@bicap.com` | `Farmpassword@2026` | `/` |
| Farm Manager | `farm@bicap.vn` | `Farmpassword@2026` | `/` |
| Retailer | `retailer@bicap.com` | `Retailpassword@2026` | `/` |
| Shipping Manager | `shipping_mgr@bicap.com` | `Shipping@2026` | `/` |

---

## 📚 Tài liệu dự án

| Tài liệu | Nội dung |
| --- | --- |
| `docs/requirement.md`, `docs/user-requirements.md` | Yêu cầu nghiệp vụ |
| `docs/software-requirement-specifications.md` | Đặc tả yêu cầu phần mềm |
| `docs/architecture-design.md`, `docs/detail-design.md` | Thiết kế kiến trúc & chi tiết |
| `docs/bicap-79-database-setup.md`, `docs/sql/` | Thiết lập CSDL & script schema |
| `docs/run-guide.md`, `docs/installation-guide.md` | Hướng dẫn chạy/cài đặt |
| `docs/testing-document.md`, `docs/uat-plan.md` | Tài liệu kiểm thử & UAT |
| `docs/user-manual.md` | Hướng dẫn người dùng cuối |
| `docs/BAO-CAO-TONG-HOP.md` | Báo cáo tổng kết dự án |

> Các tài liệu trong `docs/` đã được cập nhật theo cấu trúc mới (`backend/`, `web/`, `dev/`). Riêng các yêu cầu gốc về **Mobile App tài xế** và **Docker** vẫn được giữ trong tài liệu đặc tả kèm ghi chú rằng phần triển khai tương ứng đã được gỡ khỏi repository.
