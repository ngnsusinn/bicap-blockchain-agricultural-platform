-- =============================================================
-- BICAP-76: Shipment Management API — Database Migration
-- Tables: vehicles, drivers, shipments, shipment_tracking
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- Table: vehicles
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `vehicles` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
  `license_plate` VARCHAR(20)  NOT NULL UNIQUE,
  `type`          VARCHAR(50)  NOT NULL,
  `capacity`      DOUBLE       NOT NULL,
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE, IN_USE, MAINTENANCE
  `created_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: drivers
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `drivers` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id`        BIGINT       NOT NULL UNIQUE,
  `citizen_id`     VARCHAR(20)  NOT NULL UNIQUE,
  `license_number` VARCHAR(30)  NOT NULL UNIQUE,
  `vehicle_id`     BIGINT       NULL,
  `status`         VARCHAR(20)  NOT NULL DEFAULT 'IDLE',  -- IDLE, ON_TRIP, OFFLINE
  `created_at`     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_drivers_user`    FOREIGN KEY (`user_id`)    REFERENCES `users`    (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_drivers_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: shipments
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `shipments` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id`      BIGINT       NOT NULL UNIQUE,
  `driver_id`     BIGINT       NULL,
  `vehicle_id`    BIGINT       NULL,
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'PICKING_UP',  -- PICKING_UP, IN_TRANSIT, DELIVERED, RETURNED
  `pickup_time`   DATETIME     NULL,
  `delivery_time` DATETIME     NULL,
  `route_summary` VARCHAR(500) NULL,
  `created_at`    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_shipments_order`   FOREIGN KEY (`order_id`)   REFERENCES `orders`   (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_shipments_driver`  FOREIGN KEY (`driver_id`)  REFERENCES `drivers`  (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_shipments_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: shipment_tracking
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `shipment_tracking` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
  `shipment_id` BIGINT       NOT NULL,
  `status`      VARCHAR(50)  NOT NULL,
  `gps_lat`     DOUBLE       NOT NULL,
  `gps_lng`     DOUBLE       NOT NULL,
  `images`      JSON         NULL,
  `notes`       TEXT         NULL,
  `timestamp`   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_tracking_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Indexes (performance)
-- -----------------------------------------------------
CREATE INDEX `idx_shipments_driver_status` ON `shipments` (`driver_id`, `status`);
CREATE INDEX `idx_shipments_status`        ON `shipments` (`status`);
CREATE INDEX `idx_tracking_shipment`       ON `shipment_tracking` (`shipment_id`);
CREATE INDEX `idx_tracking_timestamp`      ON `shipment_tracking` (`timestamp`);
CREATE INDEX `idx_drivers_user_id`         ON `drivers` (`user_id`);

-- -----------------------------------------------------
-- Roles (insert if not already present)
-- -----------------------------------------------------
INSERT IGNORE INTO `roles` (`name`, `description`)
VALUES
  ('SHIPPING_MGR', 'Shipping Manager — manages shipments, drivers and vehicles'),
  ('SHIP_DRIVER',  'Shipping Driver — executes shipment delivery');

SET FOREIGN_KEY_CHECKS = 1;
