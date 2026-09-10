# BICAP - Running and Deployment Guide

This document contains step-by-step instructions to run the Blockchain Agricultural Platform (BICAP) backend API and the unified React web app (Farm/Retailer/Shipping/Guest portal **and** admin dashboard).

---

## 1. Prerequisites

Ensure you have the following installed on your machine:
*   **Java JDK 21**
*   **Node.js v22.22.2+ (or v24.15.0+)** & **npm** — jsdom 30/undici 8 no longer support Node 20
*   **IntelliJ IDEA** (Optional, recommended for development)

---

## 2. Configuration (`.env`)

A template `.env.example` is provided at the root of the project. Copy it to `.env` and fill in the values you need:

```bat
copy .env.example .env
```

For local development you do **not** have to edit anything: the backend defaults to an H2 in-memory database, blockchain `mock` mode and an in-memory cache. Real `JWT_SECRET` and `SEPAY_API_KEY` values are required only when deploying (the app fails fast on missing/weak defaults).

*   **Database Configs** (`SPRING_DATASOURCE_*`): H2 in-memory by default; set them to your MySQL host to use a shared/cloud database.
*   **Redis Caching** (`SPRING_REDIS_*`): connects to the Redis host when provided, otherwise the app automatically falls back to an in-memory cache.
*   **Web API URL** (`VITE_API_BASE_URL`, a build-time variable of `web/`): points the web app to the backend, `http://localhost:8080`.

---

## 3. Running the Backend API (Spring Boot)

The Maven project lives in the `backend/` directory.

### Option A: Running in IntelliJ IDEA (Recommended)
1. Open the repository (or the `backend/` folder) in IntelliJ IDEA.
2. IntelliJ will detect the Maven configuration from `backend/pom.xml` automatically.
3. Open `backend/src/main/java/vn/courses/ut/edu/javaprogramming/bicap/Application.java` and click the green **Run** button.
4. To pass the environment variables from the `.env` file, install the **EnvFile** plugin in IntelliJ or set them in the Run Configuration settings.

The backend starts on **port 8080** with H2 in-memory by default.

### Option B: Running via Terminal (PowerShell)
If Maven (`mvn`) is not registered in your global system `PATH`, you can use the Maven executable bundled with your IntelliJ installation:

1. Open PowerShell and change to the backend directory:
   ```powershell
   cd backend
   ```
2. Export the database environment variables to your session (only needed when connecting to an external MySQL/Redis instead of the H2 default). Lấy giá trị thật từ file `.env` — **không ghi thông tin đăng nhập thật vào tài liệu**:
   ```powershell
   $env:SPRING_DATASOURCE_URL="jdbc:mysql://<mysql-host>:3306/<database>?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   $env:SPRING_DATASOURCE_USERNAME="<db-user>"
   $env:SPRING_DATASOURCE_PASSWORD="<db-password>"
   $env:SPRING_REDIS_HOST="<redis-host>"
   $env:SPRING_REDIS_PORT="<redis-port>"
   $env:SPRING_REDIS_PASSWORD="<redis-password>"
   $env:SPRING_REDIS_SSL="true"
   ```
3. Run the Spring Boot application from the `backend/` directory:
   ```powershell
   mvn spring-boot:run
   ```
   If `mvn` is not on `PATH`, use the IntelliJ Maven bundle path instead:
   ```powershell
   & "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" spring-boot:run
   ```

Other backend commands (run from `backend/`):

```bash
mvn test                        # 251 tests
mvn clean package -DskipTests   # → backend/target/*.jar
```

---

## 4. Single-Port Mode — Spring Boot phục vụ app web (Khuyên dùng cho demo/test)

Toàn bộ hệ thống chạy trên **một port duy nhất 8080**:

| URL | Nội dung |
|---|---|
| `http://localhost:8080/` | Portal (`web/`) — đăng nhập Farm / Retailer / Shipping / Guest |
| `http://localhost:8080/admin` | Admin dashboard (`web/src/admin`) — bảng điều khiển quản trị |
| `http://localhost:8080/api/**` | Backend API |

Cách chạy:

1. Build app web (`web/`) rồi copy `web/dist` vào static resources của backend:
   ```bash
   cd web
   npm install
   npm run build

   cd ..
   rm -rf backend/src/main/resources/static
   mkdir -p backend/src/main/resources/static
   cp -r web/dist/. backend/src/main/resources/static/
   ```
   (thư mục `backend/src/main/resources/static/` được gitignore vì là build artifact)
2. Chạy backend như mục 3:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. Mở `http://localhost:8080/` (portal) hoặc `http://localhost:8080/admin` (quản trị) — không cần chạy thêm server React nào.

Deep-link SPA (`/trace/<hash>`, `/admin/...`) đã được `SpaForwardController` forward về đúng `index.html`, refresh không bị 404.

> **Dev hot-reload:** nếu vẫn muốn sửa code React và thấy ngay, chạy `npm run dev` trong `web/` (port 5174) — portal ở `http://localhost:5174/`, admin ở `http://localhost:5174/admin`. Hai chế độ này song song, không ảnh hưởng nhau.

### Tài khoản test (đã seed sẵn — trên trang login có nút điền nhanh)

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Super Admin | `superadmin@bicap.com` | `Superadmin@2026` |
| Admin | `admin@bicap.com` | `Adminpassword@2026` |
| Moderator | `moderator@bicap.com` | `Moderator@2026` |
| Farm Manager | `farm@bicap.com` | `Farmpassword@2026` |
| Retailer | `retailer@bicap.com` | `Retailpassword@2026` |
| Shipping Manager | `shipping_mgr@bicap.com` | `Shipping@2026` |
| Driver | `driver@bicap.com` | `Driver@2026` |

---

## 5. Running the Frontend Dashboard (React Vite)

The unified web app lives in the `web/` directory (portal at `/` and admin dashboard at `/admin` in a single React 19 + TypeScript + Vite app). The dev server must run on **port 5174** to align with the CORS policy allowed by the backend.

1. Open a new terminal window.
2. Navigate to the `web` directory:
   ```bash
   cd web
   ```
3. Install the dependencies:
   ```bash
   npm install
   ```
4. Run the development server on port 5174:
   ```bash
   npm run dev
   ```
5. Open your browser and navigate to:
   * Portal (Farm / Retailer / Shipping / Guest): `http://localhost:5174/`
   * Admin dashboard: `http://localhost:5174/admin`

The portal and the admin dashboard share one session, stored in `localStorage` (`accessToken`, `currentUser`, `refreshToken`); tokens are **not** passed through the URL (`?token=`).

Other web commands (run from `web/`):

```bash
npm ci              # clean install from package-lock.json (used by CI)
npm run lint        # oxlint
npm test            # vitest run — 28 tests
npm run build       # tsc -b && vite build → web/dist
npm run preview     # preview the production build on port 5174
```
