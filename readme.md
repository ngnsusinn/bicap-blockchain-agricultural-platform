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

Backend chạy với **cấu hình kiểu production ngay từ đầu**:

- MySQL + Redis là **dịch vụ bắt buộc** (server remote) — Redis không kết nối được là **dừng khởi động**;
- `JWT_SECRET` / `SEPAY_API_KEY` phải là **key thật**; thiếu hoặc còn placeholder là **dừng khởi động**;
- blockchain ở chế độ **`live`** (mock chỉ dành cho test/CI).

**Không còn secret sinh tạm hay fallback ngầm** (H2 in-memory, cache in-memory, hash giả lập) —
cấu hình thiếu sẽ báo lỗi rõ ràng thay vì chạy ở chế độ giả lập.

```powershell
# 1) Tạo .env từ template rồi điền key thật (file .env đã được .gitignore)
Copy-Item .env.example .env

# 2) Chạy backend kèm .env — script export toàn bộ biến rồi gọi mvn
.\dev\run-backend.ps1
# → API tại http://localhost:8080
```

```bash
# Hoặc trên bash/macOS:
set -a; source .env; set +a
cd backend && mvn spring-boot:run
```

Chuẩn bị hạ tầng trước khi chạy: database MySQL trên server remote, Redis trên server remote,
và ví signer VeChainThor đã nạp VTHO (nếu dùng tính năng xuất kho/QR). Dữ liệu mẫu được seed tự
động bởi `DatabaseSeeder` ở lần chạy đầu.

Chạy test backend (test dùng H2 + tắt cache + blockchain giả lập qua `src/test/resources/application.properties`,
nên **không cần** MySQL/Redis):

```bash
cd backend
mvn test
```

Đóng gói JAR:

```bash
cd backend
mvn clean package -DskipTests     # → backend/target/*.jar
# chạy kèm biến môi trường từ .env (ví dụ trên bash):
set -a; source ../.env; set +a; java -jar target/bicap-blockchain-agricultural-platform-0.0.1-SNAPSHOT.jar
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
| `SPRING_DATASOURCE_URL` | **bắt buộc** (MySQL remote) | JDBC URL của server MySQL, ví dụ `jdbc:mysql://host:3306/bicap_db?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | **bắt buộc** | Tài khoản DB trên server remote |
| `DDL_AUTO` | `update` | Production thật nên dùng `validate` + migration (`docs/sql/`) |
| `JWT_SECRET` | **bắt buộc** | `openssl rand -base64 48` (≥ 32 byte). Thiếu/placeholder/đã từng public ⇒ **dừng khởi động** |
| `FRONTEND_URL` | `http://localhost:5174` | Dùng để sinh link QR trace |
| `UPLOAD_DIR` | `uploads` | Thư mục lưu file tải lên |
| `SEPAY_API_KEY` | **bắt buộc** | Khoá webhook Sepay (dán key thật từ dashboard Sepay) — placeholder ⇒ **dừng khởi động** |
| `SPRING_REDIS_HOST` / `_PORT` / `_PASSWORD` / `_SSL` | **bắt buộc** (Redis remote) | Cache BICAP-79. Không kết nối được ⇒ **dừng khởi động** (không còn fallback in-memory) |
| `APP_CACHE_ENABLED` | `true` | Chỉ đặt `false` khi chủ động tắt cache (test/CI) |
| `BLOCKCHAIN_MODE` | `live` | Production **phải** là `live` (ký & broadcast thật) |
| `ALLOW_SIMULATION` | `false` | Chỉ test/CI mới đặt `true` để cho phép chế độ giả lập |
| `BLOCKCHAIN_EXPORT_MODE` | `vechain` | `vechain` (neo lô xuất kho thật); `local` (stub SHA-256) bị chặn như `ALLOW_SIMULATION=false` |
| `BLOCKCHAIN_PRIVATE_KEY` | **bắt buộc khi `live`** | Private key 32 byte hex của ví signer (sinh bằng `dev/tools/WalletGen.java`), ví cần VTHO để trả gas |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Build-time của `web/` — origin backend |

### 🔐 Cấu hình kiểu production & bảo mật (quan trọng)

- Backend **không ship secret và không tự sinh secret**. `JWT_SECRET`/`SEPAY_API_KEY` để trống hoặc còn
  dạng template (`replace-with-…`, `your_…`) ⇒ `SecureSecretInitializer` **dừng khởi động ở mọi profile**,
  kể cả dev — không còn "ephemeral development secret" khiến token chết sau mỗi lần restart.
