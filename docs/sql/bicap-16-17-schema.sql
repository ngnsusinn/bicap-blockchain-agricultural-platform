-- BICAP-16/17 only. Apply after the farming_seasons table from BICAP-14/15 exists.
CREATE TABLE IF NOT EXISTS season_exports (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    farm_id BIGINT NOT NULL,
    season_id BIGINT NOT NULL,
    quantity DECIMAL(18,2) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    export_date DATE NOT NULL,
    warehouse VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'BLOCKCHAIN_PENDING',
    tx_hash VARCHAR(66) NULL,
    trace_hash VARCHAR(66) NULL,
    qr_image TEXT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_season_exports_farm FOREIGN KEY (farm_id) REFERENCES farms(id),
    CONSTRAINT fk_season_exports_season FOREIGN KEY (season_id) REFERENCES farming_seasons(id),
    CONSTRAINT fk_season_exports_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT uk_season_exports_trace_hash UNIQUE (trace_hash),
    CONSTRAINT uk_season_exports_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_season_exports_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_season_exports_season ON season_exports(season_id);
CREATE INDEX idx_season_exports_farm_created ON season_exports(farm_id, created_at);
