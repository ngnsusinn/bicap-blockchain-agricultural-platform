# BICAP - Database & Cache Configuration Guide (BICAP-79)

This document provides instructions for deploying and configuring the database and caching tiers for the Blockchain Agricultural Platform (BICAP). It contains:
1. **Online/Cloud Deployment Configurations** for MySQL (e.g. Supabase, AWS RDS, Aiven, Clever Cloud) and Redis (e.g. Upstash, Redis Labs).
2. **Local Docker Compose Setup** for MySQL 5.7.41 and Redis 8.6.
3. **Complete MySQL 5.7.41 DDL scripts** for all 23 system tables.
4. **MySQL Indexing Strategy** for high-performance query optimization.
5. **Redis 8.6 Caching Configurations** (eviction policies, memory limits, network security, and key patterns).

---

## 1. Online / Cloud Platform Deployment (Recommended)

To deploy your databases on the cloud, follow these recommendations and use the environment variables in your hosting environment (such as Render, Fly.io, Railway, AWS ECS, or Vercel).

### 1.1. Cloud MySQL (e.g., AWS RDS, Supabase, Aiven, Clever Cloud)
1. **Create Instance**: Provision a MySQL 5.7 or 8.0 instance on your preferred cloud provider.
2. **Environment Variables**: Configure the following environment variables in your backend hosting service:
   * `SPRING_DATASOURCE_URL`: `jdbc:mysql://<your-cloud-db-host>:<port>/<database_name>?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true`
   * `SPRING_DATASOURCE_USERNAME`: Your database username.
   * `SPRING_DATASOURCE_PASSWORD`: Your database password.
3. **Run DDL**: Execute the SQL scripts in **Section 3** below against your cloud instance using a database client (such as DBeaver, MySQL Workbench, or IntelliJ Database tool).

### 1.2. Cloud Redis (e.g., Upstash, Redis Labs, Aiven)
1. **Create Cache**: Create a Redis database (e.g., on Upstash with SSL/TLS enabled).
2. **Environment Variables**: Configure the following variables in your backend hosting service:
   * `SPRING_REDIS_HOST`: The endpoint string (e.g., `redis-12345.c10.us-east-1-4.ec2.cloud.redislabs.com` or Upstash host).
   * `SPRING_REDIS_PORT`: Typically `6379` (or `30000`–`65000` depending on the provider).
   * `SPRING_REDIS_PASSWORD`: Your Redis access token/password.
   * `SPRING_REDIS_SSL`: Set to `true` (strongly recommended for cloud networks to ensure data encrypts in-transit).

---

## 2. Alternative: Local Docker Compose Setup

Use the following configuration to deploy MySQL and Redis services locally or in your dev/staging environment. Save this as `docker-compose.db.yml`.

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:5.7.41
    container_name: bicap-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root_secure_password
      MYSQL_DATABASE: bicap_db
      MYSQL_USER: bicap_user
      MYSQL_PASSWORD: bicap_password
    volumes:
      - bicap-mysql-data:/var/lib/mysql
    networks:
      - bicap-network
    restart: unless-stopped
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:8.6-rc2-alpine # Or stable 8.x when officially released
    container_name: bicap-redis
    ports:
      - "6379:6379"
    command: redis-server --requirepass redis_secure_password --maxmemory 512mb --maxmemory-policy allkeys-lru --tls-port 0 # For local dev (TLS disabled). Enable TLS for staging/production.
    volumes:
      - bicap-redis-data:/data
    networks:
      - bicap-network
    restart: unless-stopped

networks:
  bicap-network:
    name: bicap-network
    driver: bridge

volumes:
  bicap-mysql-data:
    driver: local
  bicap-redis-data:
    driver: local
```

To run the services:
```bash
docker-compose -f docker-compose.db.yml up -d
```

---

## 2. MySQL 5.7.41 DDL Schemas

Below is the complete SQL schema containing all tables in the database tier, ordered by dependency hierarchy (tables without foreign key dependencies first).

```sql
-- Disable foreign key checks to prevent ordering dependency errors during DDL execution
SET FOREIGN_KEY_CHECKS = 0;

-- Create Database (Optional - Commented out for restricted cloud/shared hosting environments)
-- CREATE DATABASE IF NOT EXISTS bicap_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE bicap_db;

