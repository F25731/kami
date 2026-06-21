package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.Order;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class OrderMapper {

    private final JdbcTemplate jdbcTemplate;

    public OrderMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int insert(Order order) {
        String sql = "INSERT INTO orders (project_id, order_no, user_id, username, card_type, card_spec, quantity, unit_price, total_price, status, source, external_order_no, package_id, card_keys, create_time, pay_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, order.getProjectId());
            ps.setString(2, order.getOrderNo());
            ps.setObject(3, order.getUserId());
            ps.setString(4, order.getUsername());
            ps.setString(5, order.getCardType());
            ps.setString(6, order.getCardSpec());
            ps.setInt(7, order.getQuantity());
            ps.setBigDecimal(8, order.getUnitPrice());
            ps.setBigDecimal(9, order.getTotalPrice());
            ps.setString(10, order.getStatus());
            ps.setString(11, order.getSource());
            ps.setString(12, order.getExternalOrderNo());
            ps.setObject(13, order.getPackageId());
            ps.setString(14, order.getCardKeys());
            ps.setTimestamp(15, Timestamp.valueOf(order.getCreateTime()));
            ps.setTimestamp(16, order.getPayTime() != null ? Timestamp.valueOf(order.getPayTime()) : null);
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    private final org.springframework.jdbc.core.RowMapper<Order> rowMapper = (rs, rowNum) -> {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setProjectId(getLong(rs, "project_id"));
        order.setOrderNo(rs.getString("order_no"));
        order.setUserId(rs.getInt("user_id"));
        order.setUsername(rs.getString("username"));
        order.setCardType(rs.getString("card_type"));
        order.setCardSpec(rs.getString("card_spec"));
        order.setQuantity(rs.getInt("quantity"));
        order.setUnitPrice(rs.getBigDecimal("unit_price"));
        order.setTotalPrice(rs.getBigDecimal("total_price"));
        order.setStatus(rs.getString("status"));
        order.setSource(getString(rs, "source"));
        order.setExternalOrderNo(getString(rs, "external_order_no"));
        order.setPackageId(getLong(rs, "package_id"));
        order.setCardKeys(rs.getString("card_keys"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) order.setCreateTime(createTime.toLocalDateTime());
        Timestamp finishTime = rs.getTimestamp("pay_time");
        if (finishTime != null) order.setPayTime(finishTime.toLocalDateTime());
        return order;
    };

    public Order findByOrderNo(String orderNo) {
        String sql = "SELECT * FROM orders WHERE order_no = ?";
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, orderNo);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Order> search(String orderNo, String username, String status, String cardType, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder("SELECT * FROM orders WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (orderNo != null && !orderNo.isEmpty()) {
            sql.append(" AND (order_no LIKE ? OR external_order_no LIKE ?)");
            params.add("%" + orderNo + "%");
            params.add("%" + orderNo + "%");
        }
        if (username != null && !username.isEmpty()) {
            sql.append(" AND username LIKE ?");
            params.add("%" + username + "%");
        }
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (cardType != null && !cardType.isEmpty() && !"all".equals(cardType)) {
            sql.append(" AND card_type = ?");
            params.add(cardType);
        }
        if (startTime != null && !startTime.isEmpty()) {
            sql.append(" AND create_time >= ?");
            params.add(Timestamp.valueOf(startTime + " 00:00:00"));
        }
        if (endTime != null && !endTime.isEmpty()) {
            sql.append(" AND create_time <= ?");
            params.add(Timestamp.valueOf(endTime + " 23:59:59"));
        }

        sql.append(" ORDER BY create_time DESC");
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public List<Order> findByUserId(Integer userId) {
        return jdbcTemplate.query("SELECT * FROM orders WHERE user_id = ? ORDER BY create_time DESC", rowMapper, userId);
    }

    public List<Order> findAll() {
        return jdbcTemplate.query("SELECT * FROM orders ORDER BY create_time DESC", rowMapper);
    }

    public int update(Order order) {
        String sql = "UPDATE orders SET status = ?, pay_time = ?, card_keys = ?, source = ?, external_order_no = ?, package_id = ? WHERE order_no = ?";
        return jdbcTemplate.update(sql,
                order.getStatus(),
                order.getPayTime() != null ? Timestamp.valueOf(order.getPayTime()) : null,
                order.getCardKeys(),
                order.getSource(),
                order.getExternalOrderNo(),
                order.getPackageId(),
                order.getOrderNo());
    }

    public int updateStatus(String orderNo, String status) {
        return jdbcTemplate.update("UPDATE orders SET status = ? WHERE order_no = ?", status, orderNo);
    }

    public Order findByCardKey(String cardKey) {
        String sql = "SELECT * FROM orders WHERE card_keys LIKE ? LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, "%" + cardKey + "%");
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    private String getString(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        try {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }
}