- `SecretConfigValidator` chặn thêm: `JWT_SECRET` giải mã < 32 byte; `BLOCKCHAIN_MODE` khác `live`
  (trừ khi đặt `ALLOW_SIMULATION=true`); `BLOCKCHAIN_EXPORT_MODE=local`; thiếu hoặc sai định dạng
  `BLOCKCHAIN_PRIVATE_KEY` khi chạy `live`.
- **Redis là dependency bắt buộc**: không kết nối được ⇒ dừng khởi động (trước đây tự fallback cache
  in-memory). Chỉ tắt cache khi chủ động đặt `APP_CACHE_ENABLED=false` (test/CI).
- Bất kỳ giá trị nào từng được công bố trong repo (`test-jwt-secret-key-*`, `test-sepay-api-key`) đều bị
  **từ chối khởi động**.
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

- `BLOCKCHAIN_MODE=live` (**cấu hình production**) — giao dịch legacy type-0 được RLP-encode, ký secp256k1
  (RFC 6979, low-s) và broadcast lên node; `BlockchainMaintenanceJob` xác nhận receipt mỗi 15s và tự
  retry tối đa 3 lần. Export trả `chainMode = "LIVE"`.
- `BLOCKCHAIN_MODE=mock` — hash mô phỏng tại chỗ, **không** dùng cho production: `SecretConfigValidator`
  sẽ chặn khởi động, chỉ cho phép khi đặt `ALLOW_SIMULATION=true` (dành cho test/CI — xem
  `src/test/resources/application.properties`).
- Lô xuất kho + mã QR được neo qua `VeChainExportBlockchainGateway`;
  `BLOCKCHAIN_EXPORT_MODE=local` (stub SHA-256) cũng bị chặn như chế độ mock.
- Hợp đồng mẫu: `dev/blockchain/contracts/Traceability.sol`.
- Trang truy xuất công khai: `/trace/{hash}`.

### Ví signer & broadcast thật (testnet)

```bash
# 1. Sinh ví signer (ghi BLOCKCHAIN_PRIVATE_KEY vào ./.env, chỉ in ra địa chỉ)
BC=$(find ~/.m2/repository/org/bouncycastle -name 'bcprov-jdk18on-*.jar' | head -1)
java -cp "backend/target/classes:$BC" dev/tools/WalletGen.java

# 2. Nạp VTHO (gas) cho địa chỉ vừa in tại https://faucet.vecha.in
#    Kiểm tra:  curl -s https://testnet.vechain.org/accounts/<diaChi>

# 3. Chạy backend kèm .env (đã có BLOCKCHAIN_MODE=live + BLOCKCHAIN_PRIVATE_KEY):
.\dev\run-backend.ps1        # PowerShell
# hoặc: set -a; source .env; set +a; cd backend && mvn spring-boot:run
```

> Chưa nạp VTHO thì backend **vẫn khởi động** (chỉ kiểm tra định dạng key), nhưng thao tác xuất kho/neo
> giao dịch sẽ bị node từ chối do thiếu gas — xem log `BlockchainMaintenanceJob`.

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
| `docs/system-specification.md` | **Đặc tả chi tiết hệ thống theo mã nguồn hiện hành (As-Built)** — kiến trúc, mô hình dữ liệu, API, bảo mật, luồng nghiệp vụ, blockchain, cấu hình, kiểm thử |
| `docs/architecture-design.md`, `docs/detail-design.md` | Thiết kế kiến trúc & chi tiết |
| `docs/bicap-79-database-setup.md`, `docs/sql/` | Thiết lập CSDL & script schema |
| `docs/run-guide.md`, `docs/installation-guide.md` | Hướng dẫn chạy/cài đặt |
| `docs/testing-document.md`, `docs/uat-plan.md` | Tài liệu kiểm thử & UAT |
| `docs/user-manual.md` | Hướng dẫn người dùng cuối |
| `docs/BAO-CAO-TONG-HOP.md` | Báo cáo tổng kết dự án |

> Các tài liệu trong `docs/` đã được cập nhật theo cấu trúc mới (`backend/`, `web/`, `dev/`). Riêng các yêu cầu gốc về **Mobile App tài xế** và **Docker** vẫn được giữ trong tài liệu đặc tả kèm ghi chú rằng phần triển khai tương ứng đã được gỡ khỏi repository.
