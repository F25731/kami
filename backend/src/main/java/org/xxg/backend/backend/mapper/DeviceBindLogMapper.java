package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.DeviceBindLog;

import java.sql.*;
import java.util.List;

@Repository
public class DeviceBindLogMapper {

    private final JdbcTemplate jdbcTemplate;

    public DeviceBindLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(DeviceBindLog log) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO device_bind_logs " +
                "(project_id, card_id, device_id, action, reason, operator_type, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, log.getProjectId());
            ps.setLong(2, log.getCardId());
            ps.setString(3, log.getDeviceId());
            ps.setString(4, log.getAction());
            ps.setString(5, log.getReason());
            ps.setString(6, log.getOperatorType() != null ? log.getOperatorType() : "api");
            return ps;
        }, holder);
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    public List<DeviceBindLog> findByCardId(Long cardId) {
        return jdbcTemplate.query(
            "SELECT * FROM device_bind_logs WHERE card_id = ? ORDER BY id DESC",
            rowMapper, cardId);
    }

    public List<DeviceBindLog> findByProjectId(Long projectId, int offset, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM device_bind_logs WHERE project_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
            rowMapper, projectId, limit, offset);
    }

    public int countByProjectIdAndCardId(Long projectId, Long cardId) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM device_bind_logs WHERE project_id = ? AND card_id = ?",
            Integer.class, projectId, cardId);
        return c != null ? c : 0;
    }

    private final org.springframework.jdbc.core.RowMapper<DeviceBindLog> rowMapper = (rs, rowNum) -> {
        DeviceBindLog l = new DeviceBindLog();
        l.setId(rs.getLong("id"));
        l.setProjectId(rs.getLong("project_id"));
        l.setCardId(rs.getLong("card_id"));
        l.setDeviceId(rs.getString("device_id"));
        l.setAction(rs.getString("action"));
        l.setReason(rs.getString("reason"));
        l.setOperatorType(rs.getString("operator_type"));
        if (rs.getTimestamp("created_at") != null)
            l.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return l;
    };
}
