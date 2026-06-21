-- Multi-project license center migration.
-- Run after the original kami.sql. It is idempotent for normal upgrades.

CREATE TABLE IF NOT EXISTS projects (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_name VARCHAR(100) NOT NULL,
  project_code VARCHAR(80) NOT NULL,
  project_token VARCHAR(7) NOT NULL,
  project_type VARCHAR(30) NOT NULL DEFAULT 'other',
  environment VARCHAR(30) NOT NULL DEFAULT 'production',
  usage_mode VARCHAR(30) NOT NULL DEFAULT 'direct_license',
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',
  remark VARCHAR(500) NULL,
  enable_device_bind TINYINT(1) NOT NULL DEFAULT 0,
  device_bind_mode VARCHAR(30) NOT NULL DEFAULT 'none',
  enable_signature TINYINT(1) NOT NULL DEFAULT 0,
  enable_ip_whitelist TINYINT(1) NOT NULL DEFAULT 0,
  rate_limit_per_minute INT NOT NULL DEFAULT 120,
  max_generate_per_request INT NOT NULL DEFAULT 100,
  max_generate_per_day INT NOT NULL DEFAULT 10000,
  webhook_enabled TINYINT(1) NOT NULL DEFAULT 0,
  webhook_url VARCHAR(500) NULL,
  webhook_secret VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_projects_code (project_code),
  UNIQUE KEY uk_projects_token (project_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO projects (
  project_name, project_code, project_token, project_type, environment, usage_mode,
  status, remark, enable_device_bind, device_bind_mode
)
SELECT 'Default Project', 'default', 'Default', 'other', 'production', 'direct_license',
       'enabled', 'Migrated from the original single-project data', 0, 'none'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'default');

SET @default_project_id := (SELECT id FROM projects WHERE project_code = 'default' LIMIT 1);

ALTER TABLE cards ADD COLUMN project_id BIGINT NULL;
ALTER TABLE card_pricing ADD COLUMN project_id BIGINT NULL;
ALTER TABLE api_keys ADD COLUMN project_id BIGINT NULL;
ALTER TABLE orders ADD COLUMN project_id BIGINT NULL;
ALTER TABLE operation_logs ADD COLUMN project_id BIGINT NULL;

UPDATE cards SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE card_pricing SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE api_keys SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE orders SET project_id = @default_project_id WHERE project_id IS NULL;
UPDATE operation_logs SET project_id = @default_project_id WHERE project_id IS NULL;

ALTER TABLE api_keys ADD COLUMN api_secret_hash VARCHAR(255) NULL;
ALTER TABLE api_keys ADD COLUMN permissions TEXT NULL;
ALTER TABLE api_keys ADD COLUMN allowed_ips TEXT NULL;
ALTER TABLE api_keys ADD COLUMN environment VARCHAR(30) NOT NULL DEFAULT 'production';
ALTER TABLE api_keys ADD COLUMN expired_at DATETIME NULL;

ALTER TABLE orders ADD COLUMN source VARCHAR(50) NOT NULL DEFAULT 'admin';
ALTER TABLE orders ADD COLUMN external_order_no VARCHAR(100) NULL;
ALTER TABLE orders ADD COLUMN package_id INT NULL;

CREATE TABLE IF NOT EXISTS api_call_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  api_key_id BIGINT NULL,
  endpoint VARCHAR(255) NOT NULL,
  method VARCHAR(20) NOT NULL,
  request_ip VARCHAR(64) NULL,
  request_body TEXT NULL,
  response_code INT NULL,
  success TINYINT(1) NOT NULL DEFAULT 0,
  message VARCHAR(500) NULL,
  cost_ms BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_api_call_logs_project_time (project_id, created_at),
  KEY idx_api_call_logs_api_key_time (api_key_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consume_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  api_key_id BIGINT NULL,
  card_id BIGINT NULL,
  card_key VARCHAR(512) NULL,
  user_id VARCHAR(100) NULL,
  biz_id VARCHAR(100) NOT NULL,
  consume_scene VARCHAR(100) NULL,
  amount INT NOT NULL DEFAULT 1,
  before_count INT NULL,
  after_count INT NULL,
  refunded TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  refunded_at DATETIME NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_consume_project_biz (project_id, biz_id),
  KEY idx_consume_project_card (project_id, card_key),
  KEY idx_consume_project_user (project_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_entitlements (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  user_id VARCHAR(100) NOT NULL,
  entitlement_type VARCHAR(20) NOT NULL DEFAULT 'count',
  total_count INT NOT NULL DEFAULT 0,
  remaining_count INT NOT NULL DEFAULT 0,
  expire_time DATETIME NULL,
  source_card_id BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_entitlement_project_user (project_id, user_id),
  KEY idx_entitlement_project_expire (project_id, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS api_nonces (
  id BIGINT NOT NULL AUTO_INCREMENT,
  project_id BIGINT NOT NULL,
  api_key_id BIGINT NOT NULL,
  nonce VARCHAR(120) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_nonce_project_key (project_id, api_key_id, nonce),
  KEY idx_nonce_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cards_project_key ON cards(project_id, card_key);
CREATE INDEX idx_cards_project_status ON cards(project_id, status);
CREATE INDEX idx_pricing_project_type ON card_pricing(project_id, type);
CREATE INDEX idx_api_keys_project_key ON api_keys(project_id, api_key);
CREATE INDEX idx_orders_project_order ON orders(project_id, order_no);
CREATE INDEX idx_orders_project_external ON orders(project_id, external_order_no);
