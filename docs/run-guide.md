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

There is **no development fallback anymore**: the values in `.env` are the production posture and apply to every profile. A missing infrastructure dependency or secret **stops the application at startup** with an explicit error message.

*   **Configuration is loaded from `.env`**: the backend imports it itself via `spring.config.import=optional:file:./.env[.properties],optional:file:../.env[.properties]`, so `mvn spring-boot:run` works from `backend/` as well as from the repo root. The helper script `dev/run-backend.ps1` exports every `.env` entry into the process environment and then runs `mvn spring-boot:run` (use it, or the IntelliJ EnvFile plugin, if you prefer explicit export).
*   **Database Configs** (`SPRING_DATASOURCE_*`): a **remote MySQL** server is required — `spring.datasource.url` has no H2 default. A blank or H2 URL stops the application (H2 is accepted only for test/CI together with `ALLOW_SIMULATION=true`).
*   **Redis Caching** (`SPRING_REDIS_*`): a reachable **remote Redis** is required. There is no in-memory cache fallback: if Redis cannot be reached, the backend refuses to start. Cache is switched off only by deliberately setting `APP_CACHE_ENABLED=false` (test/CI).
*   **Secrets** (`JWT_SECRET`, `SEPAY_API_KEY`): mandatory in **every** profile — no secret is generated at runtime. Template values (including the ones previously published in this repo/`.env.example`) are rejected and the application stops.
*   **Blockchain** (`BLOCKCHAIN_MODE=live` + `BLOCKCHAIN_PRIVATE_KEY`): required, with a signer wallet holding VTHO. `BLOCKCHAIN_MODE=mock` and `BLOCKCHAIN_EXPORT_MODE=local` are accepted only when `ALLOW_SIMULATION=true` (test/CI).
*   **Web API URL** (`VITE_API_BASE_URL`, a build-time variable of `web/`): points the web app to the backend, `http://localhost:8080`.

> **Test/CI only flag:** `ALLOW_SIMULATION=true` is the single switch that permits H2 + mock blockchain + local export stub. Never enable it for a real deployment.

---

## 3. Running the Backend API (Spring Boot)

The Maven project lives in the `backend/` directory.

### Option A: Running in IntelliJ IDEA (Recommended)
1. Open the repository (or the `backend/` folder) in IntelliJ IDEA.
2. IntelliJ will detect the Maven configuration from `backend/pom.xml` automatically.
3. Open `backend/src/main/java/vn/courses/ut/edu/javaprogramming/bicap/Application.java` and click the green **Run** button.
4. The `.env` file is imported automatically by `spring.config.import`, so no extra plugin is strictly required; the **EnvFile** plugin (or Run Configuration env vars) is still handy if you prefer explicit export.

The backend starts on **port 8080** only when the required production dependencies are reachable: remote MySQL, remote Redis, live blockchain (`BLOCKCHAIN_MODE=live` + `BLOCKCHAIN_PRIVATE_KEY`) and the `JWT_SECRET` / `SEPAY_API_KEY` secrets. Missing infrastructure stops the startup with an explicit error instead of falling back to H2 / mock / in-memory cache.

### Option B: Running via Terminal (PowerShell)
If Maven (`mvn`) is not registered in your global system `PATH`, you can use the Maven executable bundled with your IntelliJ installation:

1. Open PowerShell and change to the backend directory:
   ```powershell
   cd backend
   ```
2. Make sure `.env` exists at the repo root (copy `.env.example` and fill in the real MySQL / Redis / VeChain / Sepay values). The backend imports it on its own; the helper script does the export for you. If you prefer to override a variable in the session, export it like this (values here are placeholders — **never write real credentials into documentation**):
   ```powershell
   $env:SPRING_DATASOURCE_URL="jdbc:mysql://<mysql-host>:3306/<database>?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   $env:SPRING_DATASOURCE_USERNAME="<db-user>"
   $env:SPRING_DATASOURCE_PASSWORD="<db-password>"
   $env:SPRING_REDIS_HOST="<redis-host>"
   $env:SPRING_REDIS_PORT="<redis-port>"
   $env:SPRING_REDIS_PASSWORD="<redis-password>"
   $env:SPRING_REDIS_SSL="true"
   $env:JWT_SECRET="<base64 secret, >=32 bytes>"
   $env:SEPAY_API_KEY="<real Sepay key>"
   $env:BLOCKCHAIN_MODE="live"
   $env:BLOCKCHAIN_PRIVATE_KEY="<signer wallet hex key with VTHO>"
   ```
3. Run the Spring Boot application from the `backend/` directory:
   ```powershell
   mvn spring-boot:run
   ```
   Or from the repo root, load `.env` first and then start Maven:
   ```powershell
   .\dev\run-backend.ps1
   ```
   If `mvn` is not on `PATH`, use the IntelliJ Maven bundle path instead:
   ```powershell
   & "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" spring-boot:run
   ```

Other backend commands (run from `backend/`):

```bash
mvn test                        # 345 tests — no MySQL/Redis needed (H2 create-drop + cache off + ALLOW_SIMULATION=true in src/test/resources)
node ../dev/tests/cross-role-matrix.mjs   # 122 probe HTTP đa vai trò (cần app đang chạy)
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
npm test            # vitest run — 84 tests (22 test files)
npm run build       # tsc -b && vite build → web/dist
npm run preview     # preview the production build on port 5174
```
