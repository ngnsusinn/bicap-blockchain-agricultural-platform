-- BICAP-18: Đăng ký đẩy sản phẩm lên sàn giao dịch (SRS-FM-012) — schema update.
-- Apply AFTER bicap-16-17-schema.sql (which creates `season_exports`) and once the
-- `products` table exists (see bicap-79-database-setup.md full DDL).

-- export_id: nguồn lô hàng xuất kho mà sản phẩm được đẩy lên sàn (SRS-FM-012 "Mã lô hàng")
ALTER TABLE products
    ADD COLUMN export_id BIGINT NULL AFTER season_id,
    -- images: JSON array of uploaded product image URLs (SRS-FM-012 "Hình ảnh sản phẩm", 1-10 ảnh)
    ADD COLUMN images TEXT NULL AFTER description;

-- FK để truy xuất QR / trace hash từ lô xuất kho tương ứng
ALTER TABLE products
    ADD CONSTRAINT fk_products_export
        FOREIGN KEY (export_id) REFERENCES season_exports (id) ON DELETE SET NULL;
