# Báo Cáo Hoàn Thành Ticket: BICAP-73 (Farming Season API)

## 1. Mục tiêu
Thực hiện yêu cầu từ ticket **BICAP-73**: "API quản lý mùa vụ (farming season): CRUD, cập nhật quy trình, lưu dữ liệu vào blockchain".
Nhằm mục đích cung cấp luồng nghiệp vụ hoàn chỉnh cho nhà nông (Farmer) từ khi bắt đầu gieo trồng một mùa vụ mới, ghi chép nhật ký canh tác hàng ngày, cho đến khi thu hoạch và xuất bán sản phẩm, với dữ liệu được băm (hash) và lưu trữ trên Blockchain (Traceability).

## 2. Chi tiết công việc đã thực hiện

### 2.1. Thiết kế và phát triển Database (Entities)
Đã tạo và ánh xạ 3 thực thể (Entity) chính vào cơ sở dữ liệu:
- `FarmingSeason`: Quản lý thông tin Mùa vụ (Tên, Loại sản phẩm, Giống, Diện tích, Ngày bắt đầu/kết thúc, Trạng thái: APPROVED, IN_PROGRESS, HARVESTED, CANCELLED).
- `FarmingProcess`: Nhật ký canh tác (Loại quy trình, Vật tư sử dụng, Hình ảnh đính kèm, Ghi chú).
- `Export`: Thông tin xuất bán sau thu hoạch (Ngày xuất, Số lượng, Nơi đến).

### 2.2. Xây dựng Data Transfer Objects (DTOs)
Sử dụng kiến trúc không Lombok (manual Builder pattern và getters/setters) để tạo ra 10 DTOs phục vụ việc giao tiếp API an toàn và validate dữ liệu chặt chẽ (sử dụng Jakarta Validation `@NotBlank`, `@NotNull`, `@Size`, `@DecimalMin`):
- `SeasonCreateRequest`, `SeasonUpdateRequest`, `SeasonStatusUpdateRequest`
- `SeasonResponse`, `SeasonDetailResponse`
- `ProcessCreateRequest`, `ProcessUpdateRequest`, `ProcessResponse`
- `ExportCreateRequest`, `ExportResponse`

### 2.3. Phát triển Business Logic (Services)
Xây dựng 3 Service xử lý nghiệp vụ với các ràng buộc khắt khe:
- **`SeasonService`**: 
  - Chỉ cho phép tạo mùa vụ khi Farm đã được duyệt (`APPROVED`).
  - Phải có gói dịch vụ (Subscription) đang hoạt động (`ACTIVE`).
  - Quản lý luồng trạng thái nghiêm ngặt: Chỉ được chuyển từ `IN_PROGRESS` sang `HARVESTED` hoặc `CANCELLED`.
- **`ProcessService`**: 
  - Chỉ được thêm quy trình canh tác khi mùa vụ đang ở trạng thái `IN_PROGRESS`.
- **`ExportService`**: 
  - Chỉ được tạo phiếu xuất kho khi mùa vụ đã hoàn tất thu hoạch (`HARVESTED`).
- Ràng buộc phân quyền: Các service đều kiểm tra chặt chẽ quyền sở hữu (Ownership) của người dùng hiện tại (Current User) đối với Nông trại (Farm).

### 2.4. Tích hợp Blockchain
Toàn bộ các tác vụ quan trọng làm thay đổi trạng thái của Mùa vụ đều được tự động gọi đến `BlockchainService` để ghi nhận dữ liệu lên mạng lưới VeChain:
- `recordSeason`: Ghi nhận khởi tạo và cập nhật trạng thái mùa vụ.
- `recordProcess`: Ghi nhận các bước chăm sóc (phân bón, tưới tiêu).
- `recordExport`: Ghi nhận dữ liệu xuất kho.
*Dữ liệu trả về (Transaction Hash - `txHash`) được lưu trữ tại các bản ghi tương ứng trong MySQL.*

### 2.5. Phát triển Controllers & API Endpoints
Đã cung cấp đầy đủ các RESTful APIs (được phân quyền `ROLE_FARMER`):
- `POST /api/farmers/seasons`: Tạo mùa vụ.
- `GET /api/farmers/seasons/{seasonId}`: Lấy chi tiết mùa vụ.
- `GET /api/farmers/farms/{farmId}/seasons`: Lấy danh sách mùa vụ của một nông trại.
- `PUT /api/farmers/seasons/{seasonId}`: Cập nhật thông tin mùa vụ.
- `PATCH /api/farmers/seasons/{seasonId}/status`: Cập nhật trạng thái mùa vụ.
- `POST /api/farmers/seasons/{seasonId}/processes`: Thêm quy trình canh tác.
- `GET /api/farmers/seasons/{seasonId}/processes`: Liệt kê quy trình của mùa vụ.
- `POST /api/farmers/seasons/{seasonId}/exports`: Tạo thông tin xuất bán.

### 2.6. Tự động hóa Dữ Liệu Khởi Tạo (Seeder)
- Nâng cấp `DatabaseSeeder` để tự động khởi tạo Gói Dịch Vụ (`ServicePackage`) và tự động cấp phát cho Nông trại mẫu (Subscription) nhằm giúp môi trường dev/test có thể vượt qua bước validate Subscription khi tạo Mùa vụ mới.

## 3. Kết quả & Đóng gói
- **Tính năng hoàn thiện 100%**: Code đã được test và build thành công.
- **Tương thích hoàn toàn**: Không phát sinh conflict trên nhánh `feature/BICAP-73-farming-season-api`.
- **Tài liệu kiểm thử**: Đã tạo và đính kèm bộ collection Postman hoàn chỉnh (`docs/BICAP-73-Farming-Season-API.postman_collection.json`) chứa toàn bộ quy trình test từ Login đến Export.
- Cấu hình tự động đồng bộ Schema CSDL (`DDL_AUTO=update`) đảm bảo các bảng mới (`exports`) tự động được tạo tại môi trường thật.

---
**Ticket:** BICAP-73
**Trạng thái:** Hoàn thành (Ready for Review)
