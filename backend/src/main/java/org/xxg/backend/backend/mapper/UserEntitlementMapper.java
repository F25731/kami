package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.UserEntitlement;

import java.sql.*;
import java.util.List;

@Repository
public class UserEntitlementMapper {

    private final JdbcTemplate jdbcTemplate;

    public UserEntitlementMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserEntitlement findById(Long id) {
        List<UserEntitlement> list = jdbcTemplate.query(
            "SELECT * FROM user_entitlements WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<UserEntitlement> findByProjectIdAndUserId(Long projectId, String userId) {
        return jdbcTemplate.query(
            "SELECT * FROM user_entitlements WHERE project_id = ? AND user_id = ? ORDER BY id DESC",
            rowMapper, projectId, userId);
    }

    public UserEntitlement findActiveByProjectIdAndUserIdAndType(Long projectId, String userId, String entitlementType) {
        List<UserEntitlement> list = jdbcTemplate.query(
            "SELECT * FROM user_entitlements WHERE project_id = ? AND user_id = ? " +
            "AND entitlement_type = ? AND status = 'active' " +
            "AND (is_permanent = 1 OR expire_time > NOW()) " +
            "ORDER BY expire_time IS NULL DESC, expire_time DESC LIMIT 1",
            rowMapper, projectId, userId, entitlementType);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<UserEntitlement> findByProjectId(Long projectId, int offset, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM user_entitlements WHERE project_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
            rowMapper, projectId, limit, offset);
    }

    public int countByProjectId(Long projectId) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_entitlements WHERE project_id = ?", Integer.class, projectId);
        return c != null ? c : 0;
    }

    public UserEntitlement findBySourceCardId(Long cardId) {
        List<UserEntitlement> list = jdbcTemplate.query(
            "SELECT * FROM user_entitlements WHERE source_card_id = ? LIMIT 1", rowMapper, cardId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long insert(UserEntitlement e) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO user_entitlements " +
                "(project_id, user_id, entitlement_type, total_count, remaining_count, " +
                " start_time, expire_time, is_permanent, source_card_id, source_order_no, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, e.getProjectId());
            ps.setString(2, e.getUserId());
            ps.setString(3, e.getEntitlementType());
            ps.setInt(4, e.getTotalCount() != null ? e.getTotalCount() : 0);
            ps.setInt(5, e.getRemainingCount() != null ? e.getRemainingCount() : 0);
            if (e.getStartTime() != null) ps.setTimestamp(6, Timestamp.valueOf(e.getStartTime()));
            else ps.setNull(6, Types.TIMESTAMP);
            if (e.getExpireTime() != null) ps.setTimestamp(7, Timestamp.valueOf(e.getExpireTime()));
            else ps.setNull(7, Types.TIMESTAMP);
            ps.setInt(8, Boolean.TRUE.equals(e.getIsPermanent()) ? 1 : 0);
            if (e.getSourceCardId() != null) ps.setLong(9, e.getSourceCardId());
            else ps.setNull(9, Types.BIGINT);
            ps.setString(10, e.getSourceOrderNo());
            ps.setString(11, e.getStatus() != null ? e.getStatus() : "active");
            return ps;
        }, holder);
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    /**
     * Atomically decrement remaining_count with optimistic check.
     * Returns 1 if decremented successfully, 0 if not enough remaining.
     */
    public int decrementRemainingCount(Long id, int amount, int expectedMin) {
        return jdbcTemplate.update(
            "UPDATE user_entitlements SET remaining_count = remaining_count - ? " +
            "WHERE id = ? AND remaining_count >= ? AND status = 'active'",
            amount, id, expectedMin);
    }

    /**
     * Refund: atomically increment remaining_count up to total_count.
     */
    public int incrementRemainingCount(Long id, int amount) {
        return jdbcTemplate.update(
            "UPDATE user_entitlements SET remaining_count = LEAST(remaining_count + ?, total_count) " +
            "WHERE id = ? AND status = 'active'",
            amount, id);
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE user_entitlements SET status=? WHERE id=?", status, id);
    }

    private final RowMapper<UserEntitlement> rowMapper = (rs, rowNum) -> {
        UserEntitlement e = new UserEntitlement();
        e.setId(rs.getLong("id"));
        e.setProjectId(rs.getLong("project_id"));
        e.setUserId(rs.getString("user_id"));
        e.setEntitlementType(rs.getString("entitlement_type"));
        e.setTotalCount(rs.getInt("total_count"));
        e.setRemainingCount(rs.getInt("remaining_count"));
        if (rs.getTimestamp("start_time") != null)
            e.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        if (rs.getTimestamp("expire_time") != null)
            e.setExpireTime(rs.getTimestamp("expire_time").toLocalDateTime());
        e.setIsPermanent(rs.getInt("is_permanent") == 1);
        long sourceCardId = rs.getLong("source_card_id");
        if (!rs.wasNull()) e.setSourceCardId(sourceCardId);
        e.setSourceOrderNo(rs.getString("source_order_no"));
        e.setStatus(rs.getString("status"));
        if (rs.getTimestamp("created_at") != null)
            e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null)
            e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return e;
    };
}