-- -----------------------------------------------------
-- Table: permissions
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `permissions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(100) NOT NULL UNIQUE,
  `description` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: roles
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL UNIQUE,
  `description` VARCHAR(255) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: role_permissions
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `role_permissions` (
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`),
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: users
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `password` VARCHAR(128) NOT NULL,
  `full_name` VARCHAR(255) NOT NULL,
  `phone` VARCHAR(15) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, SUSPENDED
  `avatar_url` VARCHAR(500) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: user_roles
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_roles` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: service_packages
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `service_packages` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT NULL,
  `price` DECIMAL(12,2) NOT NULL,
  `duration_days` INT NOT NULL,
  `features` JSON NULL, -- Features structured as JSON
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: vehicles
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `vehicles` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `license_plate` VARCHAR(20) NOT NULL UNIQUE,
  `type` VARCHAR(50) NOT NULL,
  `capacity` DOUBLE NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, IN_USE, MAINTENANCE
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: drivers
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `drivers` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL UNIQUE,
  `citizen_id` VARCHAR(20) NOT NULL UNIQUE,
  `license_number` VARCHAR(30) NOT NULL UNIQUE,
  `vehicle_id` BIGINT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'IDLE', -- IDLE, ON_TRIP, OFFLINE
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_drivers_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_drivers_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: farms
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `farms` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `address` VARCHAR(500) NOT NULL,
  `area` DOUBLE NOT NULL,
  `gps_lat` DOUBLE NULL,
  `gps_lng` DOUBLE NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_farms_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: farm_certifications
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `farm_certifications` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `farm_id` BIGINT NOT NULL,
  `type` VARCHAR(100) NOT NULL, -- VietGAP, GlobalGAP, Organic, etc.
  `file_url` VARCHAR(500) NOT NULL,
  `expiry_date` DATE NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_certifications_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: subscriptions
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `subscriptions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `farm_id` BIGINT NOT NULL,
  `package_id` BIGINT NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXPIRED, CANCELLED
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_subscriptions_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_subscriptions_package` FOREIGN KEY (`package_id`) REFERENCES `service_packages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: farming_seasons
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `farming_seasons` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `farm_id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `product_type` VARCHAR(100) NOT NULL,
  `variety` VARCHAR(100) NOT NULL,
  `area` DOUBLE NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, HARVESTED, CANCELLED
  `tx_hash` VARCHAR(66) NULL, -- VeChain transaction hash
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_seasons_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: farming_processes
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `farming_processes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `season_id` BIGINT NOT NULL,
  `process_type` VARCHAR(100) NOT NULL, -- SOIL_PREP, SEEDING, FERTILIZATION, PEST_CONTROL, HARVESTING
  `execution_date` DATE NOT NULL,
  `materials` JSON NULL,
  `images` JSON NULL, -- List of image URLs
  `notes` TEXT NULL,
  `tx_hash` VARCHAR(66) NULL, -- VeChain transaction hash
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_processes_season` FOREIGN KEY (`season_id`) REFERENCES `farming_seasons` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
-- Table: qrcodes
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `qrcodes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `trace_url` VARCHAR(500) NOT NULL,
  `qr_image` VARCHAR(500) NOT NULL,
  `season_id` BIGINT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_qrcodes_season` FOREIGN KEY (`season_id`) REFERENCES `farming_seasons` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: products
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `products` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `season_id` BIGINT NOT NULL,
  `export_id` BIGINT NULL, -- Nguồn lô hàng xuất kho khi đăng sản phẩm lên sàn (BICAP-18 / SRS-FM-012)
  `category_id` BIGINT NOT NULL, -- References categories (e.g. Rau, Củ quả, Trái cây)
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `images` TEXT NULL, -- JSON array of product image URLs (BICAP-18 / SRS-FM-012: 1-10 ảnh)
  `price` DECIMAL(12,2) NOT NULL,
  `quantity` DOUBLE NOT NULL,
  `qr_code_id` BIGINT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, PENDING_REVIEW (BICAP-5 / SRS-ADM-004)
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_products_season` FOREIGN KEY (`season_id`) REFERENCES `farming_seasons` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_products_export` FOREIGN KEY (`export_id`) REFERENCES `season_exports` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `fk_products_qrcode` FOREIGN KEY (`qr_code_id`) REFERENCES `qrcodes` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: orders
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `product_id` BIGINT NOT NULL,
  `retailer_id` BIGINT NOT NULL, -- References users(id) with RETAILER role
  `quantity` DOUBLE NOT NULL,
  `price` DECIMAL(12,2) NOT NULL, -- Snapshotted unit price
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, PAID, SHIPPING, COMPLETED, CANCELLED
  `delivery_addr` VARCHAR(500) NOT NULL,
  `deposit_rate` DOUBLE NOT NULL DEFAULT 0.0,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_orders_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`),
  CONSTRAINT `fk_orders_retailer` FOREIGN KEY (`retailer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: payments
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `payments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `method` VARCHAR(50) NOT NULL DEFAULT 'VNPAY', -- VNPAY, STRIPE, METAMASK
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED, REFUNDED
  `tx_ref` VARCHAR(100) NULL, -- Gateway reference ID
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_payments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: shipments
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `shipments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL UNIQUE,
  `driver_id` BIGINT NULL,
  `vehicle_id` BIGINT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PICKING_UP', -- PICKING_UP, IN_TRANSIT, DELIVERED, RETURNED
  `pickup_time` DATETIME NULL,
  `delivery_time` DATETIME NULL,
  `route_summary` VARCHAR(500) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_shipments_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_shipments_driver` FOREIGN KEY (`driver_id`) REFERENCES `drivers` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_shipments_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: shipment_tracking
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `shipment_tracking` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `shipment_id` BIGINT NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `gps_lat` DOUBLE NOT NULL,
  `gps_lng` DOUBLE NOT NULL,
  `images` JSON NULL,
  `notes` TEXT NULL,
  `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_tracking_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: blockchain_transactions
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `blockchain_transactions` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `entity_type` VARCHAR(50) NOT NULL, -- SEASON, PROCESS, QR, EXPORT
  `entity_id` BIGINT NOT NULL,
  `tx_hash` VARCHAR(66) NOT NULL UNIQUE,
  `contract_address` VARCHAR(42) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, CONFIRMED, FAILED
  `retry_count` INT NOT NULL DEFAULT 0,
  `idempotency_key` VARCHAR(100) NOT NULL UNIQUE,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: notifications
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(50) NOT NULL, -- INFO, SUCCESS, WARNING, ALARM
  `title` VARCHAR(255) NOT NULL,
  `content` TEXT NOT NULL,
  `channel` VARCHAR(20) NOT NULL DEFAULT 'IN_APP', -- IN_APP, PUSH, EMAIL
  `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_notif_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: reports
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `reports` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `type` VARCHAR(50) NOT NULL, -- INCIDENT, SYSTEM, INQUIRY
  `title` VARCHAR(255) NOT NULL,
  `content` TEXT NOT NULL,
  `attachments` JSON NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, INVESTIGATING, RESOLVED, CLOSED
  `admin_response` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_reports_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Table: iot_data
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `iot_data` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `farm_id` BIGINT NOT NULL,
  `temperature` DOUBLE NOT NULL,
  `humidity` DOUBLE NOT NULL,
  `ph` DOUBLE NOT NULL,
  `measured_at` TIMESTAMP NOT NULL,
  CONSTRAINT `fk_iot_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------
-- Seed default permissions
-- -----------------------------------------------------
INSERT INTO `permissions` (`id`, `code`, `description`) VALUES
(1, 'ADMIN_CREATE', 'Permission to create admin accounts'),
(2, 'ADMIN_READ', 'Permission to view admin accounts'),
(3, 'ADMIN_UPDATE', 'Permission to update admin accounts'),
(4, 'ADMIN_DELETE', 'Permission to delete admin accounts')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- -----------------------------------------------------
-- Seed default roles
-- -----------------------------------------------------
INSERT INTO `roles` (`id`, `name`, `description`) VALUES
(1, 'SUPER_ADMIN', 'Super Administrator with full access'),
(2, 'ADMIN', 'Administrator with read/write access'),
(3, 'MODERATOR', 'Moderator with read-only access'),
(4, 'FARM_MANAGER', 'Farm Manager for managing farms, seasons, and exports'),
(5, 'RETAILER', 'Retailer for purchasing products and tracking orders'),
(6, 'SHIPPING_MGR', 'Shipping Manager for coordinating deliveries'),
(7, 'SHIP_DRIVER', 'Shipping Driver for executing shipments'),
(8, 'GUEST', 'Guest user for browsing products and educational content')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

-- -----------------------------------------------------
-- Seed default role-permission mappings
-- -----------------------------------------------------
INSERT INTO `role_permissions` (`role_id`, `permission_id`) VALUES
(1, 1), -- SUPER_ADMIN can create admins
(1, 2), -- SUPER_ADMIN can read admins
(1, 3), -- SUPER_ADMIN can update admins
(1, 4), -- SUPER_ADMIN can delete admins
(2, 1), -- ADMIN can create admins
(2, 2), -- ADMIN can read admins
(2, 3), -- ADMIN can update admins
(3, 2)  -- MODERATOR can read admins
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
```

---

## 3. MySQL Indexing Strategy

To optimize queries under heavy transactional load, run the following index configurations on the MySQL instance:

```sql
-- Index for login/query lookup
-- Note: 'email' is already defined as UNIQUE, so MySQL automatically indexes it. No separate index is needed.
CREATE INDEX `idx_users_phone` ON `users` (`phone`);

-- Index for filtering farms by approval status
CREATE INDEX `idx_farms_status` ON `farms` (`status`);

-- Index for listing seasons inside a farm by active status
CREATE INDEX `idx_seasons_farm_status` ON `farming_seasons` (`farm_id`, `status`);

-- Index for filtering products by category and status on marketplace
CREATE INDEX `idx_products_category_status` ON `products` (`category_id`, `status`);
CREATE INDEX `idx_products_price` ON `products` (`price`);

-- Index for order histories by status and buyer
CREATE INDEX `idx_orders_retailer_status` ON `orders` (`retailer_id`, `status`);

-- Index for driver delivery status
CREATE INDEX `idx_shipments_driver_status` ON `shipments` (`driver_id`, `status`);

-- Index for listing unread notifications
CREATE INDEX `idx_notif_user_read` ON `notifications` (`user_id`, `is_read`);

-- Index for time-series IoT query trends (MySQL 5.7 compatibility)
CREATE INDEX `idx_iot_farm_time` ON `iot_data` (`farm_id`, `measured_at`);
```

---

## 4. Redis 8.6 Caching Configurations

### 4.1. Server Parameters (`redis.conf`)
Configure the Redis daemon with the following rules:

```ini
# Require password authentication
requirepass redis_secure_password

# Memory Management (512MB limit, use Least Recently Used eviction for keys when full)
maxmemory 512mb
maxmemory-policy allkeys-lru

# Disable persistence if pure cache is desired, or enable AOF for high availability
appendonly yes
appendfsync everysec

# Networking security - bind to internal network only
bind 127.0.0.1 ::1
protected-mode yes
```

### 4.2. Caching Key Policy
The application implements a **Dual Storage** mechanism caching the following structures:

| Key Pattern | Data Structure | TTL | Purpose |
|-------------|----------------|-----|---------|
| `session:{token}` | String (JSON) | 15 mins | Current user session details |
| `user:{id}` | String (JSON) | 30 mins | General user profile info |
| `farm:{id}` | String (JSON) | 1 hour | Farm registration data |
| `product:list:{page}:{filterHash}` | String (JSON) | 5 mins | Paginated product search results |
| `product:{id}` | String (JSON) | 30 mins | Detailed product metadata |
| `notification:{userId}:unread` | Integer | 1 min | Count of unread notifications |
| `iot:{farmId}:latest` | String (JSON) | 5 mins | Latest telemetry dashboard data |
| `rate_limit:{ip}` | String | 1 min | IP Rate limiting count |
| `rate_limit:{userId}` | String | 1 min | User Rate limiting count |
| `bc:tx:{hash}` | String (JSON) | 24 hours | VeChain Thor transaction receipt |

### 4.3. Cache Invalidation Patterns
1. **Write-through / Invalidate:** When a database record changes (e.g. updating user info), invoke Redis `DEL user:{id}` to force fresh lookup on next request.
2. **TTL expiry:** Static data (products, farms) uses short-term TTL (5-60m) to self-heal discrepancies.
3. **Event-driven invalidation:** When an order transitions status, dispatch an internal event that clears `product:list:*` to prevent displaying stale inventory count.
