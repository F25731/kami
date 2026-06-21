package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Map;

/**
 * Handles both card_consume_logs and entitlement_consume_logs.
 * Each method targets the appropriate table explicitly.
 */
@Repository
public class ConsumeLogMapper {

    private final JdbcTemplate jdbcTemplate;

    public ConsumeLogMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ─── card_consume_logs ───────────────────────────────────────────────────

    public Long insertCardConsumeLog(Long projectId, Long cardId, String bizId,
                                     String deviceId, String userId, String action,
                                     String clientIp) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO card_consume_logs " +
                "(project_id, card_id, biz_id, device_id, user_id, action, client_ip, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setLong(2, cardId);
            ps.setString(3, bizId);
            ps.setString(4, deviceId);
            ps.setString(5, userId);
            ps.setString(6, action);
            ps.setString(7, clientIp);
            return ps;
        }, holder);
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    /**
     * Idempotency check for consume: find by (project_id, card_id, biz_id).
     */
    public Map<String, Object> findCardConsumeLog(Long projectId, Long cardId, String bizId) {
        var list = jdbcTemplate.queryForList(
            "SELECT * FROM card_consume_logs WHERE project_id = ? AND card_id = ? AND biz_id = ? LIMIT 1",
            projectId, cardId, bizId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ─── entitlement_consume_logs ────────────────────────────────────────────

    public Long insertEntitlementConsumeLog(Long projectId, Long entitlementId, String bizId,
                                            String userId, int amount, String clientIp) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO entitlement_consume_logs " +
                "(project_id, entitlement_id, biz_id, user_id, amount, client_ip, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, projectId);
            ps.setLong(2, entitlementId);
            ps.setString(3, bizId);
            ps.setString(4, userId);
            ps.setInt(5, amount);
            ps.setString(6, clientIp);
            return ps;
        }, holder);
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    /**
     * Idempotency check for entitlement consume: find by (project_id, entitlement_id, biz_id).
     */
    public Map<String, Object> findEntitlementConsumeLog(Long projectId, Long entitlementId, String bizId) {
        var list = jdbcTemplate.queryForList(
            "SELECT * FROM entitlement_consume_logs " +
            "WHERE project_id = ? AND entitlement_id = ? AND biz_id = ? LIMIT 1",
            projectId, entitlementId, bizId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Find any entitlement consume log for a biz_id across all entitlements in a project.
     * Used for cross-entitlement deduplication.
     */
    public Map<String, Object> findEntitlementConsumeLogByBizId(Long projectId, String bizId) {
        var list = jdbcTemplate.queryForList(
            "SELECT * FROM entitlement_consume_logs WHERE project_id = ? AND biz_id = ? LIMIT 1",
            projectId, bizId);
        return list.isEmpty() ? null : list.get(0);
    }

    public void markCardConsumeRefunded(Long logId) {
        jdbcTemplate.update(
            "UPDATE card_consume_logs SET refunded = 1 WHERE id = ?", logId);
    }

    public void markEntitlementConsumeRefunded(Long logId) {
        jdbcTemplate.update(
            "UPDATE entitlement_consume_logs SET refunded = 1 WHERE id = ?", logId);
    }
}
