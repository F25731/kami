package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.Project;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ProjectMapper {
    private final JdbcTemplate jdbcTemplate;

    public ProjectMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Project> findAll() {
        String sql = """
            SELECT p.*,
                   (SELECT COUNT(*) FROM cards c WHERE c.project_id = p.id) AS card_count,
                   (SELECT COUNT(*) FROM card_pricing cp WHERE cp.project_id = p.id) AS pricing_count,
                   (SELECT COUNT(*) FROM api_keys ak WHERE ak.project_id = p.id) AS api_key_count,
                   (SELECT COUNT(*) FROM api_call_logs l WHERE l.project_id = p.id AND DATE(l.created_at) = CURDATE()) AS today_call_count
            FROM projects p
            WHERE p.status <> 'deleted'
            ORDER BY p.created_at DESC
            """;
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Project findById(Long id) {
        List<Project> list = jdbcTemplate.query("SELECT * FROM projects WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public Project findByToken(String token) {
        List<Project> list = jdbcTemplate.query("SELECT * FROM projects WHERE project_token = ?", rowMapper, token);
        return list.isEmpty() ? null : list.get(0);
    }

    public Project findDefaultProject() {
        List<Project> list = jdbcTemplate.query("SELECT * FROM projects WHERE project_code = 'default' LIMIT 1", rowMapper);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean existsByCode(String code) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects WHERE project_code = ?", Integer.class, code);
        return count != null && count > 0;
    }

    public boolean existsByToken(String token) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM projects WHERE project_token = ?", Integer.class, token);
        return count != null && count > 0;
    }

    public void insert(Project project) {
        jdbcTemplate.update("""
            INSERT INTO projects (
                project_name, project_code, project_token, project_type, environment, usage_mode,
                status, remark, enable_device_bind, device_bind_mode, enable_signature, enable_ip_whitelist,
                rate_limit_per_minute, max_generate_per_request, max_generate_per_day,
                webhook_enabled, webhook_url, webhook_secret, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            project.getProjectName(), project.getProjectCode(), project.getProjectToken(),
            defaultString(project.getProjectType(), "other"), defaultString(project.getEnvironment(), "production"),
            defaultString(project.getUsageMode(), "direct_license"), defaultString(project.getStatus(), "enabled"),
            project.getRemark(), bool(project.getEnableDeviceBind()), defaultString(project.getDeviceBindMode(), "none"),
            bool(project.getEnableSignature()), bool(project.getEnableIpWhitelist()),
            defaultInt(project.getRateLimitPerMinute(), 120),
            defaultInt(project.getMaxGeneratePerRequest(), 100),
            defaultInt(project.getMaxGeneratePerDay(), 10000),
            bool(project.getWebhookEnabled()), project.getWebhookUrl(), project.getWebhookSecret(),
            Timestamp.valueOf(LocalDateTime.now())
        );
    }

    public void update(Project project) {
        jdbcTemplate.update("""
            UPDATE projects SET
                project_name = ?, project_code = ?, project_type = ?, environment = ?, usage_mode = ?,
                status = ?, remark = ?, enable_device_bind = ?, device_bind_mode = ?, enable_signature = ?,
                enable_ip_whitelist = ?, rate_limit_per_minute = ?, max_generate_per_request = ?,
                max_generate_per_day = ?, webhook_enabled = ?, webhook_url = ?, webhook_secret = ?
            WHERE id = ?
            """,
            project.getProjectName(), project.getProjectCode(), defaultString(project.getProjectType(), "other"),
            defaultString(project.getEnvironment(), "production"), defaultString(project.getUsageMode(), "direct_license"),
            defaultString(project.getStatus(), "enabled"), project.getRemark(), bool(project.getEnableDeviceBind()),
            defaultString(project.getDeviceBindMode(), "none"), bool(project.getEnableSignature()),
            bool(project.getEnableIpWhitelist()), defaultInt(project.getRateLimitPerMinute(), 120),
            defaultInt(project.getMaxGeneratePerRequest(), 100), defaultInt(project.getMaxGeneratePerDay(), 10000),
            bool(project.getWebhookEnabled()), project.getWebhookUrl(), project.getWebhookSecret(), project.getId());
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE projects SET status = ? WHERE id = ?", status, id);
    }

    public void updateToken(Long id, String token) {
        jdbcTemplate.update("UPDATE projects SET project_token = ? WHERE id = ?", token, id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("UPDATE projects SET status = 'deleted' WHERE id = ?", id);
    }

    private final RowMapper<Project> rowMapper = new RowMapper<>() {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project p = new Project();
            p.setId(rs.getLong("id"));
            p.setProjectName(rs.getString("project_name"));
            p.setProjectCode(rs.getString("project_code"));
            p.setProjectToken(rs.getString("project_token"));
            p.setProjectType(rs.getString("project_type"));
            p.setEnvironment(rs.getString("environment"));
            p.setUsageMode(rs.getString("usage_mode"));
            p.setStatus(rs.getString("status"));
            p.setRemark(rs.getString("remark"));
            p.setEnableDeviceBind(rs.getInt("enable_device_bind") == 1);
            p.setDeviceBindMode(rs.getString("device_bind_mode"));
            p.setEnableSignature(rs.getInt("enable_signature") == 1);
            p.setEnableIpWhitelist(rs.getInt("enable_ip_whitelist") == 1);
            p.setRateLimitPerMinute(rs.getInt("rate_limit_per_minute"));
            p.setMaxGeneratePerRequest(rs.getInt("max_generate_per_request"));
            p.setMaxGeneratePerDay(rs.getInt("max_generate_per_day"));
            p.setWebhookEnabled(rs.getInt("webhook_enabled") == 1);
            p.setWebhookUrl(rs.getString("webhook_url"));
            p.setWebhookSecret(rs.getString("webhook_secret"));
            if (rs.getTimestamp("created_at") != null) {
                p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            setOptionalCounts(rs, p);
            return p;
        }
    };

    private void setOptionalCounts(ResultSet rs, Project p) {
        try { p.setCardCount(rs.getInt("card_count")); } catch (SQLException ignored) {}
        try { p.setPricingCount(rs.getInt("pricing_count")); } catch (SQLException ignored) {}
        try { p.setApiKeyCount(rs.getInt("api_key_count")); } catch (SQLException ignored) {}
        try { p.setTodayCallCount(rs.getInt("today_call_count")); } catch (SQLException ignored) {}
    }

    private int bool(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
