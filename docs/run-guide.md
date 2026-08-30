# BICAP - Running and Deployment Guide

This document contains step-by-step instructions to run the Blockchain Agricultural Platform (BICAP) backend API and React admin dashboard.

---

## 1. Prerequisites

Ensure you have the following installed on your machine:
*   **Java JDK 21**
*   **Node.js (v20 or newer)** & **npm**
*   **IntelliJ IDEA** (Optional, recommended for development)
*   **Docker & Docker Compose** (Optional, for containerized deployments)

---

## 2. Configuration (`.env`)

We have created a `.env` file at the root of the project with the credentials for the online databases (MySQL and Redis). 
For local runs, the application will use the environment variables defined in this file.

*   **Database Configs**: Connects to the host `free02.123host.vn`.
*   **Redis Caching**: Connects to the host `foamy-ship-mind-96497.db.redis.io`.
*   **Frontend API URL**: Pointed to `http://localhost:8080/api/admins`.

---

## 3. Running the Backend API (Spring Boot)

### Option A: Running in IntelliJ IDEA (Recommended)
1. Open the project in IntelliJ IDEA.
2. IntelliJ will detect the Maven configurations automatically.
3. Open `src/main/java/vn/courses/ut/edu/javaprogramming/bicap/Application.java` and click the green **Run** button.
4. To pass the environment variables from the `.env` file, install the **EnvFile** plugin in IntelliJ or set them in the Run Configuration settings.

### Option B: Running via Terminal (PowerShell)
If Maven (`mvn`) is not registered in your global system `PATH`, you can use the Maven executable bundled with your IntelliJ installation:

1. Open PowerShell.
2. Export the database environment variables to your session:
   ```powershell
   $env:SPRING_DATASOURCE_URL="jdbc:mysql://free02.123host.vn:3306/roacqgfa_bicap?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   $env:SPRING_DATASOURCE_USERNAME="roacqgfa_bicap"
   $env:SPRING_DATASOURCE_PASSWORD="laptrinhjavahahaha"
   $env:SPRING_REDIS_HOST="foamy-ship-mind-96497.db.redis.io"
   $env:SPRING_REDIS_PORT="16599"
   $env:SPRING_REDIS_PASSWORD="6eYWXsUsc9rPuDhQmrQClml4HZfqFX87"
   $env:SPRING_REDIS_SSL="true"
   ```
3. Run the Spring Boot application using the IntelliJ Maven bundle path:
   ```powershell
   & "C:\Program Files\JetBrains\IntelliJ IDEA 2026.2\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" spring-boot:run
   ```

---

## 4. Single-Port Mode — Spring Boot phục vụ cả 2 frontend (Khuyên dùng cho demo/test)

Toàn bộ hệ thống chạy trên **một port duy nhất 8080**:

| URL | Nội dung |
|---|---|
| `http://localhost:8080/` | Farm Portal (`frontend`) — đăng nhập Farm / Retailer / Admin |
| `http://localhost:8080/admin/` | Admin Web (`admin-web`) — bảng điều khiển quản trị |
| `http://localhost:8080/api/**` | Backend API |

Cách chạy (Windows):

1. Build và lắp 2 app React vào JAR:
   ```bat
   build-web.bat
   ```
   (script chạy `npm run build` cho cả `frontend` và `admin-web`, rồi copy `dist/` vào `src/main/resources/static/` — thư mục này được gitignore vì là build artifact)
2. Chạy backend như mục 3 (`run-backend.bat` hoặc `mvn spring-boot:run`).
3. Mở `http://localhost:8080/` — không cần chạy thêm server React nào.

Deep-link SPA (`/trace/<hash>`, `/admin/farm`, `/admin/retail`…) đã được `SpaForwardController` forward về đúng `index.html`, refresh không bị 404.

> **Dev hot-reload:** nếu vẫn muốn sửa code React và thấy ngay, chạy riêng `npm run dev` trong từng thư mục (farm 5174, admin 5173 — vào `http://localhost:5173/admin/`). Hai chế độ này song song, không ảnh hưởng nhau.

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

The frontend client must run on **port 3001** to align with the CORS policy allowed by the backend.

1. Open a new terminal window.
2. Navigate to the `admin-web` directory:
   ```bash
   cd admin-web
   ```
3. Install the dependencies:
   ```bash
   npm install
   ```
4. Run the development server specifying port 3001:
   ```bash
   npm run dev -- --port 3001
   ```
5. Open your browser and navigate to `http://localhost:3001`.

---

## 6. Running via Docker Compose (Production/Contanerized)

We configured a unified multi-container docker compose setup inside `docker-compose.db.yml` to launch both the backend API and the React web application:

1. Ensure Docker is running.
2. From the root directory, build and launch the containers:
   ```bash
   docker-compose -f docker-compose.db.yml up -d --build
   ```
3. This spins up:
   *   **Backend (`bicap-api`)**: Running on `http://localhost:8080` and connecting to the online MySQL/Redis.
   *   **Frontend (`bicap-admin-web`)**: Running on `http://localhost:3001` inside Nginx with Single Page Application routing support.
