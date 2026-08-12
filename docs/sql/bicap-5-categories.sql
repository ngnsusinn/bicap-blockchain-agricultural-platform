-- BICAP-5: Product Monitoring & Category Management — Database Migration
-- Adds the `categories` table that `products.category_id` references, plus the FK.
-- Run this BEFORE creating the `products` table on a fresh MySQL 5.7.41 database
-- (or after, if products already exists without the FK constraint).

-- -----------------------------------------------------
-- Table: categories
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `categories` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL UNIQUE,
  `description` VARCHAR(500) NULL,
  `icon` VARCHAR(10) NULL COMMENT 'emoji icon for display',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Seed default categories (idempotent — duplicates ignored)
-- -----------------------------------------------------
INSERT IGNORE INTO `categories` (`name`, `description`, `icon`) VALUES
  ('Rau ăn lá', 'Các loại rau ăn lá, rau gia vị', '🥬'),
  ('Củ quả', 'Các loại củ, quả', '🥔'),
  ('Trái cây', 'Các loại trái cây', '🍎'),
  ('Lúa gạo', 'Lúa, gạo, các loại ngũ cốc', '🌾'),
  ('Thủy hải sản', 'Cá, tôm, các loại thủy sản', '🐟'),
  ('Thịt - Trứng - Sữa', 'Thịt gia súc, gia cầm, trứng, sữa', '🥩'),
  ('Khác', 'Các sản phẩm nông nghiệp khác', '📦');

-- -----------------------------------------------------
-- Link products → categories (add FK if products table already exists)
-- -----------------------------------------------------
-- If `products` already exists WITHOUT the FK, uncomment:
-- ALTER TABLE `products`
--   ADD CONSTRAINT `fk_products_category`
--   FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`);
