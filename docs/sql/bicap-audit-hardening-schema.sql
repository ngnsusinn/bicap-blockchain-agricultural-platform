-- =============================================================
-- BICAP — Audit hardening migration (C-1…C-4, F1, F2, F5)
--
-- Production runs with DDL_AUTO=validate, so the schema changes introduced by
-- the audit fixes MUST be applied before deploying the new backend build.
-- MySQL 8.x. Safe to re-run where noted.
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- F5: public educational content (guest "Kiến thức" screen)
--     Replaces the hard-coded mock arrays that lived in the browser.
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `educational_contents` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title`           VARCHAR(200)  NOT NULL,
  `summary`         VARCHAR(500)  NULL,
  `content`         TEXT          NOT NULL,
  `type`            VARCHAR(20)   NOT NULL DEFAULT 'ARTICLE',   -- ARTICLE | VIDEO
  `video_url`       VARCHAR(500)  NULL,
  `cover_image_url` VARCHAR(500)  NULL,
  `tags`            VARCHAR(300)  NULL,                          -- comma-separated
  `status`          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',      -- DRAFT | PUBLISHED
  `published_at`    DATETIME      NULL,
  `created_at`      DATETIME      NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- C-3: guests may only read platform-wide announcements.
--      `user_id` becomes nullable (NULL = no individual recipient) and
--      `is_system` flags the announcements that are public.
-- -----------------------------------------------------
ALTER TABLE `notifications`
  MODIFY COLUMN `user_id` BIGINT NULL;

-- MySQL has no "ADD COLUMN IF NOT EXISTS"; skip the error if it already ran.
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notifications' AND COLUMN_NAME = 'is_system'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `notifications` ADD COLUMN `is_system` TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE INDEX `idx_notifications_system` ON `notifications` (`is_system`);

-- -----------------------------------------------------
-- F1: record whether an export receipt was really broadcast to VeChainThor
--     (LIVE) or is a simulated development receipt (MOCK).
-- -----------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'season_exports' AND COLUMN_NAME = 'chain_mode'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `season_exports` ADD COLUMN `chain_mode` VARCHAR(10) NULL',
  'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------
-- F2: permissions granted directly to an admin account, on top of its role.
--     AdminService previously validated these codes and then discarded them.
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_permissions` (
  `user_id`       BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `permission_id`),
  CONSTRAINT `fk_user_permissions_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_permissions_permission`
    FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
