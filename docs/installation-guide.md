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

### 1.2 Hạ tầng bắt buộc (dev và production dùng chung một cấu hình)
| Phần mềm | Mục đích |
|---|---|
| MySQL | 5.7.41 — database chính (server **remote**; backend không còn H2 mặc định) |
| Redis | 8.6 — cache phân tán (**bắt buộc**: không kết nối được là backend dừng khởi động, không còn fallback in-memory) |
| Node VeChainThor + ví signer có VTHO | blockchain `live` cho mọi lần ghi dữ liệu lên chain |
| k6 | load test (`dev/loadtest/k6-loadtest.js`) |

### 1.3 Tài nguyên phần cứng khuyến nghị
- Dev: 4 CPU / 8 GB RAM (cộng với MySQL + Redis remote).
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
Chế độ dev giờ dùng **đúng cấu hình production — không còn fallback**: backend bắt buộc MySQL **remote** + Redis **remote** + blockchain **live** (`BLOCKCHAIN_MODE=live` + `BLOCKCHAIN_PRIVATE_KEY` cho ví signer có VTHO). `JWT_SECRET` và `SEPAY_API_KEY` bắt buộc ở **mọi profile** (không còn secret sinh tạm), và giá trị template/đã từng public trong repo bị từ chối. Thiếu hạ tầng/key ⇒ **app dừng khởi động** kèm thông báo rõ ràng.

Backend **tự nạp** `.env` qua `spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]`, nên `mvn spring-boot:run` trong `backend/` hoặc từ thư mục gốc đều đọc được. Chạy nhanh bằng script `dev/run-backend.ps1` (nạp `.env` rồi gọi `mvn spring-boot:run`).

> Cờ `ALLOW_SIMULATION=true` **chỉ dành cho test/CI**: nó cho phép H2 + mock blockchain + local stub. Không bật cho môi trường thật.

### 2.2 Chạy backend
```bat
cd backend
mvn spring-boot:run
```
Hoặc từ thư mục gốc: `.\dev\run-backend.ps1` (nạp `.env` trước khi chạy).
API: `http://localhost:8080/api`, health: `/actuator/health`.
Các lệnh khác (chạy trong `backend/`): `mvn test` (345 test) · `mvn clean package -DskipTests` → `backend/target/*.jar`.
Bộ test chạy **không cần** MySQL/Redis: `backend/src/test/resources/application.properties` tự khai báo H2 `create-drop`, `app.cache.enabled=false` và `app.allow-simulation=true`.

### 2.3 Chạy web (dev hot-reload, tùy chọn)
```bat
cd web
npm install
npm run dev
```
- Portal (Farm / Retailer / Shipping / Guest): `http://localhost:5174/`
- Admin dashboard: `http://localhost:5174/admin`
- Backend CORS chỉ cho phép origin `http://localhost:5174`.

Các lệnh khác trong `web/`: `npm ci` · `npm run lint` · `npm test` (84 test / 22 file) · `npm run build` → `web/dist` · `npm run preview`.

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
3. Đặt `DDL_AUTO=validate` khi chạy production thật (`.env.example` đang để `update` cho tiện demo — nhớ đổi lại trước khi lên production).

### 3.2 Biến môi trường bắt buộc
Mọi biến dưới đây đều **bắt buộc** (dùng chung cho dev và production); thiếu key hoặc hạ tầng không tới được ⇒ backend **dừng khởi động** kèm thông báo lỗi cụ thể.

| Biến | Yêu cầu |
|---|---|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | MySQL **remote** thật (không còn default H2; URL rỗng hoặc `jdbc:h2:` đều bị chặn) |
| `JWT_SECRET` | base64 ≥32 bytes — `openssl rand -base64 48`; bắt buộc ở **mọi profile**, giá trị template/đã từng public bị từ chối |
| `SEPAY_API_KEY` | key thật — `openssl rand -hex 32`; bắt buộc ở **mọi profile**, placeholder bị từ chối |
| `SPRING_REDIS_HOST/PORT/PASSWORD/SSL` | Redis 8.6 **remote**; không kết nối được là dừng khởi động — chỉ tắt cache khi chủ động đặt `APP_CACHE_ENABLED=false` (test/CI) |
| `BLOCKCHAIN_MODE=live` + `BLOCKCHAIN_PRIVATE_KEY` | ví signer có VTHO; node URL testnet/mainnet. `BLOCKCHAIN_MODE=mock` / `BLOCKCHAIN_EXPORT_MODE=local` chỉ chấp nhận khi `ALLOW_SIMULATION=true` (test/CI) |

