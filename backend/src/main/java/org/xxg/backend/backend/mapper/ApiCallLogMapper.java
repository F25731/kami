package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.ApiCallLog;

import java.sql.*;
import java.util.List;

@Repository
public class ApiCallLogMapper {

    private final JdbcTemplate jdbcTemplate;

    public ApiCallLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(ApiCallLog log) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO api_call_logs " +
                "(project_id, api_key_id, api_key, endpoint, method, request_body, " +
                " response_code, response_body, client_ip, duration_ms, status, error_message, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, log.getProjectId());
            if (log.getApiKeyId() != null) ps.setLong(2, log.getApiKeyId());
            else ps.setNull(2, Types.BIGINT);
            ps.setString(3, log.getApiKey());
            ps.setString(4, log.getEndpoint());
            ps.setString(5, log.getMethod());
            ps.setString(6, truncate(log.getRequestBody(), 4000));
            if (log.getResponseCode() != null) ps.setInt(7, log.getResponseCode());
            else ps.setNull(7, Types.INTEGER);
            ps.setString(8, truncate(log.getResponseBody(), 4000));
            ps.setString(9, log.getClientIp());
            if (log.getDurationMs() != null) ps.setLong(10, log.getDurationMs());
            else ps.setNull(10, Types.BIGINT);
            ps.setString(11, log.getStatus() != null ? log.getStatus() : "success");
            ps.setString(12, truncate(log.getErrorMessage(), 1000));
            return ps;
        }, holder);
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    public List<ApiCallLog> findByProjectId(Long projectId, int offset, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM api_call_logs WHERE project_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
            rowMapper, projectId, limit, offset);
    }

    public List<ApiCallLog> findByApiKey(String apiKey, int offset, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM api_call_logs WHERE api_key = ? ORDER BY id DESC LIMIT ? OFFSET ?",
            rowMapper, apiKey, limit, offset);
    }

    public int countByProjectId(Long projectId) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM api_call_logs WHERE project_id = ?", Integer.class, projectId);
        return c != null ? c : 0;
    }

    private final RowMapper<ApiCallLog> rowMapper = (rs, rowNum) -> {
        ApiCallLog l = new ApiCallLog();
        l.setId(rs.getLong("id"));
        l.setProjectId(rs.getLong("project_id"));
        long keyId = rs.getLong("api_key_id");
        if (!rs.wasNull()) l.setApiKeyId(keyId);
        l.setApiKey(rs.getString("api_key"));
        l.setEndpoint(rs.getString("endpoint"));
        l.setMethod(rs.getString("method"));
        l.setRequestBody(rs.getString("request_body"));
        int code = rs.getInt("response_code");
        if (!rs.wasNull()) l.setResponseCode(code);
        l.setResponseBody(rs.getString("response_body"));
        l.setClientIp(rs.getString("client_ip"));
        long dur = rs.getLong("duration_ms");
        if (!rs.wasNull()) l.setDurationMs(dur);
        l.setStatus(rs.getString("status"));
        l.setErrorMessage(rs.getString("error_message"));
        if (rs.getTimestamp("created_at") != null)
            l.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return l;
    };

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
