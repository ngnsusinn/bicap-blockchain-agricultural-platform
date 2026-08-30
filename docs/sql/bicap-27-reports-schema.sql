-- =============================================================
-- BICAP-27: Reports gửi Admin — Database Migration
-- Table: reports (dùng chung cho Farm/Retailer/Shipping/Driver)
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- Table: reports
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `reports` (
  `id`               BIGINT AUTO_INCREMENT PRIMARY KEY,
  `reporter_id`      BIGINT        NOT NULL,   -- Người gửi (Farm Manager / Retailer / Shipping Mgr / Driver)
  `reporter_role`    VARCHAR(30)   NOT NULL,   -- Snapshot vai trò lúc gửi
  `type`             VARCHAR(20)   NOT NULL,   -- COMPLAINT, FEEDBACK, INCIDENT, OTHER
  `subject`          VARCHAR(200)  NOT NULL,
  `content`          VARCHAR(4000) NOT NULL,
  `related_order_id` BIGINT        NULL,       -- Đơn hàng liên quan (tuỳ chọn)
  `status`           VARCHAR(20)   NOT NULL DEFAULT 'OPEN',  -- OPEN, IN_PROGRESS, RESOLVED, REJECTED
  `admin_response`   VARCHAR(4000) NULL,
  `handled_by_id`    BIGINT        NULL,       -- Admin xử lý
  `handled_at`       DATETIME      NULL,
  `created_at`       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      NULL,
  CONSTRAINT `fk_reports_reporter` FOREIGN KEY (`reporter_id`)   REFERENCES `users`  (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reports_handler`  FOREIGN KEY (`handled_by_id`) REFERENCES `users`  (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_reports_order`    FOREIGN KEY (`related_order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Indexes (performance)
-- -----------------------------------------------------
CREATE INDEX `idx_reports_reporter` ON `reports` (`reporter_id`);
CREATE INDEX `idx_reports_status`   ON `reports` (`status`);

SET FOREIGN_KEY_CHECKS = 1;
