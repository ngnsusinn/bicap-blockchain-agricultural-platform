-- BICAP-36/37/38 schema update for MySQL 5.7.
-- Apply once before running with DDL_AUTO=validate.

ALTER TABLE users
    ADD COLUMN address VARCHAR(500) NULL,
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME NULL;

-- EnumType.STRING expects a string-compatible column and now includes
-- PENDING_VERIFICATION in addition to the existing statuses.
ALTER TABLE users
    MODIFY COLUMN status VARCHAR(32) NOT NULL;

CREATE TABLE IF NOT EXISTS retailer_business_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    license_url VARCHAR(1000) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_retailer_business_user (user_id),
    CONSTRAINT fk_retailer_business_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
