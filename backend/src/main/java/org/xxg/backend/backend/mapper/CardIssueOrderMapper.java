package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.CardIssueOrder;

import java.sql.*;
import java.util.List;

@Repository
public class CardIssueOrderMapper {

    private final JdbcTemplate jdbcTemplate;

    public CardIssueOrderMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CardIssueOrder findById(Long id) {
        List<CardIssueOrder> list = jdbcTemplate.query(
            "SELECT * FROM card_issue_orders WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Idempotency check: find by (project_id, order_no) */
    public CardIssueOrder findByProjectIdAndOrderNo(Long projectId, String orderNo) {
        List<CardIssueOrder> list = jdbcTemplate.query(
            "SELECT * FROM card_issue_orders WHERE project_id = ? AND order_no = ?",
            rowMapper, projectId, orderNo);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<CardIssueOrder> findByProjectId(Long projectId, int offset, int limit) {
        return jdbcTemplate.query(
            "SELECT * FROM card_issue_orders WHERE project_id = ? ORDER BY id DESC LIMIT ? OFFSET ?",
            rowMapper, projectId, limit, offset);
    }

    public int countByProjectId(Long projectId) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM card_issue_orders WHERE project_id = ?", Integer.class, projectId);
        return c != null ? c : 0;
    }

    public Long insert(CardIssueOrder order) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO card_issue_orders " +
                "(project_id, order_no, package_id, quantity, card_type, count_value, " +
                " duration_days, is_permanent, remark, status, operator_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, order.getProjectId());
            ps.setString(2, order.getOrderNo());
            if (order.getPackageId() != null) ps.setLong(3, order.getPackageId());
            else ps.setNull(3, Types.BIGINT);
            ps.setInt(4, order.getQuantity() != null ? order.getQuantity() : 1);
            ps.setString(5, order.getCardType());
            ps.setInt(6, order.getCountValue() != null ? order.getCountValue() : 0);
            ps.setInt(7, order.getDurationDays() != null ? order.getDurationDays() : 0);
            ps.setInt(8, Boolean.TRUE.equals(order.getIsPermanent()) ? 1 : 0);
            ps.setString(9, order.getRemark());
            ps.setString(10, order.getStatus() != null ? order.getStatus() : "completed");
            if (order.getOperatorId() != null) ps.setLong(11, order.getOperatorId());
            else ps.setNull(11, Types.BIGINT);
            return ps;
        }, holder);
        return holder.getKey() != null ? holder.getKey().longValue() : null;
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE card_issue_orders SET status=? WHERE id=?", status, id);
    }

    private final RowMapper<CardIssueOrder> rowMapper = (rs, rowNum) -> {
        CardIssueOrder o = new CardIssueOrder();
        o.setId(rs.getLong("id"));
        o.setProjectId(rs.getLong("project_id"));
        o.setOrderNo(rs.getString("order_no"));
        long pkgId = rs.getLong("package_id");
        if (!rs.wasNull()) o.setPackageId(pkgId);
        o.setQuantity(rs.getInt("quantity"));
        o.setCardType(rs.getString("card_type"));
        o.setCountValue(rs.getInt("count_value"));
        o.setDurationDays(rs.getInt("duration_days"));
        o.setIsPermanent(rs.getInt("is_permanent") == 1);
        o.setRemark(rs.getString("remark"));
        o.setStatus(rs.getString("status"));
        long opId = rs.getLong("operator_id");
        if (!rs.wasNull()) o.setOperatorId(opId);
        if (rs.getTimestamp("created_at") != null)
            o.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return o;
    };
}
