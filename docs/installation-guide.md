# Hướng dẫn Cài đặt Hệ thống (Installation Guide)

| Thông tin | Chi tiết |
|---|---|
| **Dự án** | BICAP |
| **Mã ticket** | BICAP-97 |
| **Phiên bản** | 1.0 — 30/08/2026 |

---

## 1. Yêu cầu hệ thống (prerequisites)

### 1.1 Phần mềm bắt buộc
| Phần mềm | Phiên bản | Kiểm tra |
|---|---|---|
| Java JDK | 21 (Temurin/Corretto) | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Node.js | 22.22.2+ hoặc 24.15+ (đã test 22.23 & 24.21) | `node -v` |
| npm | 10+ | `npm -v` |

### 1.2 Phần mềm tùy chọn (production)
| Phần mềm | Mục đích |
|---|---|
| MySQL | 5.7.41 — database chính |
| Redis | 8.6 — cache phân tán (thiếu thì app tự fallback in-memory) |
| k6 | load test (`dev/loadtest/k6-loadtest.js`) |

### 1.3 Tài nguyên phần cứng khuyến nghị
- Dev: 4 CPU / 8 GB RAM.
- Production: 4 CPU / 8 GB RAM (app) + MySQL & Redis riêng hoặc managed.

## 2. Cài đặt từ mã nguồn (dev)

```bat
git clone https://github.com/ngnsusinn/bicap-blockchain-agricultural-platform.git
cd bicap-blockchain-agricultural-platform
```

### 2.1 Cấu hình biến môi trường
```bat
copy .env.example .env
```
Chế độ dev **không bắt buộc** sửa gì — backend tự chạy H2 in-memory + blockchain mock + cache in-memory. Chỉ cần `JWT_SECRET` và `SEPAY_API_KEY` thật khi deploy (app fail-fast nếu thiếu/yếu).

### 2.2 Chạy backend
```bat
cd backend
mvn spring-boot:run
```
API: `http://localhost:8080/api`, health: `/actuator/health`.
Các lệnh khác (chạy trong `backend/`): `mvn test` (251 test) · `mvn clean package -DskipTests` → `backend/target/*.jar`.

### 2.3 Chạy web (dev hot-reload, tùy chọn)
```bat
cd web
npm install
npm run dev
```
- Portal (Farm / Retailer / Shipping / Guest): `http://localhost:5174/`
- Admin dashboard: `http://localhost:5174/admin`
- Backend CORS chỉ cho phép origin `http://localhost:5174`.

Các lệnh khác trong `web/`: `npm ci` · `npm run lint` · `npm test` (28 test) · `npm run build` → `web/dist` · `npm run preview`.

### 2.4 Chạy gộp MỘT PORT (khuyến nghị demo/test)
```bat
cd web
npm install
npm run build                                 :: build web → web/dist

cd ..
rmdir /s /q backend\src\main\resources\static
mkdir backend\src\main\resources\static
xcopy /E /I /Y web\dist backend\src\main\resources\static

cd backend
mvn spring-boot:run
```
- Portal: `http://localhost:8080/`
- Admin dashboard: `http://localhost:8080/admin`
- Session dùng chung cho portal và admin qua `localStorage` (`accessToken` / `currentUser` / `refreshToken`); **không** truyền token qua URL.

## 3. Cài đặt production

### 3.1 Database
1. Tạo schema MySQL 5.7.41 `bicap_db`.
2. Chạy các script theo thứ tự trong `docs/sql/`:
   `bicap-79-database-setup.md` → `bicap-5-categories.sql` → `bicap-16-17-schema.sql` → `bicap-36-38-schema.sql` → `bicap-18-marketplace-products.sql` → `bicap-76-shipment-schema.sql` → `bicap-27-reports-schema.sql`.
3. Đặt `DDL_AUTO=validate` (mặc định trong `.env.example`).

