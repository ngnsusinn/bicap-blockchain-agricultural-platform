# TÀI LIỆU THIẾT KẾ KIẾN TRÚC HỆ THỐNG (ARCHITECTURE DESIGN DOCUMENT)

## DỰ ÁN: TÍCH HỢP BLOCKCHAIN TRONG SẢN XUẤT NÔNG SẢN SẠCH (BICAP)

| Thông tin | Chi tiết |
|---|---|
| **Tên dự án (EN)** | Blockchain Integration in Clean Agricultural Production |
| **Tên dự án (VN)** | Tích hợp Blockchain trong sản xuất nông sản sạch |
| **Viết tắt** | BICAP |
| **Loại tài liệu** | Architecture Design Document |
| **Phiên bản tài liệu** | 1.0 |
| **Ngày tạo** | 24/07/2026 |
| **Trạng thái** | Bản nháp (Draft) |
| **Tham chiếu** | User Requirements v2.0, SRS v1.1, IEEE 42010-2011 |
| **Mã Jira** | BICAP-93 |

---

## Mục lục

1. [Tóm tắt tổng quan (Executive Summary)](#1-tóm-tắt-tổng-quan-executive-summary)
2. [Tổng quan kiến trúc (Architecture Overview)](#2-tổng-quan-kiến-trúc-architecture-overview)
3. [Quyết định thiết kế (Design Decisions)](#3-quyết-định-thiết-kế-design-decisions)
4. [Kiến trúc phân lớp (Layered Architecture)](#4-kiến-trúc-phân-lớp-layered-architecture)
5. [Thiết kế các thành phần cốt lõi (Core Components Design)](#5-thiết-kế-các-thành-phần-cốt-lõi-core-components-design)
6. [Kiến trúc tích hợp Blockchain](#6-kiến-trúc-tích-hợp-blockchain)
7. [Kiến trúc dữ liệu (Data Architecture)](#7-kiến-trúc-dữ-liệu-data-architecture)
8. [Kiến trúc tích hợp bên ngoài (Integration Architecture)](#8-kiến-trúc-tích-hợp-bên-ngoài-integration-architecture)
9. [Kiến trúc triển khai (Deployment Architecture)](#9-kiến-trúc-triển-khai-deployment-architecture)
10. [Mô hình bảo mật (Security Architecture)](#10-mô-hình-bảo-mật-security-architecture)
11. [Đặc tả hiệu năng và khả năng mở rộng (Performance & Scalability)](#11-đặc-tả-hiệu-năng-và-khả-năng-mở-rộng-performance--scalability)
12. [Luồng nghiệp vụ chính (Key Business Flows)](#12-luồng-nghiệp-vụ-chính-key-business-flows)
13. [Chiến lược kiểm thử kiến trúc (Architecture Testing Strategy)](#13-chiến-lược-kiểm-thử-kiến-trúc-architecture-testing-strategy)
14. [Phụ lục (Appendices)](#14-phụ-lục-appendices)

---

## 1. Tóm tắt tổng quan (Executive Summary)

### 1.1. Mục đích tài liệu

Tài liệu Thiết kế Kiến trúc này mô tả toàn bộ kiến trúc kỹ thuật của hệ thống BICAP — nền tảng tích hợp Blockchain trong sản xuất nông sản sạch. Tài liệu trình bày các quyết định kiến trúc, mô hình phân lớp, thiết kế các thành phần, tích hợp blockchain, cơ sở dữ liệu, triển khai, bảo mật và chiến lược mở rộng.

### 1.2. Đối tượng đọc

| Đối tượng | Mục đích sử dụng |
|-----------|-----------------|
| **Kiến trúc sư hệ thống** | Thiết kế và ra quyết định kiến trúc |
| **Developers** | Hiểu cấu trúc tổng thể để phát triển đúng mô hình |
| **DevOps Engineers** | Cấu hình hạ tầng và triển khai dựa trên kiến trúc |
| **Testers** | Lập kế hoạch kiểm thử tích hợp và hiệu năng |
| **Project Manager** | Đánh giá rủi ro kỹ thuật và phạm vi |
| **Stakeholders / Giảng viên** | Đánh giá và phê duyệt thiết kế |

### 1.3. Tài liệu tham chiếu

| STT | Tài liệu | Phiên bản | Ghi chú |
|-----|----------|-----------|---------|
| 1 | User Requirements Document | 2.0 | Yêu cầu người dùng chi tiết |
| 2 | Software Requirement Specifications (SRS) | 1.1 | Đặc tả yêu cầu phần mềm |
| 3 | Requirement Document | 1.0 | Tài liệu yêu cầu gốc |
| 4 | IEEE 42010-2011 | — | Chuẩn mô tả kiến trúc hệ thống |

### 1.4. Tóm tắt dành cho Stakeholders

Hệ thống BICAP được thiết kế theo kiến trúc **Client-Server 3 tầng** (Three-Tier Architecture), kết hợp tích hợp Blockchain VeChainThor để đảm bảo tính minh bạch và bất biến dữ liệu truy xuất nguồn gốc nông sản. Hệ thống bao gồm:

- **4 ứng dụng Web** (Admin, Farm Management, Retailer, Shipping Management) xây dựng bằng ReactJS/Next.js
- **2 ứng dụng Mobile** (Shipping Driver, Guest) xây dựng bằng React Native
- **1 Backend API** trung tâm (Java Spring Boot 3.x) kết nối với MySQL, Redis và VeChainThor Blockchain
- **Hạ tầng cloud** (AWS/Google Cloud) với Docker containerization

Kiến trúc được thiết kế để đáp ứng yêu cầu phi chức năng: thời gian phản hồi < 2 giây, 500 người dùng đồng thời, uptime ≥ 99.5%, và xử lý ≥ 100 giao dịch blockchain/phút.

---

## 2. Tổng quan kiến trúc (Architecture Overview)

### 2.1. Mô hình kiến trúc tổng quan (System Context Diagram — C4 Level 1)

```
                         ┌────────────────────────────────────────┐
                         │           NGƯỜI DÙNG / ACTORS           │
                         │                                        │
                         │  ┌───────┐ ┌────┐ ┌────────┐ ┌─────┐  │
                         │  │ Admin │ │ FM │ │  RT    │ │ SM  │  │
                         │  └───┬───┘ └──┬─┘ └───┬────┘ └──┬──┘  │
                         │      │        │       │         │      │
                         │  ┌───┴────┐  ┌┴───────┴─┐  ┌───┴──┐  │
                         │  │   SD   │  │    GS    │  │      │  │
                         │  └───┬────┘  └────┬─────┘  │      │  │
                         └──────┼────────────┼────────┘──────┘──┘
                                │            │
                    ┌───────────┼────────────┼────────────┐
                    │           ▼            ▼            │
                    │  ┌─────────────────────────────┐   │
                    │  │    HỆ THỐNG BICAP           │   │
                    │  │                             │   │
                    │  │  ┌───────────────────────┐  │   │
                    │  │  │  Presentation Tier    │  │   │
                    │  │  │  (Web Apps + Mobile)  │  │   │
                    │  │  └───────────┬───────────┘  │   │
                    │  │              │               │   │
                    │  │  ┌───────────┴───────────┐  │   │
                    │  │  │   Application Tier    │  │   │
                    │  │  │  (Spring Boot API)    │  │   │
                    │  │  └───────────┬───────────┘  │   │
                    │  │              │               │   │
                    │  │  ┌───────────┴───────────┐  │   │
                    │  │  │     Data Tier         │  │   │
                    │  │  │  (MySQL + Redis)      │  │   │
                    │  │  └──────────────────────┘  │   │
                    │  └─────────────────────────────┘   │
                    └────────────────────────────────────┘
                                     │
                    ┌────────────────┼─────────────────┐
                    │                │                  │
              ┌─────┴──────┐  ┌─────┴──────┐  ┌───────┴──────┐
              │ VeChainThor│  │  Payment   │  │ IoT Sensors  │
              │ Blockchain │  │  Gateway   │  │ (Temp/pH/Hum)│
              └────────────┘  └────────────┘  └──────────────┘
                          HỆ THỐNG BÊN NGOÀI
```

**Chú giải vai trò:**
- **ADM** — Admin (Quản trị viên)
- **FM** — Farm Manager (Chủ trang trại)
- **RT** — Retailer (Nhà bán lẻ)
- **SM** — Shipping Manager (Quản lý vận chuyển)
- **SD** — Shipping Driver (Tài xế)
- **GS** — Guest (Khách truy cập)

### 2.2. Kiến trúc Container (C4 Level 2)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                               HỆ THỐNG BICAP                                    │
│                                                                                  │
│  ┌────────────────────── PRESENTATION TIER ────────────────────────────────────┐ │
│  │                                                                             │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐  │ │
│  │  │   Admin     │  │    Farm     │  │  Retailer   │  │    Shipping      │  │ │
│  │  │   Web App   │  │  Mgmt App  │  │   Web App   │  │   Mgmt Web App   │  │ │
│  │  │             │  │            │  │             │  │                  │  │ │
│  │  │  ReactJS/   │  │  ReactJS/  │  │  ReactJS/   │  │   ReactJS/       │  │ │
│  │  │  Next.js    │  │  Next.js   │  │  Next.js    │  │   Next.js        │  │ │
│  │  │  TypeScript │  │  TypeScript│  │  TypeScript │  │   TypeScript     │  │ │
│  │  └──────┬──────┘  └──────┬─────┘  └──────┬──────┘  └────────┬─────────┘  │ │
│  │         │                │               │                   │            │ │
│  │  ┌──────┴───────────┐  ┌┴────────────────┴──┐                │            │ │
│  │  │  Shipping Driver │  │     Guest App      │                │            │ │
│  │  │  Mobile App      │  │   (Web / Mobile)   │                │            │ │
│  │  │                  │  │                    │                │            │ │
│  │  │  React Native    │  │   React Native     │                │            │ │
│  │  │  TypeScript      │  │   TypeScript       │                │            │ │
│  │  └────────┬─────────┘  └──────────┬─────────┘                │            │ │
│  └───────────┼───────────────────────┼──────────────────────────┼────────────┘ │
│              │                       │                          │              │
│              └───────────────────────┼──────────────────────────┘              │
│                                      │                                        │
│                              HTTPS / WebSocket                                │
│                                      │                                        │
│  ┌───────────────────────── APPLICATION TIER ─────────────────────────────────┐│
│  │                                   │                                        ││
│  │                     ┌─────────────┴─────────────┐                          ││
│  │                     │    API GATEWAY / NGINX     │                          ││
│  │                     │    (Reverse Proxy + LB)    │                          ││
│  │                     └─────────────┬─────────────┘                          ││
│  │                                   │                                        ││
│  │                     ┌─────────────┴─────────────┐                          ││
│  │                     │    SPRING BOOT API         │                          ││
│  │                     │    (Java 21 / JDK 21)      │                          ││
│  │                     │                            │                          ││
│  │                     │  ┌──────────────────────┐  │                          ││
│  │                     │  │  Controller Layer    │  │                          ││
│  │                     │  │  (REST Controllers)  │  │                          ││
│  │                     │  └──────────┬───────────┘  │                          ││
│  │                     │             │              │                          ││
│  │                     │  ┌──────────┴───────────┐  │                          ││
│  │                     │  │   Service Layer      │  │                          ││
│  │                     │  │  (Business Logic)    │  │                          ││
│  │                     │  └──────────┬───────────┘  │                          ││
│  │                     │             │              │                          ││
│  │                     │  ┌──────────┴───────────┐  │                          ││
│  │                     │  │  Repository Layer    │  │                          ││
│  │                     │  │  (Data Access / JPA) │  │                          ││
│  │                     │  └──────────────────────┘  │                          ││
│  │                     └────────────────────────────┘                          ││
│  └────────────────────────────────────────────────────────────────────────────┘│
│                                      │                                        │
│  ┌─────────────────────── DATA TIER ─────────────────────────────────────────┐│
│  │                                   │                                        ││
│  │    ┌──────────────┐    ┌──────────┴───┐    ┌───────────────────┐           ││
│  │    │  MySQL 5.7.41│    │  Redis 8.6   │    │  VeChainThor      │           ││
│  │    │              │    │              │    │  Blockchain        │           ││
│  │    │  Primary DB  │    │  Cache Layer │    │                   │           ││
│  │    │  (Relational │    │  - Session   │    │  - Smart Contracts│           ││
│  │    │   Data)      │    │  - Cache     │    │  - Immutable Data │           ││
│  │    │              │    │  - Rate Limit│    │  - Traceability   │           ││
│  │    └──────────────┘    └──────────────┘    └───────────────────┘           ││
│  └────────────────────────────────────────────────────────────────────────────┘│
│                                                                                │
│  ┌───────────── EXTERNAL INTEGRATIONS ────────────────────────────────────────┐│
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐  ┌─────────────┐ ││
│  │  │  Payment     │  │  Email       │  │  Firebase      │  │ IoT Gateway │ ││
│  │  │  Gateway     │  │  Service     │  │  Cloud Msg     │  │ (MQTT/REST) │ ││
│  │  │  (VNPay/     │  │  (SMTP/      │  │  (FCM)         │  │             │ ││
│  │  │   MoMo)      │  │   SendGrid)  │  │                │  │             │ ││
│  │  └──────────────┘  └──────────────┘  └────────────────┘  └─────────────┘ ││
│  └────────────────────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 2.3. Bảng tóm tắt thành phần

| # | Thành phần | Công nghệ | Vai trò | Giao thức |
|---|-----------|-----------|---------|-----------|
| 1 | Admin Web App | ReactJS/Next.js (TS) | Quản trị hệ thống | HTTPS |
| 2 | Farm Management Web App | ReactJS/Next.js (TS) | Quản lý trang trại & mùa vụ | HTTPS |
| 3 | Retailer Web App | ReactJS/Next.js (TS) | Mua bán nông sản trên sàn | HTTPS |
| 4 | Shipping Mgmt Web App | ReactJS/Next.js (TS) | Quản lý vận chuyển | HTTPS |
| 5 | Shipping Driver Mobile App | React Native (TS) | Cập nhật hành trình, QR scan | HTTPS + FCM |
| 6 | Guest App | React Native / Web (TS) | Tra cứu sản phẩm, giáo dục | HTTPS |
| 7 | API Gateway / Nginx | Nginx | Reverse proxy, load balancing, SSL | HTTPS |
| 8 | Backend API | Java Spring Boot 3.x (JDK 21) | Logic nghiệp vụ trung tâm | REST + WebSocket |
| 9 | MySQL Database | MySQL 5.7.41 | Lưu trữ dữ liệu quan hệ | JDBC |
| 10 | Redis Cache | Redis 8.6 | Cache, session, rate limiting | Redis Protocol |
| 11 | VeChainThor Blockchain | VeChainThor + Solidity | Dữ liệu truy xuất bất biến | Thor REST API |
| 12 | Payment Gateway | VNPay / MoMo | Xử lý thanh toán | REST API |
| 13 | Email Service | SMTP / SendGrid | Gửi thông báo email | SMTP |
| 14 | Firebase Cloud Messaging | FCM | Push notification cho mobile | FCM API |
| 15 | IoT Gateway | MQTT Broker | Nhận dữ liệu cảm biến | MQTT / REST |

---

## 3. Quyết định thiết kế (Design Decisions)

### 3.1. Tổng hợp các quyết định kiến trúc (Architecture Decision Records — ADR)

#### ADR-001: Kiến trúc Monolithic Backend

| Thuộc tính | Chi tiết |
|------------|----------|
| **Quyết định** | Sử dụng kiến trúc **Monolithic** cho Backend API thay vì Microservices |
| **Bối cảnh** | Dự án có quy mô vừa, team phát triển nhỏ (đội sinh viên), yêu cầu triển khai nhanh |
| **Lý do chọn** | ① Đơn giản hóa việc phát triển, debug và deploy ② Giảm overhead vận hành (không cần service mesh, message queue phức tạp) ③ Team nhỏ dễ quản lý codebase thống nhất ④ Phù hợp với giai đoạn MVP |
| **Đánh đổi** | Khó scale từng module riêng lẻ; coupling cao hơn microservices |
| **Giảm thiểu** | Tổ chức code theo module rõ ràng (package-by-feature), sử dụng interface để tách biệt logic; chuẩn bị sẵn sàng để tách thành microservices nếu cần |

---

#### ADR-002: Java Spring Boot cho Backend

| Thuộc tính | Chi tiết |
|------------|----------|
| **Quyết định** | Sử dụng **Java (Spring Boot 3.x, JDK 21)** cho Backend API |
| **Bối cảnh** | Yêu cầu dự án chỉ định sử dụng Java; cần tích hợp VeChainThor Blockchain |
| **Lý do chọn** | ① Ràng buộc từ yêu cầu dự án ② Hệ sinh thái Spring Boot phong phú (Spring Data JPA, Spring Security, Spring WebSocket) ③ web3j/VeChain Java SDK hỗ trợ tương tác blockchain ④ JDK 21 cung cấp virtual threads, pattern matching |
| **Đánh đổi** | Nặng hơn Node.js về bộ nhớ; thời gian khởi động lâu hơn |

---

#### ADR-003: ReactJS/Next.js cho Web Frontend

| Thuộc tính | Chi tiết |
|------------|----------|
| **Quyết định** | Sử dụng **ReactJS / Next.js (TypeScript)** cho tất cả Web App |
| **Bối cảnh** | Cần xây dựng 4 Web App với UX nhất quán; yêu cầu dự án chỉ định ReactJS |
| **Lý do chọn** | ① Ràng buộc từ yêu cầu dự án ② Component-based architecture phù hợp tái sử dụng UI ③ Next.js hỗ trợ SSR/SSG tối ưu SEO cho Guest App ④ TypeScript đảm bảo type safety |
| **Đánh đổi** | Đường cong học tập cho team mới; cần quản lý state cẩn thận |

---

#### ADR-004: React Native cho Mobile

| Thuộc tính | Chi tiết |
|------------|----------|
| **Quyết định** | Sử dụng **React Native (TypeScript)** cho ứng dụng Mobile |
| **Bối cảnh** | Cần phát triển cho cả Android và iOS; chia sẻ logic với Web App |
| **Lý do chọn** | ① Ràng buộc từ yêu cầu dự án ② Cross-platform, một codebase cho 2 nền tảng ③ Chia sẻ kiến thức React giữa Web và Mobile team ④ Hỗ trợ tốt camera (QR scan), GPS |

---

#### ADR-005: VeChainThor Blockchain

| Thuộc tính | Chi tiết |
|------------|----------|
| **Quyết định** | Sử dụng **VeChainThor** làm nền tảng Blockchain |
| **Bối cảnh** | Cần blockchain công cộng tối ưu cho chuỗi cung ứng và truy xuất nguồn gốc |
| **Lý do chọn** | ① Ràng buộc từ yêu cầu dự án ② Thiết kế riêng cho enterprise supply chain ③ Chi phí giao dịch thấp (dual-token: VET + VTHO) ④ Consensus mechanism PoA phù hợp tốc độ xử lý ⑤ Có sẵn toolchain: VeChain Sync, Stats, ToolChain |
| **Đánh đổi** | Ít phổ biến hơn Ethereum; tài liệu/cộng đồng nhỏ hơn |

---

#### ADR-006: Chiến lược lưu trữ dữ liệu kép (Dual Storage)

| Thuộc tính | Chi tiết |
|------------|----------|
| **Quyết định** | Sử dụng mô hình **Dual Storage** — MySQL cho dữ liệu vận hành + VeChainThor cho dữ liệu truy xuất bất biến |
| **Bối cảnh** | Blockchain có chi phí ghi cao, tốc độ chậm; không phải tất cả dữ liệu cần immutability |
| **Lý do chọn** | ① Chỉ ghi dữ liệu cần tính bất biến lên Blockchain (mùa vụ, quy trình, xuất kho) ② Dữ liệu vận hành (users, orders, shipments) lưu MySQL cho hiệu năng ③ Liên kết qua transaction hash ④ Tiết kiệm chi phí gas (VTHO) |
| **Đánh đổi** | Cần đồng bộ và liên kết dữ liệu giữa 2 nguồn; phức tạp hơn single-source |

---

#### ADR-007: Redis cho Caching Layer

| Thuộc tính | Chi tiết |
|------------|----------|
| **Quyết định** | Sử dụng **Redis 8.6** làm caching layer |
| **Bối cảnh** | Cần đáp ứng yêu cầu NFR-005 (response < 2 giây) và NFR-006 (cache hit ≥ 80%) |
| **Lý do chọn** | ① Ràng buộc từ yêu cầu dự án ② In-memory data store có tốc độ truy xuất rất cao ③ Hỗ trợ nhiều data structures (string, hash, list, sorted set) ④ Dùng cho cache, session management, rate limiting |

### 3.2. Ma trận quyết định theo Quality Attribute

| Quality Attribute | Quyết định kiến trúc | Ảnh hưởng |
|---|---|---|
| **Performance** | Redis cache, connection pooling, pagination | Response < 2s |
| **Scalability** | Docker containerization, horizontal scaling, load balancer | 500 concurrent users |
| **Security** | JWT + RBAC, bcrypt, HTTPS/TLS 1.2+, OWASP Top 10 | Zero unauthorized access |
| **Availability** | Health check, auto-restart, cloud deployment | Uptime ≥ 99.5% |
| **Integrity** | VeChainThor blockchain, immutable data records | 100% dữ liệu blockchain bất biến |
| **Maintainability** | Package-by-feature, coding conventions, logging | Code review pass ≥ 90% |
| **Interoperability** | RESTful API, standard protocols (MQTT, HTTPS) | Tích hợp IoT, Payment, FCM |

---

## 4. Kiến trúc phân lớp (Layered Architecture)

### 4.1. Tổng quan phân lớp

Hệ thống BICAP được thiết kế theo mô hình **kiến trúc phân lớp 3 tầng** (Three-Tier Architecture), với phân lớp rõ ràng trong Backend API theo mô hình Spring MVC:

```
┌──────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION TIER                                │
│   ┌────────────────────────────────────────────────────────────┐     │
│   │  Web Applications (ReactJS/Next.js)                        │     │
│   │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ │     │
│   │  │  Admin   │ │   Farm   │ │ Retailer │ │  Shipping    │ │     │
│   │  │  SPA     │ │  Mgmt    │ │   SPA    │ │  Mgmt SPA    │ │     │
│   │  └──────────┘ └──────────┘ └──────────┘ └──────────────┘ │     │
│   ├────────────────────────────────────────────────────────────┤     │
│   │  Mobile Applications (React Native)                       │     │
│   │  ┌──────────────┐  ┌──────────────┐                      │     │
│   │  │  Ship Driver │  │    Guest     │                      │     │
│   │  └──────────────┘  └──────────────┘                      │     │
│   └────────────────────────────────────────────────────────────┘     │
│                               │                                      │
│                       HTTPS / WebSocket                              │
│                               │                                      │
├───────────────────────────────┼──────────────────────────────────────┤
│                      APPLICATION TIER                                 │
│                               │                                      │
│   ┌───────────────────────────┴──────────────────────────────────┐   │
│   │                     SPRING BOOT API                          │   │
│   │                                                              │   │
│   │   ┌─────────────────────────────────────────────────────┐   │   │
│   │   │  ① CONTROLLER LAYER (REST API Endpoints)            │   │   │
│   │   │     @RestController, @RequestMapping                │   │   │
│   │   │     Request validation, Response formatting         │   │   │
│   │   │     Exception handling (@ControllerAdvice)          │   │   │
│   │   └────────────────────────┬────────────────────────────┘   │   │
│   │                            │                                │   │
│   │   ┌────────────────────────┴────────────────────────────┐   │   │
│   │   │  ② SERVICE LAYER (Business Logic)                   │   │   │
│   │   │     @Service, @Transactional                        │   │   │
│   │   │     Business rules, Validation, Orchestration       │   │   │
│   │   │     Blockchain integration, Payment processing      │   │   │
│   │   └────────────────────────┬────────────────────────────┘   │   │
│   │                            │                                │   │
│   │   ┌────────────────────────┴────────────────────────────┐   │   │
│   │   │  ③ REPOSITORY LAYER (Data Access)                   │   │   │
│   │   │     @Repository, Spring Data JPA                    │   │   │
│   │   │     JPA Entities, Query Methods                     │   │   │
│   │   │     Redis cache operations                          │   │   │
│   │   └─────────────────────────────────────────────────────┘   │   │
│   │                                                              │   │
│   │   ┌─────────────────────────────────────────────────────┐   │   │
│   │   │  CROSS-CUTTING CONCERNS                              │   │   │
│   │   │  ┌──────────┐ ┌──────────┐ ┌──────┐ ┌───────────┐  │   │   │
│   │   │  │ Security │ │ Logging  │ │ Cache│ │ Exception │  │   │   │
│   │   │  │ (JWT/    │ │ (SLF4J/ │ │(Redis│ │ Handling  │  │   │   │
│   │   │  │  RBAC)   │ │ Logback)│ │ )    │ │           │  │   │   │
│   │   │  └──────────┘ └──────────┘ └──────┘ └───────────┘  │   │   │
│   │   └─────────────────────────────────────────────────────┘   │   │
│   └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│                         DATA TIER                                     │
│   ┌──────────────┐    ┌──────────────┐    ┌───────────────────┐      │
│   │  MySQL 5.7.41│    │  Redis 8.6   │    │  VeChainThor      │      │
│   │  (Primary DB)│    │  (Cache)     │    │  (Blockchain)     │      │
│   └──────────────┘    └──────────────┘    └───────────────────┘      │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.2. Chi tiết từng lớp

#### 4.2.1. Presentation Tier (Tầng trình diễn)

| Đặc điểm | Chi tiết |
|-----------|----------|
| **Kiến trúc** | Single Page Application (SPA) cho Web; Native app cho Mobile |
| **State Management** | Redux Toolkit / Zustand (Web), React Context + AsyncStorage (Mobile) |
| **HTTP Client** | Axios với interceptors cho JWT token management |
| **Routing** | React Router (Web), React Navigation (Mobile) |
| **UI Framework** | Ant Design / Material UI (Web), React Native Paper (Mobile) |
| **Real-time** | WebSocket client (SockJS + STOMP) |
| **Caching** | SWR / React Query cho server state caching |

#### 4.2.2. Application Tier (Tầng ứng dụng)

| Lớp | Trách nhiệm | Annotations Spring |
|-----|-------------|-------------------|
| **Controller** | Tiếp nhận HTTP request, validate input, format response | `@RestController`, `@RequestMapping`, `@Valid` |
| **Service** | Logic nghiệp vụ, orchestration, transaction management | `@Service`, `@Transactional` |
| **Repository** | Truy cập dữ liệu, JPA queries, cache operations | `@Repository`, `JpaRepository` |
| **DTO** | Data Transfer Objects — chuyển đổi giữa entity và response | `record`, `@Builder` |
| **Entity** | JPA entities mapping với MySQL tables | `@Entity`, `@Table` |
| **Config** | Cấu hình Spring Security, Redis, WebSocket, Blockchain | `@Configuration`, `@Bean` |

#### 4.2.3. Data Tier (Tầng dữ liệu)

| Thành phần | Vai trò | Dữ liệu lưu trữ |
|-----------|---------|------------------|
| **MySQL 5.7.41** | Primary database cho dữ liệu vận hành | Users, Farms, Orders, Shipments, Payments, ... |
| **Redis 8.6** | Cache layer, session store, rate limiter | Session data, cached queries, IoT data, rate limits |
| **VeChainThor** | Immutable ledger cho truy xuất nguồn gốc | Farming seasons, processes, export records, QR metadata |

### 4.3. Cấu trúc Package Backend (Package-by-Feature)

```
vn.courses.ut.edu.javaprogramming.bicap/
├── config/                          # Cấu hình chung
│   ├── SecurityConfig.java          # Spring Security + JWT
│   ├── RedisConfig.java             # Redis cache configuration
│   ├── WebSocketConfig.java         # WebSocket + STOMP
│   ├── BlockchainConfig.java        # VeChainThor connection
│   └── CorsConfig.java             # CORS configuration
│
├── common/                          # Shared components
│   ├── dto/                         # Common DTOs (ApiResponse, PageResponse)
│   ├── exception/                   # Global exception handling
│   ├── security/                    # JWT utilities, filters
│   ├── util/                        # Utility classes
│   └── constant/                    # Enums, constants
│
├── auth/                            # Module xác thực
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── dto/LoginRequest.java, RegisterRequest.java
│   └── ...
│
├── admin/                           # Module Admin
│   ├── controller/AdminController.java
│   ├── service/AdminService.java
│   ├── repository/AdminRepository.java
│   └── ...
│
├── farm/                            # Module Farm Management
│   ├── controller/
│   │   ├── FarmController.java
│   │   └── FarmingSeasonController.java
│   ├── service/
│   │   ├── FarmService.java
│   │   └── FarmingSeasonService.java
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── retailer/                        # Module Retailer
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── ...
│
├── order/                           # Module Order
│   ├── controller/OrderController.java
│   ├── service/OrderService.java
│   ├── entity/Order.java
│   └── ...
│
├── shipping/                        # Module Shipping Management
│   ├── controller/
│   ├── service/
│   ├── entity/
│   └── ...
│
├── blockchain/                      # Module Blockchain Integration
│   ├── service/BlockchainService.java
│   ├── contract/                    # Smart contract wrappers
│   │   ├── FarmingSeasonContract.java
│   │   ├── FarmingProcessContract.java
│   │   └── TraceabilityContract.java
│   ├── dto/
│   └── util/
│
├── payment/                         # Module Payment
│   ├── controller/PaymentController.java
│   ├── service/PaymentService.java
│   └── ...
│
├── notification/                    # Module Notification
│   ├── service/NotificationService.java
│   ├── websocket/WebSocketHandler.java
│   ├── fcm/FCMService.java
│   └── ...
│
├── iot/                             # Module IoT Integration
│   ├── service/IoTService.java
│   ├── mqtt/MqttSubscriber.java
│   └── ...
│
├── content/                         # Module Content (bài viết giáo dục)
│   ├── controller/ArticleController.java
│   ├── service/ArticleService.java
│   └── ...
│
└── report/                          # Module Report
    ├── controller/ReportController.java
    ├── service/ReportService.java
    └── ...
```

---

## 5. Thiết kế các thành phần cốt lõi (Core Components Design)

### 5.1. Component Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      SPRING BOOT APPLICATION                              │
│                                                                           │
│  ┌─────────────┐  ┌─────────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │    Auth      │  │ Farm Management │  │   Order      │  │ Shipping  │ │
│  │   Module     │  │    Module       │  │   Module     │  │  Module   │ │
│  │             │  │                 │  │              │  │           │ │
│  │ • Login     │  │ • Farm CRUD    │  │ • Create     │  │ • Create  │ │
│  │ • Register  │  │ • Season CRUD  │  │ • Accept     │  │ • Track   │ │
│  │ • JWT       │  │ • Process Mgmt │  │ • Deposit    │  │ • Update  │ │
│  │ • RBAC      │  │ • Export/QR    │  │ • Cancel     │  │ • Confirm │ │
│  │             │  │ • Trading Floor│  │ • Confirm    │  │           │ │
│  └──────┬──────┘  └────────┬────────┘  └──────┬───────┘  └─────┬─────┘ │
│         │                  │                   │                │       │
│         │    ┌─────────────┼───────────────────┼────────────────┘       │
│         │    │             │                   │                        │
│  ┌──────┴────┴─────────────┴───────────────────┴──────────────────────┐ │
│  │                     SHARED SERVICES                                │ │
│  │                                                                    │ │
│  │  ┌──────────────┐  ┌─────────────────┐  ┌───────────────────────┐ │ │
│  │  │  Blockchain  │  │  Notification   │  │     Payment           │ │ │
│  │  │  Service     │  │  Service        │  │     Service           │ │ │
│  │  │              │  │                 │  │                       │ │ │
│  │  │ • Write TX   │  │ • In-app (WS)  │  │ • Service package    │ │ │
│  │  │ • Read TX    │  │ • Push (FCM)   │  │ • Order deposit      │ │ │
│  │  │ • QR Gen     │  │ • Email (SMTP) │  │ • Callback handling  │ │ │
│  │  │ • Verify     │  │                 │  │                       │ │ │
│  │  └──────────────┘  └─────────────────┘  └───────────────────────┘ │ │
│  │                                                                    │ │
│  │  ┌──────────────┐  ┌─────────────────┐  ┌───────────────────────┐ │ │
│  │  │     IoT      │  │     Media       │  │     Report            │ │ │
│  │  │   Service    │  │    Service      │  │     Service           │ │ │
│  │  │              │  │                 │  │                       │ │ │
│  │  │ • MQTT sub  │  │ • Upload files │  │ • Create reports     │ │ │
│  │  │ • Data proc │  │ • Image resize │  │ • Admin review       │ │ │
│  │  │ • Alert     │  │ • Storage      │  │                       │ │ │
│  │  └──────────────┘  └─────────────────┘  └───────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

### 5.2. Module Auth — Luồng xác thực JWT

```
  Client                   API Gateway            Spring Security           Auth Service
    │                         │                        │                         │
    │  POST /api/auth/login   │                        │                         │
    │────────────────────────►│                        │                         │
    │                         │    Forward request     │                         │
    │                         │───────────────────────►│                         │
    │                         │                        │  Validate credentials   │
    │                         │                        │────────────────────────►│
    │                         │                        │                         │
    │                         │                        │  ◄── User + Roles ─────│
    │                         │                        │                         │
    │                         │                        │  Generate JWT tokens    │
    │                         │                        │  (Access: 15min,        │
    │                         │                        │   Refresh: 7 days)      │
    │                         │                        │                         │
    │  ◄── {accessToken, refreshToken} ────────────────│                         │
    │                                                                            │
    │  GET /api/farms (Authorization: Bearer <token>)                            │
    │────────────────────────►│                        │                         │
    │                         │───────────────────────►│                         │
    │                         │                        │  JwtAuthFilter:         │
    │                         │                        │  1. Extract token       │
    │                         │                        │  2. Validate signature  │
    │                         │                        │  3. Check expiry        │
    │                         │                        │  4. Load UserDetails    │
    │                         │                        │  5. Set SecurityContext │
    │                         │                        │                         │
    │                         │                        │  @PreAuthorize check    │
    │                         │                        │  (role-based access)    │
    │                         │                        │                         │
    │  ◄── Response data ──────────────────────────────│                         │
```

**Cấu trúc JWT Token:**

```json
{
  "header": {
    "alg": "HS512",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "email": "user@example.com",
    "roles": ["FARM_MANAGER"],
    "permissions": ["FARM_READ", "FARM_WRITE", "SEASON_MANAGE"],
    "farmId": "farm-uuid",
    "iat": 1721836800,
    "exp": 1721837700
  }
}
```

### 5.3. Module Blockchain — Interaction Pattern

```
  Farm Service              Blockchain Service         VeChainThor          MySQL
      │                           │                       │                   │
      │  createFarmingSeason()    │                       │                   │
      │──────────────────────────►│                       │                   │
      │                           │                       │                   │
      │                           │  ① Save to MySQL     │                   │
      │                           │───────────────────────┼──────────────────►│
      │                           │                       │  ◄── entity ──────│
      │                           │                       │                   │
      │                           │  ② Build TX data     │                   │
      │                           │  (encode ABI params) │                   │
      │                           │                       │                   │
      │                           │  ③ Send Transaction  │                   │
      │                           │──────────────────────►│                   │
      │                           │                       │  Smart Contract   │
      │                           │                       │  execution        │
      │                           │  ◄── TX hash ─────────│                   │
      │                           │                       │                   │
      │                           │  ④ Save TX hash      │                   │
      │                           │───────────────────────┼──────────────────►│
      │                           │                       │  blockchain_tx    │
      │                           │                       │  record saved     │
      │                           │                       │                   │
      │  ◄── Success + TX hash ───│                       │                   │
      │                                                                       │
      │  [Exception: Blockchain TX failed]                                    │
      │                           │                       │                   │
      │                           │  ⑤ Retry (3 times)   │                   │
      │                           │──────────────────────►│                   │
      │                           │                       │                   │
      │                           │  [Still failed]       │                   │
      │                           │  ⑥ Queue for retry   │                   │
      │                           │───────────────────────┼──────────────────►│
      │                           │                       │  status=PENDING   │
      │  ◄── Partial success ─────│                       │                   │
```

### 5.4. Module Notification — Multi-Channel

```
┌───────────────────────────────────────────────────────────────────┐
│                    NOTIFICATION SERVICE                            │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                  Notification Dispatcher                     │ │
│  │                                                             │ │
│  │  Event Input ──► Resolve Channels ──► Dispatch to Channels  │ │
│  └──────────┬──────────────┬───────────────────┬───────────────┘ │
│             │              │                   │                 │
│  ┌──────────▼────┐  ┌─────▼──────────┐  ┌────▼──────────────┐ │
│  │  WebSocket    │  │  FCM Push      │  │  Email (SMTP /    │ │
│  │  Channel      │  │  Channel       │  │  SendGrid)        │ │
│  │               │  │                │  │                   │ │
│  │  • In-app     │  │  • Mobile push │  │  • Verification   │ │
│  │  • Real-time  │  │  • Background  │  │  • Notifications  │ │
│  │  • Toast msg  │  │    alerts      │  │  • Invoices       │ │
│  │               │  │                │  │                   │ │
│  │  STOMP over   │  │  Firebase      │  │  Spring Mail /    │ │
│  │  WebSocket    │  │  Admin SDK     │  │  SendGrid API     │ │
│  └───────────────┘  └────────────────┘  └───────────────────┘ │
│                                                                   │
│  Notification Types:                                              │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ ORDER_CREATED    │ SHIPMENT_UPDATE │ IOT_ALERT           │  │
│  │ ORDER_ACCEPTED   │ DELIVERY_DONE   │ FARM_APPROVED       │  │
│  │ DEPOSIT_PAID     │ REPORT_RECEIVED │ SERVICE_EXPIRING    │  │
│  └────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────┘
```

---

## 6. Kiến trúc tích hợp Blockchain

### 6.1. Tổng quan tích hợp VeChainThor

```
┌───────────────────────────────────────────────────────────────────────┐
│                    BLOCKCHAIN INTEGRATION LAYER                       │
│                                                                       │
│  ┌───────────────────────┐    ┌──────────────────────────────────┐   │
│  │   Spring Boot API     │    │        VeChainThor Network        │   │
│  │                       │    │                                  │   │
│  │  ┌─────────────────┐  │    │  ┌──────────────────────────┐   │   │
│  │  │ Blockchain      │  │    │  │  Smart Contracts          │   │   │
│  │  │ Service         │  │    │  │                          │   │   │
│  │  │                 │  │    │  │  ┌────────────────────┐  │   │   │
│  │  │  ┌───────────┐  │  │    │  │  │FarmingSeasonContract│  │   │   │
│  │  │  │ web3j /   │  │──┼────┼──│  │ • createSeason()  │  │   │   │
│  │  │  │ VeChain   │  │  │    │  │  │ • addProcess()    │  │   │   │
│  │  │  │ Java SDK  │  │  │    │  │  └────────────────────┘  │   │   │
│  │  │  └───────────┘  │  │    │  │                          │   │   │
│  │  │                 │  │    │  │  ┌────────────────────┐  │   │   │
│  │  │  ┌───────────┐  │  │    │  │  │ExportContract     │  │   │   │
│  │  │  │ Thor REST │  │──┼────┼──│  │ • recordExport()  │  │   │   │
│  │  │  │ API Client│  │  │    │  │  │ • generateQR()    │  │   │   │
│  │  │  └───────────┘  │  │    │  │  └────────────────────┘  │   │   │
│  │  │                 │  │    │  │                          │   │   │
│  │  │  ┌───────────┐  │  │    │  │  ┌────────────────────┐  │   │   │
│  │  │  │ TX Queue  │  │  │    │  │  │TraceabilityContract│  │   │   │
│  │  │  │ (Retry)   │  │  │    │  │  │ • getTrace()      │  │   │   │
│  │  │  └───────────┘  │  │    │  │  │ • verify()        │  │   │   │
│  │  └─────────────────┘  │    │  │  └────────────────────┘  │   │   │
│  └───────────────────────┘    │  └──────────────────────────┘   │   │
│                               │                                  │   │
│                               │  ┌──────────────────────────┐   │   │
│                               │  │  VeChain Tools           │   │   │
│                               │  │  • VeChain Sync (Wallet) │   │   │
│                               │  │  • VeChain Stats         │   │   │
│                               │  │  • VeChain Explorer      │   │   │
│                               │  └──────────────────────────┘   │   │
│                               └──────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────┘
```

### 6.2. Smart Contract Architecture

| Smart Contract | Ngôn ngữ | Chức năng | Dữ liệu lưu trữ |
|---------------|----------|-----------|------------------|
| **FarmingSeasonContract** | Solidity 0.8+ | Ghi nhận thông tin mùa vụ khi tạo mới | farmId, seasonName, productType, variety, area, startDate |
| **FarmingProcessContract** | Solidity 0.8+ | Ghi nhận từng bước quy trình trong mùa vụ | seasonId, processType, date, materials (hash), imageHash |
| **ExportContract** | Solidity 0.8+ | Ghi nhận thông tin xuất kho và tạo QR | seasonId, quantity, exportDate, qrHash |
| **TraceabilityContract** | Solidity 0.8+ | Tổng hợp truy xuất nguồn gốc, xác thực QR | traceId, seasonRef, processRefs[], exportRef, farmInfo |

#### 6.2.1. Smart Contract Security Patterns

Tất cả Smart Contract trong hệ thống BICAP **bắt buộc** áp dụng các security patterns sau:

| Security Pattern | Thư viện / Cơ chế | Mục đích | Áp dụng cho |
|-----------------|-------------------|---------|-------------|
| **Access Control** | OpenZeppelin `AccessControl` | Phân quyền on-chain: chỉ system account được ghi dữ liệu | Tất cả contracts |
| **Reentrancy Guard** | OpenZeppelin `ReentrancyGuard` | Chống reentrancy attack cho các hàm thay đổi state | Tất cả write functions |
| **Pausable** | OpenZeppelin `Pausable` | Emergency stop — Admin có thể tạm dừng contract khi phát hiện lỗ hổng | Tất cả contracts |
| **Event Emission** | Solidity `event` + `emit` | Phát sự kiện cho off-chain indexing và audit trail | Tất cả write functions |
| **Input Validation** | `require()` / `custom errors` | Validate đầu vào trước khi lưu state | Tất cả public functions |
| **Integer Safety** | Solidity 0.8+ built-in | Tự động revert khi overflow/underflow | Mặc định (Solidity 0.8+) |

**Roles on-chain:**

| Role | Mô tả | Quyền |
|------|-------|-------|
| `DEFAULT_ADMIN_ROLE` | Quản trị contract | Cấp/thu hồi roles, pause/unpause |
| `SYSTEM_WRITER_ROLE` | Backend system account | Ghi dữ liệu mùa vụ, quy trình, xuất kho |
| `UPGRADER_ROLE` | Admin chuyên deploy | Nâng cấp contract qua proxy |

**Events bắt buộc:**

```solidity
// FarmingSeasonContract
event SeasonCreated(bytes32 indexed farmId, bytes32 indexed seasonId, string seasonName, uint256 startDate);
event SeasonStatusUpdated(bytes32 indexed seasonId, uint8 newStatus);

// FarmingProcessContract
event ProcessAdded(bytes32 indexed seasonId, bytes32 indexed processId, string processType, uint256 executionDate);

// ExportContract
event ExportRecorded(bytes32 indexed seasonId, bytes32 indexed exportId, uint256 quantity, bytes32 qrHash);

// TraceabilityContract
event TraceVerified(bytes32 indexed traceId, address verifier, uint256 timestamp);

// Tất cả contracts
event ContractPaused(address indexed by, uint256 timestamp);
event ContractUnpaused(address indexed by, uint256 timestamp);
```

**Smart Contract Audit Process:**

| Bước | Mô tả | Công cụ / Phương pháp |
|------|-------|-----------------------|
| 1 | Static analysis | Slither, Mythril |
| 2 | Manual code review | Checklist theo SWC Registry |
| 3 | Unit test coverage ≥ 95% | Hardhat + Chai |
| 4 | Fuzz testing | Echidna hoặc Foundry Fuzz |
| 5 | Internal team review | Cross-review bởi ≥ 2 developers |
| 6 | Pre-mainnet freeze | Code freeze 1 tuần trước deploy mainnet |

#### 6.2.2. Smart Contract Upgradeability (UUPS Proxy Pattern)

Hệ thống BICAP sử dụng **UUPS (Universal Upgradeable Proxy Standard)** cho tất cả Smart Contract, cho phép nâng cấp logic mà không mất dữ liệu và không thay đổi địa chỉ contract.

```
┌──────────────────────────────────────────────────────────────┐
│                   UUPS PROXY PATTERN                         │
│                                                              │
│  ┌─────────────────────┐      ┌──────────────────────────┐  │
│  │    ERC1967 Proxy     │      │   Implementation v1       │  │
│  │                     │      │   (FarmingSeason Logic)   │  │
│  │  • Stores state     │─────►│   • createSeason()       │  │
│  │  • Delegates calls  │      │   • addProcess()         │  │
│  │  • Fixed address    │      │   • _authorizeUpgrade()  │  │
│  └─────────────────────┘      └──────────────────────────┘  │
│           │                                                  │
│           │   Upgrade (chỉ UPGRADER_ROLE)                    │
│           │                                                  │
│           │                   ┌──────────────────────────┐  │
│           └──────────────────►│   Implementation v2       │  │
│                               │   (Logic cập nhật)        │  │
│                               │   • createSeason() v2    │  │
│                               │   • newFunction()        │  │
│                               └──────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

**Lý do chọn UUPS thay vì Transparent Proxy:**
- Gas cost thấp hơn (upgrade logic nằm trong implementation, không phải proxy)
- Phù hợp với VeChainThor (tối ưu VTHO)
- Dễ quản lý hơn cho team nhỏ

**Quy trình upgrade:**
1. Develop & test implementation mới trên VeChain Solo
2. Deploy implementation mới lên Testnet → kiểm thử tích hợp
3. Code review + audit bởi ≥ 2 team members
4. Admin (có `UPGRADER_ROLE`) gọi `upgradeToAndCall()` trên Testnet

5. UAT trên Testnet ≥ 48 giờ
6. Admin thực hiện upgrade trên Mainnet
7. Verify trên VeChain Explorer

### 6.3. Chiến lược On-chain vs Off-chain

| Dữ liệu | Lưu trữ | Lý do |
|---------|---------|-------|
| Thông tin mùa vụ (tên, giống, diện tích, ngày) | **On-chain** (VeChainThor) | Cần tính bất biến, minh bạch cho truy xuất nguồn gốc |
| Quy trình sản xuất (bón phân, phun thuốc, thu hoạch) | **On-chain** (VeChainThor) | Nhật ký sản xuất bất biến |
| Thông tin xuất kho + metadata QR | **On-chain** (VeChainThor) | Xác minh nguồn gốc không thể giả mạo |
| Hình ảnh minh chứng | **Off-chain** (Cloud Storage) — Hash on-chain | File lớn không phù hợp lưu on-chain; hash đảm bảo tính toàn vẹn |
| Dữ liệu IoT (nhiệt độ, độ ẩm, pH) | **Off-chain** (MySQL + Redis) — Tổng hợp hash on-chain | Dữ liệu khối lượng lớn, tần suất cao; tổng hợp theo ngày rồi hash |
| Thông tin user, đơn hàng, thanh toán | **Off-chain** (MySQL) | Dữ liệu vận hành, cần CRUD thường xuyên |

### 6.4. Luồng ghi dữ liệu Blockchain

```
                       ┌──────────────────────────┐
                       │   Farm Manager Action     │
                       │   (Tạo mùa vụ / Cập nhật │
                       │    quy trình / Xuất kho)  │
                       └─────────────┬────────────┘
                                     │
                                     ▼
                       ┌──────────────────────────┐
                       │  ① Validate Business     │
                       │     Rules                 │
                       │  - Farm APPROVED?         │
                       │  - Service package valid? │
                       │  - Data valid?            │
                       └─────────────┬────────────┘
                                     │ Pass
                                     ▼
                       ┌──────────────────────────┐
                       │  ② Save to MySQL         │
                       │  (Transactional)          │
                       │  - Create entity          │
                       │  - Status: PENDING_BC     │
                       └─────────────┬────────────┘
                                     │
                                     ▼
                       ┌──────────────────────────┐
                       │  ③ Encode Data for       │
                       │     Smart Contract        │
                       │  - ABI encoding           │
                       │  - Hash large data (imgs) │
                       └─────────────┬────────────┘
                                     │
                                     ▼
                       ┌──────────────────────────┐
                       │  ④ Send Transaction to   │
                       │     VeChainThor           │
                       │  - Sign with system key   │
                       │  - Submit via Thor API     │
                       └─────────────┬────────────┘
                                     │
                          ┌──────────┴──────────┐
                          │                     │
                     Success                  Failure
                          │                     │
                          ▼                     ▼
              ┌──────────────────┐   ┌──────────────────┐
              │ ⑤ Save TX hash  │   │ ⑤ Retry 3 times │
              │    to MySQL      │   │                  │
              │ Status: CONFIRMED│   │  Still failed?   │
              └──────────────────┘   └────────┬─────────┘
                                              │ Yes
                                              ▼
                                   ┌──────────────────┐
                                   │ ⑥ Queue for      │
                                   │    async retry    │
                                   │ Status: PENDING_BC│
                                   │ Notify user       │
                                   └──────────────────┘
```

### 6.5. Quản lý Nonce và Giao dịch Đồng thời

Khi nhiều giao dịch blockchain được gửi đồng thời (VD: nhiều Farm Manager tạo mùa vụ cùng lúc), cần chiến lược quản lý **nonce** để tránh conflict:

```
┌───────────────────────────────────────────────────────────────┐
│                 NONCE MANAGEMENT STRATEGY                      │
│                                                               │
│  ┌─────────────┐     ┌─────────────────────┐                 │
│  │ TX Request 1│────►│                     │                 │
│  └─────────────┘     │   NONCE MANAGER     │                 │
│  ┌─────────────┐     │   (Redis-based)     │    ┌─────────┐  │
│  │ TX Request 2│────►│                     │───►│VeChain  │  │
│  └─────────────┘     │  • Atomic nonce     │    │  Thor   │  │
│  ┌─────────────┐     │    increment        │    │ Network │  │
│  │ TX Request N│────►│  • Queue ordering   │    └─────────┘  │
│  └─────────────┘     │  • Gap detection    │                 │
│                      │  • Recovery logic    │                 │
│                      └─────────────────────┘                 │
└───────────────────────────────────────────────────────────────┘
```

| Cơ chế | Mô tả | Implementation |
|--------|-------|----------------|
| **Atomic Nonce Counter** | Redis `INCR` đảm bảo mỗi TX nhận nonce duy nhất | `INCR bc:nonce:{accountAddress}` |
| **Queue Ordering** | TX được xếp hàng theo nonce tăng dần trước khi gửi | Spring `@Async` + `PriorityQueue` |
| **Gap Detection** | Phát hiện nonce gap khi TX failed → fill gap trước khi gửi TX mới | Scheduled job kiểm tra mỗi 30 giây |
| **Stuck TX Recovery** | Gửi lại TX bị stuck (pending > 5 phút) với gas price cao hơn | Background worker |
| **Duplicate TX Prevention** | Kiểm tra idempotency key trong MySQL trước khi gửi TX | `BlockchainTransactions.idempotencyKey` |

**TX Confirmation Depth:**

| Loại giao dịch | Block confirmations | Lý do |
|----------------|--------------------|---------|
| Ghi mùa vụ / quy trình | ≥ 12 blocks (~2 phút) | Đảm bảo finality trên VeChainThor |
| Deploy Smart Contract | ≥ 30 blocks (~5 phút) | Critical operation, cần chắc chắn hơn |
| Đọc truy xuất nguồn gốc | 0 blocks (latest) | Read-only, không cần confirmation |

### 6.6. VeChainThor Dual-Token Model

| Token | Tên | Vai trò trong BICAP |
|-------|-----|---------------------|
| **VET** | VeChain Token | Token chính của mạng, tự động sinh VTHO theo thời gian |
| **VTHO** | VeThor Token | Token "gas" — trả phí cho mỗi giao dịch trên blockchain |

> **Quản lý gas:** Hệ thống BICAP cần duy trì đủ VTHO trong tài khoản hệ thống để thực hiện các giao dịch. Admin có thể monitor balance VTHO thông qua module Smart Contract Management.

### 6.7. Blockchain Node Failover Strategy

| Chiến lược | Mô tả | Implementation |
|-----------|-------|----------------|
| **Primary Node** | VeChainThor endpoint chính (Testnet/Mainnet) | `VECHAIN_NODE_URL_PRIMARY` |
| **Backup Node** | Node dự phòng, tự động chuyển khi primary timeout > 10 giây | `VECHAIN_NODE_URL_BACKUP` |
| **Health Check** | Kiểm tra node status mỗi 60 giây (`/blocks/best`) | Scheduled task |
| **Circuit Breaker** | Ngắt kết nối node lỗi sau 3 failures liên tiếp, thử lại sau 5 phút | Resilience4j CircuitBreaker |
| **Self-hosted Backup** | VeChain Thor node self-hosted (Docker) làm backup cuối cùng | `vechain/thor:latest` |

---

## 7. Kiến trúc dữ liệu (Data Architecture)

### 7.1. Entity-Relationship Diagram (ERD) — Tổng quan

```
┌──────────────┐     ┌──────────────┐     ┌───────────────┐
│    Users     │────┤    Roles     │────┤  Permissions  │
│              │ N:N │              │ N:N │               │
│ • id (PK)    │     │ • id (PK)    │     │ • id (PK)     │
│ • email      │     │ • name       │     │ • code        │
│ • password   │     │ • description│     │ • description │
│ • fullName   │     └──────────────┘     └───────────────┘
│ • phone      │
│ • status     │
│ • avatarUrl  │
└──────┬───────┘
       │ 1:N
       ▼
┌──────────────┐     ┌────────────────────┐
│    Farms     │────┤ FarmCertifications │
│              │ 1:N │                    │
│ • id (PK)    │     │ • id (PK)          │
│ • userId (FK)│     │ • farmId (FK)      │
│ • name       │     │ • type             │
│ • address    │     │ • fileUrl          │
│ • area       │     │ • expiryDate       │
│ • status     │     └────────────────────┘
│ • gpsLat     │
│ • gpsLng     │
└──────┬───────┘
       │ 1:N
       ▼
┌────────────────────┐     ┌─────────────────────┐
│  FarmingSeasons    │────┤  FarmingProcesses   │
│                    │ 1:N │                     │
│ • id (PK)          │     │ • id (PK)           │
│ • farmId (FK)      │     │ • seasonId (FK)     │
│ • name             │     │ • processType       │
│ • productType      │     │ • executionDate     │
│ • variety          │     │ • materials (JSON)  │
│ • area             │     │ • images            │
│ • startDate        │     │ • notes             │
│ • endDate          │     │ • txHash            │
│ • status           │     └─────────────────────┘
│ • txHash           │
└────────┬───────────┘
         │ 1:N
         ▼
┌──────────────────┐     ┌─────────────────┐     ┌──────────────┐
│    Products      │────┤     Orders      │────┤   Payments   │
│                  │ 1:N │                 │ 1:N │              │
│ • id (PK)        │     │ • id (PK)       │     │ • id (PK)    │
│ • seasonId (FK)  │     │ • productId(FK) │     │ • orderId(FK)│
│ • categoryId(FK) │     │ • retailerId(FK)│     │ • amount     │
│ • name           │     │ • quantity      │     │ • method     │
│ • description    │     │ • price         │     │ • status     │
│ • price          │     │ • status        │     │ • txRef      │
│ • quantity       │     │ • deliveryAddr  │     └──────────────┘
│ • qrCodeId (FK)  │     │ • depositRate   │
│ • status         │     └────────┬────────┘
└──────────────────┘              │ 1:1
                                  ▼
┌──────────────────┐     ┌─────────────────────┐
│   Shipments      │────┤ ShipmentTracking    │
│                  │ 1:N │                     │
│ • id (PK)        │     │ • id (PK)           │
│ • orderId (FK)   │     │ • shipmentId (FK)   │
│ • driverId (FK)  │     │ • status            │
│ • vehicleId (FK) │     │ • gpsLat            │
│ • status         │     │ • gpsLng            │
│ • pickupTime     │     │ • images            │
│ • deliveryTime   │     │ • notes             │
│ • route          │     │ • timestamp         │
└──────────────────┘     └─────────────────────┘

┌──────────────────┐     ┌──────────────────┐
│    Vehicles      │     │    Drivers       │
│                  │     │                  │
│ • id (PK)        │     │ • id (PK)        │
│ • licensePlate   │     │ • userId (FK)    │
│ • type           │     │ • citizenId      │
│ • capacity       │     │ • licenseNumber  │
│ • status         │     │ • vehicleId (FK) │
└──────────────────┘     │ • status         │
                         └──────────────────┘

┌──────────────────────────┐     ┌──────────────────┐
│  BlockchainTransactions  │     │     QRCodes      │
│                          │     │                  │
│ • id (PK)                │     │ • id (PK)        │
│ • entityType             │     │ • txId (FK)      │
│ • entityId               │     │ • qrImage        │
│ • txHash                 │     │ • traceUrl       │
│ • contractAddress        │     │ • seasonId (FK)  │
│ • status                 │     │ • createdAt      │
│ • retryCount             │     └──────────────────┘
│ • createdAt              │
└──────────────────────────┘

┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  Notifications   │     │     Reports      │     │   IoTData        │
│                  │     │                  │     │                  │
│ • id (PK)        │     │ • id (PK)        │     │ • id (PK)        │
│ • userId (FK)    │     │ • userId (FK)    │     │ • farmId (FK)    │
│ • type           │     │ • type           │     │ • temperature    │
│ • title          │     │ • title          │     │ • humidity       │
│ • content        │     │ • content        │     │ • ph             │
│ • channel        │     │ • attachments    │     │ • measuredAt     │
│ • isRead         │     │ • status         │     └──────────────────┘
│ • createdAt      │     │ • adminResponse  │
└──────────────────┘     └──────────────────┘

┌──────────────────────┐     ┌──────────────┐
│  ServicePackages     │     │ Subscriptions│
│                      │     │              │
│ • id (PK)            │     │ • id (PK)    │
│ • name               │     │ • farmId(FK) │
│ • description        │     │ • pkgId (FK) │
│ • price              │     │ • startDate  │
│ • duration           │     │ • endDate    │
│ • features (JSON)    │     │ • status     │
│ • status             │     └──────────────┘
└──────────────────────┘
```

### 7.2. Redis Cache Strategy

| Key Pattern | Giá trị | TTL | Mục đích |
|-------------|---------|-----|----------|
| `session:{token}` | User session data (JSON) | 15 phút | Quản lý session, giảm DB lookup |
| `user:{id}` | User profile data | 30 phút | Cache thông tin user thường truy cập |
| `farm:{id}` | Farm details | 1 giờ | Cache thông tin nông trại |
| `product:list:{page}:{hash(filters)}` | Danh sách sản phẩm (JSON) | 5 phút | Cache kết quả tìm kiếm sàn giao dịch |
| `product:{id}` | Chi tiết sản phẩm | 30 phút | Cache thông tin sản phẩm |
| `notification:{userId}:unread` | Số lượng unread | 1 phút | Badge count thông báo |
| `iot:{farmId}:latest` | Dữ liệu IoT mới nhất | 5 phút | Display real-time IoT data |
| `rate_limit:{ip}` | Request counter | 1 phút | Rate limiting per IP |
| `rate_limit:{userId}` | Request counter | 1 phút | Rate limiting per user |
| `bc:tx:{hash}` | Blockchain TX result | 24 giờ | Cache blockchain query results |

**Cache Invalidation Strategy:**
- **Write-through:** Khi cập nhật dữ liệu trong MySQL, đồng thời invalidate cache key tương ứng
- **TTL-based expiry:** Mỗi key có TTL phù hợp, tự động hết hạn
- **Event-driven invalidation:** Khi có sự kiện thay đổi dữ liệu (order status change), publish event để invalidate related cache

### 7.3. Chiến lược Indexing MySQL

| Bảng | Index | Cột | Mục đích |
|------|-------|-----|----------|
| Users | `idx_users_email` | email | Login lookup |
| Users | `idx_users_phone` | phone | Phone login |
| Farms | `idx_farms_status` | status | Lọc farm theo trạng thái |
| FarmingSeasons | `idx_seasons_farm_status` | farmId, status | Danh sách mùa vụ theo farm |
| Products | `idx_products_category_status` | categoryId, status | Tìm kiếm sản phẩm trên sàn |
| Products | `idx_products_price` | price | Sắp xếp/lọc theo giá |
| Orders | `idx_orders_retailer_status` | retailerId, status | Lịch sử đơn hàng retailer |
| Orders | `idx_orders_farm_status` | farmId (via product), status | Đơn hàng của farm |
| Shipments | `idx_shipments_driver_status` | driverId, status | Chuyến hàng của tài xế |
| Notifications | `idx_notif_user_read` | userId, isRead | Thông báo chưa đọc |
| IoTData | `idx_iot_farm_time` | farmId, measuredAt | Truy vấn dữ liệu IoT theo thời gian |

---

## 8. Kiến trúc tích hợp bên ngoài (Integration Architecture)

### 8.1. Tổng quan tích hợp

```
┌──────────────────────────────────────────────────────────────────────┐
│                     SPRING BOOT API (Integration Hub)                │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────────┐│
│  │                    Integration Services                         ││
│  │                                                                 ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ││
│  │  │Blockchain│  │ Payment  │  │  Email   │  │     FCM      │  ││
│  │  │ Service  │  │ Service  │  │ Service  │  │   Service    │  ││
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘  ││
│  │       │              │              │               │          ││
│  │  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐  ┌──────┴───────┐  ││
│  │  │ IoT      │  │  File    │  │  QR Code │  │  Scheduler   │  ││
│  │  │ Service  │  │ Storage  │  │ Generator│  │  Service     │  ││
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────────────┘  ││
│  └───────┼──────────────┼──────────────┼─────────────────────────┘│
│          │              │              │                          │
└──────────┼──────────────┼──────────────┼──────────────────────────┘
           │              │              │
    ┌──────┴──────┐ ┌─────┴──────┐ ┌────┴────────┐
    │  MQTT       │ │ Cloud      │ │ ZXing / QR  │
    │  Broker     │ │ Storage    │ │ Library     │
    │ (IoT)      │ │ (AWS S3 /  │ │             │
    │             │ │ GCS)       │ │             │
    └─────────────┘ └────────────┘ └─────────────┘
```

### 8.2. Payment Gateway Integration

```
  Retailer / FM          Backend API           Payment Gateway        Database
       │                     │                       │                    │
       │  Initiate Payment   │                       │                    │
       │────────────────────►│                       │                    │
       │                     │                       │                    │
       │                     │  Create payment order │                    │
       │                     │───────────────────────┼───────────────────►│
       │                     │                       │     (PENDING)      │
       │                     │                       │                    │
       │                     │  Create payment URL   │                    │
       │                     │──────────────────────►│                    │
       │                     │  ◄── payment URL ─────│                    │
       │                     │                       │                    │
       │  ◄── Redirect to ──│                       │                    │
       │     payment page    │                       │                    │
       │                     │                       │                    │
       │  User completes     │                       │                    │
       │  payment on gateway │                       │                    │
       │                     │                       │                    │
       │                     │  Callback (webhook)   │                    │
       │                     │◄──────────────────────│                    │
       │                     │                       │                    │
       │                     │  Verify signature     │                    │
       │                     │  Update payment status│                    │
       │                     │───────────────────────┼───────────────────►│
       │                     │                       │   (SUCCESS/FAILED) │
       │                     │                       │                    │
       │                     │  Trigger business     │                    │
       │                     │  logic (activate pkg  │                    │
       │                     │  or confirm order)    │                    │
       │                     │                       │                    │
       │  ◄── Redirect back  │                       │                    │
       │     + notification  │                       │                    │
```

**Payment Idempotency & Verification:**
- **Idempotency Key:** Mọi yêu cầu thanh toán gửi kèm header `X-Idempotency-Key: {UUID}` tránh trùng lặp giao dịch.
- **Signature Verification:** Xác thực HMAC-SHA256 signature trên tất cả callback/webhook từ Payment Gateway trước khi cập nhật DB.
- **PCI DSS Scope Reduction:** Sử dụng Hosted Payment Page (Redirect flow) — giảm scope compliance xuống SAQ A.

### 8.3. IoT Data Pipeline

```
  IoT Sensors          MQTT Broker        IoT Service          Redis         MySQL
     │                     │                  │                   │             │
     │  Publish data       │                  │                   │             │
     │  (temp, humidity,   │                  │                   │             │
     │   pH)               │                  │                   │             │
     │────────────────────►│                  │                   │             │
     │                     │  Subscribe topic │                   │             │
     │                     │─────────────────►│                   │             │
     │                     │                  │                   │             │
     │                     │                  │  ① Parse & validate            │
     │                     │                  │                   │             │
     │                     │                  │  ② Cache latest   │             │
     │                     │                  │─────────────────►│             │
     │                     │                  │  iot:{farmId}:latest            │
     │                     │                  │                   │             │
     │                     │                  │  ③ Store raw data │             │
     │                     │                  │────────────────────────────────►│
     │                     │                  │                   │  IoTData    │
     │                     │                  │                   │             │
     │                     │                  │  ④ Check thresholds             │
     │                     │                  │  (Temp > 40°C ?                │
     │                     │                  │   Humidity < 30% ?             │
     │                     │                  │   pH outside 5.5-7.5 ?)        │
     │                     │                  │                   │             │
     │                     │                  │  ⑤ If alert:      │             │
     │                     │                  │  → NotificationService          │
     │                     │                  │  → Push + In-app   │             │
```

**MQTT Topic Structure:**
```
bicap/iot/{farmId}/temperature    → Temperature readings
bicap/iot/{farmId}/humidity       → Humidity readings
bicap/iot/{farmId}/ph             → pH readings
bicap/iot/{farmId}/all            → Combined sensor data (JSON)
```

**IoT / MQTT Security Requirements:**
- **Protocol:** Bắt buộc MQTTS trên TLS (port 8883) — **cấm plaintext (port 1883)** ở môi trường Staging/Production.
- **Authentication:** Mỗi thiết bị IoT / Gateway bắt buộc dùng Username/Password độc lập hoặc TLS Client Certificate (X.509).
- **Access Control List (ACL):** Cấu hình ACL trên MQTT Broker sao cho thiết bị ở Farm A **chỉ được publish** vào topic `bicap/iot/{farmA}/*`, không được publish/subscribe topic của trang trại khác.
- **Data Tampering Protection:** Kiểm tra ngưỡng dữ liệu vật lý hợp lý (VD: nhiệt độ 0-60°C, pH 0-14); đánh dấu `IS_SUSPICIOUS` nếu dữ liệu bất thường.

---

## 9. Kiến trúc triển khai (Deployment Architecture)

### 9.1. Deployment Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      CLOUD INFRASTRUCTURE (AWS / Google Cloud)               │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        LOAD BALANCER (ALB / Cloud LB)                │   │
│  │                    ┌──────────┐                                      │   │
│  │        ┌──────────►│  HTTPS   │◄───────────┐                        │   │
│  │        │           │ :443     │             │                        │   │
│  │        │           └────┬─────┘             │                        │   │
│  │        │                │                   │                        │   │
│  └────────┼────────────────┼───────────────────┼────────────────────────┘   │
│           │                │                   │                            │
│  ┌────────┼────────────────┼───────────────────┼────────────────────────┐   │
│  │        │      DOCKER HOST / KUBERNETES CLUSTER                       │   │
│  │        │                │                   │                        │   │
│  │   ┌────┴──────────┐ ┌──┴───────────┐ ┌─────┴──────────┐            │   │
│  │   │  NGINX        │ │  NGINX       │ │  NGINX         │            │   │
│  │   │  (Web Static) │ │  (Reverse    │ │  (Web Static)  │            │   │
│  │   │  Container    │ │   Proxy)     │ │  Container     │            │   │
│  │   │               │ │  Container   │ │                │            │   │
│  │   │  Admin App    │ │              │ │  Farm/Retailer │            │   │
│  │   │  :80          │ │  → API :8080 │ │  /Shipping App │            │   │
│  │   └───────────────┘ │  → WS  :8080 │ │  :80           │            │   │
│  │                     └──────┬───────┘ └────────────────┘            │   │
│  │                            │                                       │   │
│  │                     ┌──────┴───────┐                               │   │
│  │                     │ Spring Boot  │  ← Có thể scale N instances  │   │
│  │                     │ API Container│                               │   │
│  │                     │              │                               │   │
│  │                     │  :8080       │                               │   │
│  │                     │  JDK 21      │                               │   │
│  │                     └──────┬───────┘                               │   │
│  │                            │                                       │   │
│  │              ┌─────────────┼──────────────┐                       │   │
│  │              │             │              │                       │   │
│  │       ┌──────┴──────┐ ┌───┴──────┐ ┌─────┴───────┐              │   │
│  │       │  MySQL      │ │  Redis   │ │  MQTT       │              │   │
│  │       │  Container  │ │  Container│ │  Broker     │              │   │
│  │       │             │ │          │ │  Container  │              │   │
│  │       │  :3306      │ │  :6379   │ │  :1883      │              │   │
│  │       │             │ │          │ │             │              │   │
│  │       │  Volume:    │ │  Volume: │ │             │              │   │
│  │       │  /data/mysql│ │  /data/  │ │             │              │   │
│  │       │             │ │  redis   │ │             │              │   │
│  │       └─────────────┘ └──────────┘ └─────────────┘              │   │
│  │                                                                   │   │
│  └───────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                     EXTERNAL CONNECTIONS                           │  │
│  │                                                                    │  │
│  │  ┌────────────────────┐  ┌──────────────────┐                     │  │
│  │  │  VeChainThor       │  │  Payment Gateway │                     │  │
│  │  │  (Testnet/Mainnet) │  │  (VNPay/MoMo)    │                     │  │
│  │  │  External Network  │  │  External API    │                     │  │
│  │  └────────────────────┘  └──────────────────┘                     │  │
│  │                                                                    │  │
│  │  ┌────────────────────┐  ┌──────────────────┐                     │  │
│  │  │  Firebase (FCM)    │  │  SendGrid /      │                     │  │
│  │  │  Push Notification │  │  SMTP Server     │                     │  │
│  │  └────────────────────┘  └──────────────────┘                     │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                     MOBILE DISTRIBUTION                            │  │
│  │                                                                    │  │
│  │  ┌──────────────────┐  ┌──────────────────┐                       │  │
│  │  │  Google Play     │  │  Apple App Store │                       │  │
│  │  │  Store           │  │                  │                       │  │
│  │  │  (Ship Driver)   │  │  (Ship Driver)   │                       │  │
│  │  │  (Guest App)     │  │  (Guest App)     │                       │  │
│  │  └──────────────────┘  └──────────────────┘                       │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

### 9.2. Docker Compose Configuration Overview

| Service | Image | Port | Depends On | Volumes |
|---------|-------|------|------------|---------|
| `nginx-proxy` | nginx:latest | 80, 443 | api, web-apps | nginx.conf, SSL certs |
| `api` | bicap-api:latest | 8080 | mysql, redis | logs |
| `mysql` | mysql:5.7.41 | 3306 | — | /data/mysql |
| `redis` | redis:8.6 | 6379 | — | /data/redis |
| `mqtt` | eclipse-mosquitto:latest | 1883, 9001 | — | mosquitto.conf |
| `admin-web` | bicap-admin:latest | 3001 | api | — |
| `farm-web` | bicap-farm:latest | 3002 | api | — |
| `retailer-web` | bicap-retailer:latest | 3003 | api | — |
| `shipping-web` | bicap-shipping:latest | 3004 | api | — |

### 9.3. CI/CD Pipeline

```
  Developer        Git Repository        CI/CD Pipeline          Cloud
     │                  │                     │                    │
     │  git push        │                     │                    │
     │─────────────────►│                     │                    │
     │                  │  Webhook trigger     │                    │
     │                  │────────────────────►│                    │
     │                  │                     │                    │
     │                  │                     │  ① Checkout code   │
     │                  │                     │                    │
     │                  │                     │  ② Build & Test    │
     │                  │                     │  - mvn clean test  │
     │                  │                     │  - npm run test    │
     │                  │                     │  - SonarQube scan  │
     │                  │                     │                    │
     │                  │                     │  ③ Build Docker    │
     │                  │                     │     images         │
     │                  │                     │                    │
     │                  │                     │  ④ Push to         │
     │                  │                     │     Container      │
     │                  │                     │     Registry       │
     │                  │                     │                    │
     │                  │                     │  ⑤ Deploy to       │
     │                  │                     │     Staging        │
     │                  │                     │────────────────────►│
     │                  │                     │                    │
     │                  │                     │  ⑥ Run E2E tests   │
     │                  │                     │                    │
     │                  │                     │  ⑦ Manual approval │
     │                  │                     │     (Production)   │
     │                  │                     │                    │
     │                  │                     │  ⑧ Deploy to       │
     │                  │                     │     Production     │
     │                  │                     │────────────────────►│
```

### 9.4. Môi trường triển khai

| Môi trường | Mục đích | VeChain Network | Database |
|-----------|---------|----------------|----------|
| **Development (Local)** | Phát triển cá nhân | VeChain Solo (local) | MySQL local + Redis local |
| **Testing / QA** | Kiểm thử chức năng & tích hợp | VeChain Testnet | MySQL test instance + Redis test |
| **Staging** | UAT, kiểm thử hiệu năng | VeChain Testnet | MySQL staging + Redis staging |
| **Production** | Vận hành thực tế | VeChain Mainnet | MySQL production (HA) + Redis cluster |

---

## 10. Mô hình bảo mật (Security Architecture)

### 10.1. Tổng quan bảo mật

```
┌───────────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                                 │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Layer 1: NETWORK SECURITY                                 │   │
│  │  • HTTPS/TLS 1.2+ cho toàn bộ Web & API traffic            │   │
│  │  • MQTTS/TLS 1.3 (port 8883) cho IoT sensors               │   │
│  │  • Firewall rules (Security Groups, IP Whitelisting)       │   │
│  │  • Rate limiting (Redis-based: 60 req/min per IP)          │   │
│  │  • DDoS protection (Cloudflare / AWS Shield)               │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Layer 2: APPLICATION SECURITY                              │   │
│  │  • JWT Authentication (HS512, Access 15min, Refresh 7d)    │   │
│  │  • Token Revocation: Redis Blacklist cho Logout / Lockout  │   │
│  │  • Role-Based Access Control (RBAC 7 roles)                │   │
│  │  • Input validation (@Valid, OWASP Sanitizer)               │   │
│  │  • OWASP Top 10 protection:                                │   │
│  │    - SQL Injection → Parameterized queries (JPA/Hibernate)  │   │
│  │    - XSS → Output encoding, CSP headers, Content-Security  │   │
│  │    - CSRF → SameSite=Strict cookies, httpOnly storage      │   │
│  │    - Brute Force → Lockout 30 min after 5 failed attempts  │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Layer 3: DATA SECURITY & PRIVACY                           │   │
│  │  • Password hashing: bcrypt (cost factor 12)               │   │
│  │  • Sensitive data encryption at rest (AES-256 GCM)         │   │
│  │  • Redis Security: AUTH password + TLS encryption          │   │
│  │  • Audit Logging: Ghi vết toàn bộ hành vi Admin / Sensitive  │   │
│  │  • File upload validation (type white-list, max 10MB)       │   │
│  └────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Layer 4: BLOCKCHAIN & KEY SECURITY                         │   │
│  │  • VeChain Private Key management via HashiCorp Vault      │   │
│  │  • Isolated Transaction Signing Service                    │   │
│  │  • Multi-signature cho Smart Contract Upgrades             │   │
│  │  • Smart Contract Security Patterns (OpenZeppelin)         │   │
│  │  • Immutable data integrity on VeChainThor                 │   │
│  └────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────┘
```

### 10.2. Ma trận phân quyền RBAC

| Tài nguyên / Hành động | SUPER_ADMIN | ADMIN | FARM_MANAGER | RETAILER | SHIPPING_MANAGER | SHIPPING_DRIVER | GUEST |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **Admin Accounts** | CRUD | Read | — | — | — | — | — |
| **Farm Registrations** | Approve/Reject | Approve/Reject | Create/Read | — | — | — | — |
| **Farm Details** | Read/Manage | Read/Manage | CRUD (own) | — | — | — | — |
| **Farming Seasons** | Read | Read | CRUD (own) | Read | — | — | Read |
| **Products (Trading)** | Read/Manage | Read/Manage | Create/Read (own) | Read | — | — | Read |
| **Orders** | Read | Read | Accept/Reject (own) | CRUD (own) | Read | — | — |
| **Shipments** | Read | Read | Read (own) | Read (own) | CRUD | Read/Update (assigned) | — |
| **Vehicles** | Read | Read | — | — | CRUD | Read (assigned) | — |
| **Drivers** | Read | Read | — | — | CRUD | Read (self) | — |
| **Smart Contracts** | CRUD | Read | — | — | — | — | — |
| **Reports** | Read All | Read All | Create (own) | Create (own) | Create (own) | Create (own) | — |
| **Notifications** | Send/Read | Send/Read | Read (own) | Read (own) | Send/Read | Read (own) | Read (public) |
| **IoT Data** | Read All | Read All | Read (own farm) | — | — | — | — |
| **Articles** | CRUD | CRUD | Read | Read | Read | Read | Read |
| **Service Packages** | CRUD | CRUD | Read/Purchase | — | — | — | — |

### 10.3. Chiến lược quản lý API Key & Secrets

| Secret | Nơi lưu trữ | Rotation Policy |
|--------|-------------|-----------------|
| JWT Signing Key | HashiCorp Vault / AWS Secrets Manager | Mỗi 90 ngày |
| MySQL Credentials | HashiCorp Vault / AWS Secrets Manager | Mỗi 90 ngày |
| Redis Password | HashiCorp Vault / AWS Secrets Manager | Mỗi 90 ngày |
| VeChain Private Key | HashiCorp Vault (Transit Engine) | Không rotate (Transfer ownership nếu bị hớ) |
| Payment Gateway API Key | HashiCorp Vault / AWS Secrets Manager | Theo yêu cầu provider |
| Firebase Service Account | HashiCorp Vault / AWS Secrets Manager | Mỗi 180 ngày |
| SendGrid API Key | HashiCorp Vault / AWS Secrets Manager | Mỗi 90 ngày |

### 10.4. Quản lý VeChain Private Key & Server-Side Signing

Bảo vệ Private Key của tài khoản hệ thống (dùng để ký các giao dịch lên VeChainThor) là ưu tiên bảo mật **hàng đầu**:

```
┌─────────────────────────────────────────────────────────────────────┐
│               ISOLATED SIGNING SERVICE ARCHITECTURE                 │
│                                                                     │
│  ┌───────────────────┐    REST/gRPC    ┌─────────────────────────┐  │
│  │                   │  (Internal TLS) │  Signing Service        │  │
│  │  BICAP API        │────────────────►│  (Isolated Container)   │  │
│  │  (Business Logic) │                 │                         │  │
│  │                   │◄────────────────│  • Fetch key from Vault │  │
│  └───────────────────┘    Signed TX    │  • Sign transaction     │  │
│                                        │  • Zero-log private key │  │
│                                        └────────────┬────────────┘  │
│                                                     │               │
│                                                     ▼               │
│                                        ┌─────────────────────────┐  │
│                                        │ HashiCorp Vault         │  │
│                                        │ (Transit Engine / PKI)  │  │
│                                        └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

| Tiêu chí | Giải pháp |
|----------|-----------|
| **Ký giao dịch** | Thực hiện trong **Signing Service** độc lập, không ghi log private key |
| **Lưu trữ Key** | Lưu trong **HashiCorp Vault** (hoặc AWS KMS/GCP KMS ở Production) |
| **Cold Backup** | Backup Mnemonic Phrase ở dạng encrypted đĩa cứng ngoại tuyến (Cold Storage) |
| **Quản trị Owner** | `UPGRADER_ROLE` và Contract Ownership sử dụng **Multi-Sig Wallet** (≥ 2/3 signatures) |
| **Giám sát số dư** | Tự động cảnh báo Slack/Email khi VTHO balance < 1,000 VTHO |

### 10.5. Nhật ký Kiểm toán (Audit Logging)

Hệ thống ghi nhận **Audit Log** không thể chỉnh sửa cho tất cả các thao tác quan trọng:

| Đối tượng | Hành vi bị ghi log | Dữ liệu lưu trữ |
|-----------|-------------------|------------------|
| **Admin** | Duyệt/Từ chối trang trại, thay đổi roles, deploy contract | UserID, Action, IP, Timestamp, Changes (JSON) |
| **Farm Manager** | Tạo mùa vụ, cập nhật quy trình, xuất kho | UserID, Action, SeasonID, Hash, Timestamp |
| **System** | Blockchain TX failure, IoT Alert, Security Lockout | EventType, Details, StackTrace, Timestamp |

---

## 11. Đặc tả hiệu năng và khả năng mở rộng (Performance & Scalability)

### 11.1. Yêu cầu hiệu năng (Performance Targets)

| Metric | Target | Cách đạt được |
|--------|--------|--------------|
| **API Response Time** | < 2 giây (95th percentile) | Redis cache, DB indexing, connection pooling |
| **Concurrent Users** | ≥ 500 | Horizontal scaling, load balancing |
| **Blockchain TX/min** | ≥ 100 | Async processing, TX queue, batch operations |
| **Cache Hit Rate** | ≥ 80% | Smart caching strategy, appropriate TTLs |
| **System Uptime** | ≥ 99.5% | Health checks, auto-restart, redundancy |
| **Page Load Time** | < 3 giây | CDN, code splitting, lazy loading |
| **IoT Data Latency** | < 5 giây | MQTT + Redis cache pipeline |

### 11.2. Chiến lược mở rộng (Scaling Strategy)

```
                     ┌───────────────────────────────────┐
                     │       SCALING STRATEGIES           │
                     └───────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────┐
    │  HORIZONTAL SCALING (Scale Out)                              │
    │                                                             │
    │  ┌─────────────────────────────────────────────────────┐   │
    │  │  API Instances                                       │   │
    │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐             │   │
    │  │  │  API-1  │  │  API-2  │  │  API-N  │  ◄── Auto  │   │
    │  │  │ (8080)  │  │ (8080)  │  │ (8080)  │     Scale  │   │
    │  │  └─────────┘  └─────────┘  └─────────┘             │   │
    │  │        ▲              ▲              ▲               │   │
    │  │        └──────────────┼──────────────┘               │   │
    │  │                       │                              │   │
    │  │              ┌────────┴────────┐                     │   │
    │  │              │  Load Balancer  │                     │   │
    │  │              │ (Round Robin)   │                     │   │
    │  │              └─────────────────┘                     │   │
    │  └─────────────────────────────────────────────────────┘   │
    │                                                             │
    │  Trigger: CPU > 70% hoặc RAM > 80%                         │
    │  Max Instances: 5                                           │
    └─────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────┐
    │  VERTICAL SCALING (Scale Up)                                │
    │                                                             │
    │  MySQL:  2 vCPU / 4GB RAM  →  4 vCPU / 8GB RAM            │
    │  Redis:  1 vCPU / 2GB RAM  →  2 vCPU / 4GB RAM            │
    │                                                             │
    │  Khi nào: Khi horizontal scaling API không đủ giải quyết   │
    │           bottleneck ở database layer                       │
    └─────────────────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────┐
    │  DATABASE OPTIMIZATION                                      │
    │                                                             │
    │  • MySQL Read Replica (nếu cần)                            │
    │  • Connection Pooling (HikariCP: max 20 connections)       │
    │  • Query Optimization (EXPLAIN ANALYZE)                    │
    │  • Redis Cluster (nếu cần)                                 │
    └─────────────────────────────────────────────────────────────┘
```

### 11.3. Connection Pool Configuration

| Pool | Công nghệ | Min Idle | Max Active | Timeout |
|------|-----------|----------|------------|---------|
| **MySQL** | HikariCP | 5 | 20 | 30 giây |
| **Redis** | Lettuce (Spring Data Redis) | 2 | 10 | 10 giây |
| **HTTP Client** (Blockchain, Payment) | Apache HttpClient | 2 | 10 | 15 giây |

---

## 12. Luồng nghiệp vụ chính (Key Business Flows)

### 12.1. Luồng truy xuất nguồn gốc (Farm-to-Table Traceability)

```
┌─────────────────────────────────────────────────────────────────────────┐
│              LUỒNG TRUY XUẤT NGUỒN GỐC (END-TO-END)                    │
│                                                                         │
│  ① FARM MANAGER: Tạo mùa vụ mới                                       │
│     └──► Spring Boot API ──► MySQL (save) ──► VeChainThor (ghi TX)     │
│                                                                         │
│  ② FARM MANAGER: Cập nhật quy trình (N lần)                            │
│     └──► Bón phân / Phun thuốc / Tưới nước / Thu hoạch                 │
│     └──► Mỗi lần: MySQL (save) + VeChainThor (ghi TX mới)             │
│                                                                         │
│  ③ FARM MANAGER: Xuất kho                                               │
│     └──► MySQL (save export) + VeChainThor (ghi TX xuất kho)           │
│                                                                         │
│  ④ HỆ THỐNG: Tạo QR Code                                               │
│     └──► Encode TX hash → QR Image                                     │
│     └──► URL: https://bicap.vn/trace/{hash}                            │
│                                                                         │
│  ⑤ FARM MANAGER: Đẩy sản phẩm lên sàn giao dịch                       │
│     └──► Sản phẩm + QR Code → Trading Floor                            │
│                                                                         │
│  ⑥ RETAILER: Tìm kiếm → Đặt mua → Đặt cọc                            │
│     └──► Order PENDING → ACCEPTED → DEPOSIT_PAID                       │
│                                                                         │
│  ⑦ SHIPPING MANAGER: Tạo lô vận chuyển                                 │
│     └──► Phân công Tài xế + Phương tiện                                │
│                                                                         │
│  ⑧ SHIPPING DRIVER: Quét QR tại farm → Lấy hàng → Vận chuyển → Giao   │
│     └──► QR scan → Verify on blockchain → Confirm pickup → Deliver     │
│                                                                         │
│  ⑨ RETAILER: Xác nhận nhận hàng → Hoàn tất đơn                        │
│     └──► Order DELIVERED → COMPLETED → Thanh toán 70% còn lại          │
│                                                                         │
│  ⑩ GUEST / CONSUMER: Quét QR Code sản phẩm                             │
│     └──► Xem toàn bộ lịch sử: quy trình, ngày, vật tư, hình ảnh      │
│     └──► Badge: "Đã xác minh trên Blockchain ✓"                        │
└─────────────────────────────────────────────────────────────────────────┘
```

### 12.2. Sơ đồ trạng thái đơn hàng (Order State Machine)

```
                                   ┌──────────────┐
                        ┌─────────►│   CANCELLED  │
                        │          └──────────────┘
                        │
                ┌───────┴──────┐
     Start ────►│   PENDING    │
                └───────┬──────┘
                        │
               ┌────────┴────────┐
               │                 │
        ┌──────▼──────┐   ┌─────▼────────┐
        │  ACCEPTED   │   │   REJECTED   │
        └──────┬──────┘   └──────────────┘
               │
               │ Retailer đặt cọc 30% (trong 24h)
               │
        ┌──────▼──────────┐
        │  DEPOSIT_PAID   │
        └──────┬──────────┘
               │
               │ Shipping Manager tạo shipment
               │
        ┌──────▼──────┐
        │  SHIPPING   │
        └──────┬──────┘
               │
               │ Tài xế giao hàng
               │
        ┌──────▼──────┐
        │  DELIVERED  │
        └──────┬──────┘
               │
               │ Retailer xác nhận (trong 48h)
               │ Thanh toán 70% còn lại
               │
        ┌──────▼──────┐
        │  COMPLETED  │
        └─────────────┘
```

### 12.3. Sơ đồ trạng thái lô vận chuyển (Shipment State Machine)

```
        ┌─────────┐
 Start ►│ CREATED │──────────────────────┐
        └────┬────┘                      │
             │                           │
             │ SM phân công driver       │ SM hủy (trước khi pickup)
             │                           │
    ┌────────▼────────┐          ┌───────▼─────┐
    │ PENDING_PICKUP  │          │  CANCELLED  │
    └────────┬────────┘          └─────────────┘
             │
             │ Driver quét QR + xác nhận lấy hàng
             │
    ┌────────▼────────┐
    │   PICKED_UP     │
    └────────┬────────┘
             │
             │ Driver bắt đầu vận chuyển
             │
    ┌────────▼────────┐
    │   IN_TRANSIT    │
    └────────┬────────┘
             │
             │ Driver xác nhận giao hàng (kèm ảnh + GPS)
             │
    ┌────────▼────────┐
    │   DELIVERED     │
    └─────────────────┘
```

---

## 13. Chiến lược kiểm thử kiến trúc (Architecture Testing Strategy)

### 13.1. Tổng quan các loại kiểm thử

| Loại kiểm thử | Mục tiêu | Công cụ | Phạm vi |
|---------------|---------|---------|---------|
| **Unit Testing** | Kiểm thử từng class/method riêng lẻ | JUnit 5, Mockito (Backend); Jest (Frontend) | Service layer, Utility classes |
| **Integration Testing** | Kiểm thử tương tác giữa các module | Spring Boot Test, Testcontainers | Controller ↔ Service ↔ Repository |
| **API Testing** | Kiểm thử REST API endpoints | Postman, RestAssured | Tất cả API endpoints (~130) |
| **E2E Testing** | Kiểm thử luồng nghiệp vụ đầu cuối | Cypress (Web), Detox (Mobile) | Luồng chính: đăng ký → mua bán → vận chuyển |
| **Performance Testing** | Kiểm thử hiệu năng và chịu tải | JMeter, k6 | Response < 2s, 500 concurrent users |
| **Security Testing** | Kiểm thử bảo mật | OWASP ZAP, manual testing | OWASP Top 10, RBAC, JWT |
| **Blockchain Testing** | Kiểm thử smart contract và tích hợp | Hardhat, VeChain Solo | Smart contract functions, TX flow |

### 13.2. Chiến lược kiểm thử Blockchain

| Bước | Mô tả | Môi trường |
|------|-------|-----------|
| 1 | Unit test Smart Contract bằng Hardhat/Truffle | Local (VeChain Solo) |
| 2 | Integration test: API → Blockchain Service → VeChain | VeChain Solo |
| 3 | E2E test: Tạo mùa vụ → Ghi blockchain → Đọc lại → Verify | VeChain Testnet |
| 4 | Load test: 100 TX/phút đồng thời | VeChain Testnet |
| 5 | Security audit: Review smart contract code | Pre-mainnet |

### 13.3. Test Coverage Targets

| Module | Unit Test | Integration Test |
|--------|-----------|-----------------|
| Auth | ≥ 90% | ≥ 80% |
| Farm Management | ≥ 85% | ≥ 75% |
| Order | ≥ 85% | ≥ 80% |
| Shipping | ≥ 85% | ≥ 75% |
| Blockchain Service | ≥ 80% | ≥ 70% |
| Payment Service | ≥ 80% | ≥ 70% |
| Notification | ≥ 75% | ≥ 60% |

---

## 14. Phụ lục (Appendices)

### 14.1. Bảng thuật ngữ (Glossary)

| Thuật ngữ | Định nghĩa |
|-----------|-----------|
| **Three-Tier Architecture** | Kiến trúc 3 tầng: Presentation, Application, Data |
| **SPA (Single Page Application)** | Ứng dụng web tải một trang duy nhất, render phía client |
| **JWT (JSON Web Token)** | Chuẩn token xác thực stateless, chứa thông tin user dạng JSON |
| **RBAC (Role-Based Access Control)** | Phân quyền truy cập dựa trên vai trò người dùng |
| **ORM (Object-Relational Mapping)** | Ánh xạ đối tượng Java với bảng database (Hibernate/JPA) |
| **Smart Contract** | Chương trình tự thực thi trên blockchain, viết bằng Solidity |
| **Transaction Hash (TX Hash)** | Mã định danh duy nhất của một giao dịch trên blockchain |
| **ABI (Application Binary Interface)** | Giao diện để tương tác với Smart Contract |
| **VTHO (VeThor Token)** | Token gas trên VeChainThor, dùng trả phí giao dịch |
| **PoA (Proof of Authority)** | Cơ chế đồng thuận của VeChainThor, nhanh và tiết kiệm năng lượng |
| **MQTT** | Message Queuing Telemetry Transport — giao thức nhẹ cho IoT |
| **WebSocket** | Giao thức truyền thông 2 chiều real-time qua HTTP |
| **STOMP** | Simple Text Oriented Messaging Protocol — protocol trên WebSocket |
| **CDN** | Content Delivery Network — phân phối nội dung tĩnh toàn cầu |
| **HikariCP** | Connection pool hiệu năng cao cho JDBC |

### 14.2. Ma trận truy xuất (Architecture → SRS → UR)

| Component | SRS Requirements | User Requirements |
|-----------|-----------------|------------------|
| Auth Module | SRS-FM-001, SRS-RT-001, SRS-API-001 | BICAP-7, BICAP-36, BICAP-72 |
| Farm Module | SRS-FM-002 → SRS-FM-021, SRS-API-002 | BICAP-8 → BICAP-27, BICAP-73 |
| Retailer Module | SRS-RT-001 → SRS-RT-018 | BICAP-36 → BICAP-53 |
| Order Module | SRS-FM-014, SRS-RT-007 → SRS-RT-009, SRS-API-004 | BICAP-20, BICAP-42 → BICAP-44, BICAP-75 |
| Shipping Module | SRS-SM-001 → SRS-SM-009, SRS-SD-001 → SRS-SD-006, SRS-API-005 | BICAP-54 → BICAP-68, BICAP-76 |
| Blockchain Module | SRS-ADM-005, SRS-FM-008 → SRS-FM-011, SRS-API-003 | BICAP-6, BICAP-14 → BICAP-17, BICAP-74 |
| Payment Module | SRS-FM-005, SRS-RT-008, SRS-API-007 | BICAP-11, BICAP-43, BICAP-78 |
| Notification Module | SRS-FM-018 → SRS-FM-020, SRS-RT-012 → SRS-RT-015, SRS-API-006 | BICAP-24 → BICAP-26, BICAP-47 → BICAP-50, BICAP-77 |
| Guest Module | SRS-GS-001 → SRS-GS-003 | BICAP-69 → BICAP-71 |
| Data Tier (MySQL + Redis) | SRS-API-008, NFR-005, NFR-006 | BICAP-79 |
| Security Cross-cutting | NFR-003, NFR-004, NFR-011, NFR-012 | BICAP-80, BICAP-81 |
| Infrastructure | NFR-001, NFR-013, NFR-014 | BICAP-82 → BICAP-90 |

### 14.3. Ước lượng tài nguyên hệ thống

| Tài nguyên | Development | Staging | Production |
|-----------|-------------|---------|------------|
| **API Server** | 1 instance (2 vCPU, 4GB RAM) | 1 instance (2 vCPU, 4GB RAM) | 2-5 instances (2 vCPU, 4GB RAM mỗi instance) |
| **MySQL** | 1 instance (2 vCPU, 4GB RAM, 20GB SSD) | 1 instance (2 vCPU, 4GB RAM, 50GB SSD) | 1 primary + 1 read replica (4 vCPU, 8GB RAM, 100GB SSD) |
| **Redis** | 1 instance (1 vCPU, 1GB RAM) | 1 instance (1 vCPU, 2GB RAM) | 1 instance (2 vCPU, 4GB RAM) |
| **MQTT Broker** | 1 instance (1 vCPU, 1GB RAM) | 1 instance (1 vCPU, 1GB RAM) | 1 instance (1 vCPU, 2GB RAM) |
| **Storage (S3/GCS)** | 5GB | 20GB | 100GB+ |

### 14.4. Rủi ro kiến trúc và giải pháp

| # | Rủi ro | Mức độ | Giải pháp |
|---|--------|--------|-----------|
| R1 | VeChainThor network downtime | Cao | Retry queue + async processing; dữ liệu đã lưu MySQL vẫn khả dụng |
| R2 | Hết VTHO (gas) cho giao dịch blockchain | Trung bình | Monitor VTHO balance; cảnh báo Admin khi balance thấp; dự trữ đủ VTHO |
| R3 | MySQL single point of failure | Trung bình | Backup định kỳ (daily); read replica cho production; Docker volume persistence |
| R4 | Quá tải đồng thời khi nhiều IoT sensor gửi dữ liệu | Trung bình | MQTT broker + batch processing; Redis buffer; queue overflow protection |
| R5 | Lỗ hổng Smart Contract sau khi deploy | Cao | Audit code trước deploy mainnet; sử dụng proxy pattern cho upgrade |
| R6 | Payment gateway timeout / failure | Trung bình | Retry mechanism; callback verification; manual reconciliation |
| R7 | JWT token bị đánh cắp | Trung bình | Short-lived access token (15min); HTTPS only; httpOnly cookie option |

### 14.5. Lịch sử phiên bản tài liệu

| Phiên bản | Ngày | Người thay đổi | Mô tả thay đổi |
|-----------|------|----------------|----------------|
| 1.0 | 24/07/2026 | Team BICAP | Phiên bản đầu tiên — thiết kế kiến trúc tổng thể dựa trên UR v2.0 và SRS v1.1 |

---

> **Ghi chú cuối:** Tài liệu Architecture Design này được xây dựng dựa trên User Requirements Document v2.0 và Software Requirement Specifications v1.1 của dự án BICAP. Mọi thay đổi về kiến trúc phải được cập nhật đồng bộ với các tài liệu liên quan và thông báo cho toàn bộ đội phát triển.