> Không còn cấu hình `SMTP_*`: toàn bộ email/SMTP đã bị xoá khỏi backend (không còn `spring-boot-starter-mail`, không còn email xác thực Nhà bán lẻ/email cảnh báo). Đăng ký Retailer được **kích hoạt ngay**; thông báo chỉ có in-app + realtime SSE.

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
| 1 | Backend sống | `curl http://localhost:8080/actuator/health` | `{"status":"UP"}` (MySQL + Redis + blockchain live đã cấu hình đúng; không còn health check mail/SMTP) |
| 2 | Portal | mở `/` | trang đăng nhập 3 tab |
| 3 | Admin Web | mở `/admin` | trang đăng nhập admin |
| 4 | Đăng nhập test | tài khoản `farm@bicap.com` / `Farmpassword@2026` | vào Farm Dashboard |
| 5 | Đăng nhập admin | mở `/admin` → điền nhanh → đăng nhập | vào thẳng Dashboard Admin (session dùng chung, không token trên URL) |
| 6 | API công khai | `curl http://localhost:8080/api/categories` | JSON danh mục |
| 7 | Blockchain live | tạo mùa vụ (farm) | `txHash` dạng `0x…` đã ký và broadcast thật lên VeChainThor (ví signer cần VTHO) |
| 8 | Test tự động | `cd backend && mvn test` · `cd web && npm test` | 345 + 84 pass (test backend tự dùng H2 `create-drop`, không cần MySQL/Redis) |

## 5. Khắc phục sự cố thường gặp
| Triệu chứng | Nguyên nhân | Cách xử lý |
|---|---|---|
| App không boot, lỗi JWT_SECRET | thiếu/yếu secret hoặc còn là giá trị template | sinh secret mới `openssl rand -base64 48` rồi đặt `JWT_SECRET` (mục 3.2) — **không** còn secret sinh tạm tự động |
| App không boot, lỗi SEPAY_API_KEY | thiếu key hoặc còn placeholder | dán key thật từ dashboard Sepay vào `.env` |
| App không boot: "SPRING_DATASOURCE_URL is not configured" / "đang trỏ tới H2 in-memory" | chưa cấu hình MySQL remote | đặt `SPRING_DATASOURCE_URL` MySQL remote (H2 chỉ dùng cho test/CI với `ALLOW_SIMULATION=true`) |
| App không boot: "Không kết nối được Redis tại host:port" | Redis remote không tới được | kiểm tra `SPRING_REDIS_*`, firewall/TLS (`SPRING_REDIS_SSL`) — **không còn** fallback cache in-memory; chỉ tắt cache bằng `APP_CACHE_ENABLED=false` khi test/CI |
| App không boot: "BLOCKCHAIN_MODE=... là chế độ giả lập" | đang để `mock`/`local` ngoài test/CI | đặt `BLOCKCHAIN_MODE=live` + `BLOCKCHAIN_EXPORT_MODE=vechain`, hoặc `ALLOW_SIMULATION=true` nếu thật sự là test/CI |
| 429 khi đăng nhập nhiều lần | RateLimitFilter 30/phút/IP | chờ 1 phút hoặc đổi IP |
| Trang `/admin` 404 | chưa build/copy web vào static | chạy `cd web && npm run build`, copy `web/dist/*` vào `backend/src/main/resources/static/` rồi khởi động lại backend |
| Live mode: "blockchain.private-key is required" | `BLOCKCHAIN_MODE=live` thiếu key | đặt `BLOCKCHAIN_PRIVATE_KEY` (hex, không 0x cũng được) |
| Tx kẹt PENDING | node chậm/hết gas | job tự retry 3 lần; kiểm tra số dư VTHO của ví signer |
| Ảnh upload không hiển thị | sai `UPLOAD_DIR` | kiểm tra thư mục uploads + `/uploads/**` |

## 6. Gỡ cài đặt
Dừng tiến trình backend và web dev, xóa `uploads/` và schema DB nếu muốn dọn sạch. Không có registry/dịch vụ nền nào cài lên máy.