### 3.2 Biến môi trường bắt buộc
| Biến | Yêu cầu |
|---|---|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | MySQL thật |
| `JWT_SECRET` | base64 ≥32 bytes — `openssl rand -base64 48` |
| `SEPAY_API_KEY` | key thật |
| `SPRING_REDIS_HOST/PORT/PASSWORD/SSL` | Redis 8.6 (bỏ qua nếu chấp nhận in-memory cache) |
| `SMTP_*` | SMTP thật (xác minh email retailer) |
| `BLOCKCHAIN_MODE=live` + `BLOCKCHAIN_PRIVATE_KEY` | ví signer có VTHO; node URL testnet/mainnet |

### 3.3 Đóng gói & chạy production
```bash
# 1. Build web → web/dist
cd web
npm ci
npm run build

# 2. Copy web/dist → backend static (thư mục static/ được gitignore — là build artifact)
cd ..
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r web/dist/. backend/src/main/resources/static/

# 3. Đóng gói backend
cd backend
mvn clean package -DskipTests      # → backend/target/*.jar
```
Khởi động backend với bộ biến môi trường production ở mục 3.2 (`mvn spring-boot:run`, hoặc chạy JAR trong `backend/target/`).

### 3.4 Nâng cấp phiên bản
```bash
git pull

cd web
npm ci
npm run build

cd ..
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -r web/dist/. backend/src/main/resources/static/

cd backend
mvn clean package -DskipTests
# khởi động lại backend
```
Schema mới: chạy thêm file `docs/sql/` tương ứng ticket (seed `DatabaseSeeder` tự bỏ qua bản ghi đã tồn tại — an toàn khi khởi động lại).

## 4. Xác minh cài đặt (smoke test)

| # | Kiểm tra | Lệnh/Thao tác | Kỳ vọng |
|---|---|---|---|
| 1 | Backend sống | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` (hoặc DOWN mail nếu chưa cấu hình SMTP — không chặn luồng chính) |
| 2 | Portal | mở `/` | trang đăng nhập 3 tab |
| 3 | Admin Web | mở `/admin` | trang đăng nhập admin |
| 4 | Đăng nhập test | tài khoản `farm@bicap.com` / `Farmpassword@2026` | vào Farm Dashboard |
| 5 | Đăng nhập admin | mở `/admin` → điền nhanh → đăng nhập | vào thẳng Dashboard Admin (session dùng chung, không token trên URL) |
| 6 | API công khai | `curl http://localhost:8080/api/categories` | JSON danh mục |
| 7 | Blockchain mock | tạo mùa vụ (farm) | `txHash` dạng `0x…` |
| 8 | Test tự động | `cd backend && mvn test` · `cd web && npm test` | 251 + 28 pass |

## 5. Khắc phục sự cố thường gặp
| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| App không boot, lỗi JWT_SECRET | thiếu/yếu secret | sinh secret mới (mục 3.2) |
| 429 khi đăng nhập nhiều lần | RateLimitFilter 30/phút/IP | chờ 1 phút hoặc đổi IP |
| Trang `/admin` 404 | chưa build/copy web vào static | chạy `cd web && npm run build`, copy `web/dist/*` vào `backend/src/main/resources/static/` rồi khởi động lại backend |
| Cache không dùng Redis | Redis không tới được | xem log "Cache provider: IN-MEMORY fallback"; kiểm tra `SPRING_REDIS_*` |
| Live mode: "blockchain.private-key is required" | `BLOCKCHAIN_MODE=live` thiếu key | đặt `BLOCKCHAIN_PRIVATE_KEY` (hex, không 0x cũng được) |
| Tx kẹt PENDING | node chậm/hết gas | job tự retry 3 lần; kiểm tra số dư VTHO của ví signer |
| Ảnh upload không hiển thị | sai `UPLOAD_DIR` | kiểm tra thư mục uploads + `/uploads/**` |

## 6. Gỡ cài đặt
Dừng tiến trình backend và web dev, xóa `uploads/` và schema DB nếu muốn dọn sạch. Không có registry/dịch vụ nền nào cài lên máy.
