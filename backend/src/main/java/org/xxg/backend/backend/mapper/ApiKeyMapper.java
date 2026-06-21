package org.xxg.backend.backend.mapper;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.ApiKey;
import org.xxg.backend.backend.entity.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ApiKeyMapper {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyMapper.class);

    private final JdbcTemplate jdbcTemplate;

    public ApiKeyMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchema() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS api_keys (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "project_id BIGINT NULL, " +
                    "key_name VARCHAR(50) NOT NULL, " +
                    "api_key VARCHAR(64) NOT NULL, " +
                    "api_secret_hash VARCHAR(255) NULL, " +
                    "permissions TEXT NULL, " +
                    "allowed_ips TEXT NULL, " +
                    "environment VARCHAR(30) NOT NULL DEFAULT 'production', " +
                    "expired_at DATETIME NULL, " +
                    "status TINYINT(1) NOT NULL DEFAULT 1, " +
                    "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "last_use_time DATETIME NULL DEFAULT NULL, " +
                    "use_count INT NOT NULL DEFAULT 0, " +
                    "description VARCHAR(255) NULL DEFAULT NULL, " +
                    "key_value VARCHAR(255) NOT NULL, " +
                    "name VARCHAR(100) NOT NULL DEFAULT 'API Key', " +
                    "webhook_config TEXT NULL, " +
                    "enable_card_encryption TINYINT(1) DEFAULT 0, " +
                    "require_machine_code TINYINT(1) DEFAULT 0, " +
                    "machine_spec_once_config TEXT NULL, " +
                    "update_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "UNIQUE INDEX api_key(api_key), " +
                    "UNIQUE INDEX idx_api_key_value(key_value)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            addColumn("api_keys", "project_id", "BIGINT NULL");
            addColumn("api_keys", "api_secret_hash", "VARCHAR(255) NULL");
            addColumn("api_keys", "permissions", "TEXT NULL");
            addColumn("api_keys", "allowed_ips", "TEXT NULL");
            addColumn("api_keys", "environment", "VARCHAR(30) NOT NULL DEFAULT 'production'");
            addColumn("api_keys", "expired_at", "DATETIME NULL");
            addColumn("api_keys", "webhook_config", "TEXT NULL");
            addColumn("api_keys", "enable_card_encryption", "TINYINT(1) DEFAULT 0");
            addColumn("api_keys", "require_machine_code", "TINYINT(1) NOT NULL DEFAULT 0");
            addColumn("api_keys", "machine_spec_once_config", "TEXT NULL");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_api_keys (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "api_key_id BIGINT NOT NULL, " +
                    "assign_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(user_id, api_key_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (Exception e) {
            logger.error("Failed to initialize ApiKey schema", e);
        }
    }

    public List<ApiKey> findAll() {
        return jdbcTemplate.query("SELECT * FROM api_keys ORDER BY create_time DESC", new ApiKeyRowMapper());
    }

    public List<ApiKey> findByProjectId(Long projectId) {
        return jdbcTemplate.query("SELECT * FROM api_keys WHERE project_id = ? ORDER BY create_time DESC", new ApiKeyRowMapper(), projectId);
    }

    public ApiKey findById(Long id) {
        List<ApiKey> list = jdbcTemplate.query("SELECT * FROM api_keys WHERE id = ?", new ApiKeyRowMapper(), id);
        return list.isEmpty() ? null : list.get(0);
    }

    public ApiKey findByApiKey(String apiKey) {
        List<ApiKey> list = jdbcTemplate.query("SELECT * FROM api_keys WHERE api_key = ?", new ApiKeyRowMapper(), apiKey);
        return list.isEmpty() ? null : list.get(0);
    }

    public ApiKey findByProjectAndApiKey(Long projectId, String apiKey) {
        List<ApiKey> list = jdbcTemplate.query("SELECT * FROM api_keys WHERE project_id = ? AND api_key = ?", new ApiKeyRowMapper(), projectId, apiKey);
        return list.isEmpty() ? null : list.get(0);
    }

    public void updateUsage(Long id) {
        jdbcTemplate.update("UPDATE api_keys SET use_count = IFNULL(use_count, 0) + 1, last_use_time = NOW() WHERE id = ?", id);
    }

    public void insert(ApiKey apiKey) {
        jdbcTemplate.update("""
            INSERT INTO api_keys (
                project_id, key_name, api_key, api_secret_hash, permissions, allowed_ips, environment, expired_at,
                key_value, name, description, status, create_time, webhook_config, enable_card_encryption,
                require_machine_code, machine_spec_once_config
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            apiKey.getProjectId(), apiKey.getKeyName(), apiKey.getApiKey(), apiKey.getApiSecretHash(),
            apiKey.getPermissions(), apiKey.getAllowedIps(), defaultString(apiKey.getEnvironment(), "production"),
            apiKey.getExpiredAt() != null ? Timestamp.valueOf(apiKey.getExpiredAt()) : null,
            apiKey.getKeyValue(), defaultString(apiKey.getName(), "API Key"), apiKey.getDescription(),
            apiKey.getStatus() == null ? 1 : apiKey.getStatus(), LocalDateTime.now(), apiKey.getWebhookConfig(),
            bool(apiKey.getEnableCardEncryption()), bool(apiKey.getRequireMachineCode()), apiKey.getMachineSpecOnceConfig());
    }

    public void update(ApiKey apiKey) {
        jdbcTemplate.update("""
            UPDATE api_keys SET key_name = ?, description = ?, status = ?, webhook_config = ?,
                enable_card_encryption = ?, require_machine_code = ?, machine_spec_once_config = ?,
                permissions = ?, allowed_ips = ?, environment = ?, expired_at = ?
            WHERE id = ?
            """,
            apiKey.getKeyName(), apiKey.getDescription(), apiKey.getStatus(), apiKey.getWebhookConfig(),
            bool(apiKey.getEnableCardEncryption()), bool(apiKey.getRequireMachineCode()), apiKey.getMachineSpecOnceConfig(),
            apiKey.getPermissions(), apiKey.getAllowedIps(), defaultString(apiKey.getEnvironment(), "production"),
            apiKey.getExpiredAt() != null ? Timestamp.valueOf(apiKey.getExpiredAt()) : null, apiKey.getId());
    }

    public void updateSecretHash(Long id, String secretHash) {
        jdbcTemplate.update("UPDATE api_keys SET api_secret_hash = ? WHERE id = ?", secretHash, id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM user_api_keys WHERE api_key_id = ?", id);
        jdbcTemplate.update("DELETE FROM api_keys WHERE id = ?", id);
    }

    public void assignUser(Long apiKeyId, Long userId) {
        jdbcTemplate.update("INSERT INTO user_api_keys (user_id, api_key_id) VALUES (?, ?)", userId, apiKeyId);
    }

    public void unassignUser(Long apiKeyId, Long userId) {
        jdbcTemplate.update("DELETE FROM user_api_keys WHERE user_id = ? AND api_key_id = ?", userId, apiKeyId);
    }

    public List<User> getAssignedUsers(Long apiKeyId) {
        String sql = "SELECT u.* FROM users u JOIN user_api_keys uak ON u.id = uak.user_id WHERE uak.api_key_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setNickname(rs.getString("nickname"));
            return user;
        }, apiKeyId);
    }

    public ApiKey findFirstUnassignedKey() {
        String sql = "SELECT * FROM api_keys WHERE id NOT IN (SELECT DISTINCT api_key_id FROM user_api_keys) LIMIT 1";
        List<ApiKey> list = jdbcTemplate.query(sql, new ApiKeyRowMapper());
        return list.isEmpty() ? null : list.get(0);
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

    private int bool(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static class ApiKeyRowMapper implements RowMapper<ApiKey> {
        @Override
        public ApiKey mapRow(ResultSet rs, int rowNum) throws SQLException {
            ApiKey apiKey = new ApiKey();
            apiKey.setId(rs.getLong("id"));
            try {
                long projectId = rs.getLong("project_id");
                if (!rs.wasNull()) apiKey.setProjectId(projectId);
            } catch (SQLException ignored) {}
            apiKey.setKeyName(rs.getString("key_name"));
            apiKey.setApiKey(rs.getString("api_key"));
            apiKey.setKeyValue(rs.getString("key_value"));
            apiKey.setName(rs.getString("name"));
            apiKey.setDescription(rs.getString("description"));
            apiKey.setStatus(rs.getInt("status"));
            safeString(rs, "api_secret_hash", apiKey::setApiSecretHash);
            safeString(rs, "permissions", apiKey::setPermissions);
            safeString(rs, "allowed_ips", apiKey::setAllowedIps);
            safeString(rs, "environment", apiKey::setEnvironment);
            safeString(rs, "webhook_config", apiKey::setWebhookConfig);
            safeString(rs, "machine_spec_once_config", apiKey::setMachineSpecOnceConfig);
            safeBoolean(rs, "enable_card_encryption", apiKey::setEnableCardEncryption);
            safeBoolean(rs, "require_machine_code", apiKey::setRequireMachineCode);
            safeTime(rs, "expired_at", apiKey::setExpiredAt);
            safeTime(rs, "create_time", apiKey::setCreateTime);
            safeTime(rs, "update_time", apiKey::setUpdateTime);
            safeTime(rs, "last_use_time", apiKey::setLastUseTime);
            try { apiKey.setUseCount(rs.getInt("use_count")); } catch (SQLException ignored) {}
            return apiKey;
        }

        private static void safeString(ResultSet rs, String column, java.util.function.Consumer<String> setter) {
            try { setter.accept(rs.getString(column)); } catch (SQLException ignored) {}
        }

        private static void safeBoolean(ResultSet rs, String column, java.util.function.Consumer<Boolean> setter) {
            try { setter.accept(rs.getInt(column) == 1); } catch (SQLException ignored) {}
        }

        private static void safeTime(ResultSet rs, String column, java.util.function.Consumer<LocalDateTime> setter) {
            try {
                if (rs.getTimestamp(column) != null) setter.accept(rs.getTimestamp(column).toLocalDateTime());
            } catch (SQLException ignored) {}
        }
    }
}
