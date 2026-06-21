package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.ProjectApiKey;

import java.sql.*;
import java.util.List;

@Repository
public class ProjectApiKeyMapper {

    private final JdbcTemplate jdbcTemplate;

    public ProjectApiKeyMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProjectApiKey> findByProjectId(Long projectId) {
        return jdbcTemplate.query(
            "SELECT * FROM project_api_keys WHERE project_id = ? ORDER BY id DESC",
            rowMapper, projectId);
    }

    public ProjectApiKey findById(Long id) {
        List<ProjectApiKey> list = jdbcTemplate.query(
            "SELECT * FROM project_api_keys WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public ProjectApiKey findByApiKey(String apiKey) {
        List<ProjectApiKey> list = jdbcTemplate.query(
            "SELECT * FROM project_api_keys WHERE api_key = ?", rowMapper, apiKey);
        return list.isEmpty() ? null : list.get(0);
    }

    /** lookup by secret hash (used during auth — never store/compare plain secret) */
    public ProjectApiKey findByApiKeyAndSecretHash(String apiKey, String secretHash) {
        List<ProjectApiKey> list = jdbcTemplate.query(
            "SELECT * FROM project_api_keys WHERE api_key = ? AND api_secret_hash = ? AND status = 'active'",
            rowMapper, apiKey, secretHash);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(ProjectApiKey key) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO project_api_keys " +
                "(project_id, key_name, api_key, api_secret_hash, permissions, allowed_ips, " +
                " status, environment, expired_at, remark, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, key.getProjectId());
            ps.setString(2, key.getKeyName());
            ps.setString(3, key.getApiKey());
            ps.setString(4, key.getApiSecretHash());
            ps.setString(5, key.getPermissions());
            ps.setString(6, key.getAllowedIps());
            ps.setString(7, key.getStatus() != null ? key.getStatus() : "active");
            ps.setString(8, key.getEnvironment() != null ? key.getEnvironment() : "production");
            if (key.getExpiredAt() != null) ps.setTimestamp(9, Timestamp.valueOf(key.getExpiredAt()));
            else ps.setNull(9, Types.TIMESTAMP);
            ps.setString(10, key.getRemark());
            return ps;
        }, holder);
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    public void update(ProjectApiKey key) {
        jdbcTemplate.update(
            "UPDATE project_api_keys SET key_name=?, permissions=?, allowed_ips=?, status=?, " +
            "environment=?, expired_at=?, remark=? WHERE id=?",
            key.getKeyName(), key.getPermissions(), key.getAllowedIps(), key.getStatus(),
            key.getEnvironment(),
            key.getExpiredAt() != null ? Timestamp.valueOf(key.getExpiredAt()) : null,
            key.getRemark(), key.getId());
    }

    /** rotate: replace api_key + api_secret_hash, clear use_count */
    public void rotate(Long id, String newApiKey, String newSecretHash) {
        jdbcTemplate.update(
            "UPDATE project_api_keys SET api_key=?, api_secret_hash=?, use_count=0 WHERE id=?",
            newApiKey, newSecretHash, id);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE project_api_keys SET status=? WHERE id=?", status, id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM project_api_keys WHERE id=?", id);
    }

    public void incrementUseCount(String apiKey) {
        jdbcTemplate.update(
            "UPDATE project_api_keys SET use_count = use_count + 1, last_used_at = NOW() WHERE api_key = ?",
            apiKey);
    }

    private final RowMapper<ProjectApiKey> rowMapper = (rs, rowNum) -> {
        ProjectApiKey k = new ProjectApiKey();
        k.setId(rs.getLong("id"));
        k.setProjectId(rs.getLong("project_id"));
        k.setKeyName(rs.getString("key_name"));
        k.setApiKey(rs.getString("api_key"));
        k.setApiSecretHash(rs.getString("api_secret_hash"));
        k.setPermissions(rs.getString("permissions"));
        k.setAllowedIps(rs.getString("allowed_ips"));
        k.setStatus(rs.getString("status"));
        k.setEnvironment(rs.getString("environment"));
        if (rs.getTimestamp("last_used_at") != null)
            k.setLastUsedAt(rs.getTimestamp("last_used_at").toLocalDateTime());
        k.setUseCount(rs.getLong("use_count"));
        if (rs.getTimestamp("expired_at") != null)
            k.setExpiredAt(rs.getTimestamp("expired_at").toLocalDateTime());
        k.setRemark(rs.getString("remark"));
        if (rs.getTimestamp("created_at") != null)
            k.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null)
            k.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return k;
    };
}
