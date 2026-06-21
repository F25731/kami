package org.xxg.backend.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LicenseCenterSchemaMigrator {
    private final JdbcTemplate jdbcTemplate;

    public LicenseCenterSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        createProjectsTable();
        ensureDefaultProject();
        ensureProjectColumns();
        ensureApiKeyColumns();
        ensureOrderColumns();
        createOperationalTables();
        ensureOperationalColumns();
        migrateOldRowsToDefaultProject();
        ensureIndexes();
        removePublicCommerceDefaults();
    }

    private void createProjectsTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS projects (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
                UNIQUE KEY uk_projects_code (project_code),
                UNIQUE KEY uk_projects_token (project_token)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void ensureDefaultProject() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM projects WHERE project_code = 'default'", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO projects (
                    project_name, project_code, project_token, project_type, environment, usage_mode,
                    status, remark, enable_device_bind, device_bind_mode
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "Default Project", "default", "Default", "other", "production",
                    "direct_license", "enabled", "Migrated from the original single-project data", 0, "none");
        }
    }

    private void ensureProjectColumns() {
        addColumn("cards", "project_id", "BIGINT NULL");
        addColumn("card_pricing", "project_id", "BIGINT NULL");
        addColumn("api_keys", "project_id", "BIGINT NULL");
        addColumn("orders", "project_id", "BIGINT NULL");
        addColumn("operation_logs", "project_id", "BIGINT NULL");
    }

    private void ensureApiKeyColumns() {
        addColumn("api_keys", "api_secret_hash", "VARCHAR(255) NULL");
        addColumn("api_keys", "permissions", "TEXT NULL");
        addColumn("api_keys", "allowed_ips", "TEXT NULL");
        addColumn("api_keys", "environment", "VARCHAR(30) NOT NULL DEFAULT 'production'");
        addColumn("api_keys", "expired_at", "DATETIME NULL");
    }

    private void ensureOrderColumns() {
        addColumn("orders", "source", "VARCHAR(50) NOT NULL DEFAULT 'admin'");
        addColumn("orders", "external_order_no", "VARCHAR(100) NULL");
        addColumn("orders", "package_id", "INT NULL");
    }

    private void createOperationalTables() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS api_call_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
                KEY idx_api_call_logs_project_time (project_id, created_at),
                KEY idx_api_call_logs_api_key_time (api_key_id, created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS consume_logs (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
                UNIQUE KEY uk_consume_project_biz (project_id, biz_id),
                KEY idx_consume_project_card (project_id, card_key),
                KEY idx_consume_project_user (project_id, user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_entitlements (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                project_id BIGINT NOT NULL,
                user_id VARCHAR(100) NOT NULL,
                entitlement_type VARCHAR(20) NOT NULL DEFAULT 'count',
                total_count INT NOT NULL DEFAULT 0,
                remaining_count INT NOT NULL DEFAULT 0,
                expire_time DATETIME NULL,
                source_card_id BIGINT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY uk_entitlement_project_user (project_id, user_id),
                KEY idx_entitlement_project_expire (project_id, expire_time)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS api_nonces (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                project_id BIGINT NOT NULL,
                api_key_id BIGINT NOT NULL,
                nonce VARCHAR(120) NOT NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_nonce_project_key (project_id, api_key_id, nonce),
                KEY idx_nonce_created (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
    }

    private void ensureOperationalColumns() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS card_packages (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                project_id BIGINT NOT NULL,
                package_name VARCHAR(100) NOT NULL,
                package_code VARCHAR(80) NOT NULL,
                card_type VARCHAR(20) NOT NULL DEFAULT 'count',
                count_value INT NOT NULL DEFAULT 0,
                duration_days INT NOT NULL DEFAULT 0,
                is_permanent TINYINT(1) NOT NULL DEFAULT 0,
                price DECIMAL(10,2) NULL,
                status VARCHAR(20) NOT NULL DEFAULT 'enabled',
                sort INT NOT NULL DEFAULT 0,
                remark VARCHAR(500) NULL,
                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                KEY idx_card_packages_project (project_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

        addColumn("api_call_logs", "api_key", "VARCHAR(255) NULL");
        addColumn("api_call_logs", "response_body", "TEXT NULL");
        addColumn("api_call_logs", "client_ip", "VARCHAR(64) NULL");
        addColumn("api_call_logs", "duration_ms", "BIGINT NULL");
        addColumn("api_call_logs", "status", "VARCHAR(20) NOT NULL DEFAULT 'success'");
        addColumn("api_call_logs", "error_message", "VARCHAR(1000) NULL");
        addColumn("api_call_logs", "request_ip", "VARCHAR(64) NULL");
        addColumn("api_call_logs", "success", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumn("api_call_logs", "cost_ms", "BIGINT NOT NULL DEFAULT 0");

        addColumn("user_entitlements", "start_time", "DATETIME NULL");
        addColumn("user_entitlements", "is_permanent", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumn("user_entitlements", "source_order_no", "VARCHAR(100) NULL");
        addColumn("user_entitlements", "status", "VARCHAR(20) NOT NULL DEFAULT 'active'");
    }

    private void migrateOldRowsToDefaultProject() {
        Long defaultProjectId = jdbcTemplate.queryForObject(
                "SELECT id FROM projects WHERE project_code = 'default' LIMIT 1", Long.class);
        if (defaultProjectId == null) {
            return;
        }
        jdbcTemplate.update("UPDATE cards SET project_id = ? WHERE project_id IS NULL", defaultProjectId);
        jdbcTemplate.update("UPDATE card_pricing SET project_id = ? WHERE project_id IS NULL", defaultProjectId);
        jdbcTemplate.update("UPDATE api_keys SET project_id = ? WHERE project_id IS NULL", defaultProjectId);
        jdbcTemplate.update("UPDATE orders SET project_id = ? WHERE project_id IS NULL", defaultProjectId);
        jdbcTemplate.update("UPDATE operation_logs SET project_id = ? WHERE project_id IS NULL", defaultProjectId);
    }

    private void ensureIndexes() {
        addIndex("cards", "idx_cards_project_key", "project_id, card_key");
        addIndex("cards", "idx_cards_project_status", "project_id, status");
        addIndex("card_pricing", "idx_pricing_project_type", "project_id, type");
        addIndex("api_keys", "idx_api_keys_project_key", "project_id, api_key");
        addIndex("orders", "idx_orders_project_order", "project_id, order_no");
        addIndex("orders", "idx_orders_project_external", "project_id, external_order_no");
    }

    private void removePublicCommerceDefaults() {
        safeUpdate("UPDATE settings SET value = 'Multi-Project License Center' WHERE name IN ('site_title', 'systemName')");
        safeUpdate("UPDATE settings SET value = 'Private license center for multiple projects' WHERE name IN ('site_subtitle', 'systemDescription')");
        safeUpdate("UPDATE settings SET value = 'License Center - All Rights Reserved' WHERE name = 'copyright_text'");
    }

    private void addColumn(String table, String column, String definition) {
        try {
            jdbcTemplate.queryForObject("SELECT " + column + " FROM " + table + " LIMIT 1", Object.class);
        } catch (Exception ignored) {
            try {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            } catch (Exception ignoredAgain) {
            }
        }
    }

    private void addIndex(String table, String indexName, String columns) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD INDEX " + indexName + " (" + columns + ")");
        } catch (Exception ignored) {
        }
    }

    private void safeUpdate(String sql) {
        try {
            jdbcTemplate.update(sql);
        } catch (Exception ignored) {
        }
    }
}
