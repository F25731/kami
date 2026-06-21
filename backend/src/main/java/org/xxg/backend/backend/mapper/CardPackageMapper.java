package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.CardPackage;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

@Repository
public class CardPackageMapper {

    private final JdbcTemplate jdbcTemplate;

    public CardPackageMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CardPackage> findAll() {
        return jdbcTemplate.query(
            "SELECT p.*, " +
            "(SELECT COUNT(*) FROM cards c WHERE c.package_id = p.id) AS card_count " +
            "FROM card_packages p ORDER BY p.project_id, p.sort, p.id",
            rowMapper);
    }

    public List<CardPackage> findByProjectId(Long projectId) {
        return jdbcTemplate.query(
            "SELECT p.*, " +
            "(SELECT COUNT(*) FROM cards c WHERE c.package_id = p.id) AS card_count " +
            "FROM card_packages p WHERE p.project_id = ? ORDER BY p.sort, p.id",
            rowMapper, projectId);
    }

    public List<CardPackage> findEnabledByProjectId(Long projectId) {
        return jdbcTemplate.query(
            "SELECT * FROM card_packages WHERE project_id = ? AND status = 'enabled' ORDER BY sort, id",
            rowMapper, projectId);
    }

    public CardPackage findById(Long id) {
        List<CardPackage> list = jdbcTemplate.query(
            "SELECT * FROM card_packages WHERE id = ?", rowMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public CardPackage findByProjectIdAndCode(Long projectId, String packageCode) {
        List<CardPackage> list = jdbcTemplate.query(
            "SELECT * FROM card_packages WHERE project_id = ? AND package_code = ?",
            rowMapper, projectId, packageCode);
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean existsByProjectIdAndCode(Long projectId, String packageCode) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM card_packages WHERE project_id = ? AND package_code = ?",
            Integer.class, projectId, packageCode);
        return c != null && c > 0;
    }

    public boolean existsByProjectIdAndCodeExcludeId(Long projectId, String packageCode, Long excludeId) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM card_packages WHERE project_id = ? AND package_code = ? AND id <> ?",
            Integer.class, projectId, packageCode, excludeId);
        return c != null && c > 0;
    }

    public Long insert(CardPackage pkg) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO card_packages " +
                "(project_id, package_name, package_code, card_type, count_value, duration_days, " +
                " is_permanent, price, status, sort, remark, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, pkg.getProjectId());
            ps.setString(2, pkg.getPackageName());
            ps.setString(3, pkg.getPackageCode());
            ps.setString(4, str(pkg.getCardType(), "count"));
            ps.setInt(5, pkg.getCountValue() != null ? pkg.getCountValue() : 0);
            ps.setInt(6, pkg.getDurationDays() != null ? pkg.getDurationDays() : 0);
            ps.setInt(7, bool(pkg.getIsPermanent()));
            if (pkg.getPrice() != null) ps.setBigDecimal(8, pkg.getPrice());
            else ps.setNull(8, Types.DECIMAL);
            ps.setString(9, str(pkg.getStatus(), "enabled"));
            ps.setInt(10, pkg.getSort() != null ? pkg.getSort() : 0);
            ps.setString(11, pkg.getRemark());
            return ps;
        }, keyHolder);
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    public void update(CardPackage pkg) {
        jdbcTemplate.update(
            "UPDATE card_packages SET package_name=?, package_code=?, card_type=?, count_value=?, " +
            "duration_days=?, is_permanent=?, price=?, status=?, sort=?, remark=? WHERE id=?",
            pkg.getPackageName(), pkg.getPackageCode(), str(pkg.getCardType(), "count"),
            pkg.getCountValue() != null ? pkg.getCountValue() : 0,
            pkg.getDurationDays() != null ? pkg.getDurationDays() : 0,
            bool(pkg.getIsPermanent()), pkg.getPrice(), str(pkg.getStatus(), "enabled"),
            pkg.getSort() != null ? pkg.getSort() : 0, pkg.getRemark(), pkg.getId());
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE card_packages SET status=? WHERE id=?", status, id);
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM card_packages WHERE id=?", id);
    }

    public int countCardsByPackageId(Long packageId) {
        Integer c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cards WHERE package_id=?", Integer.class, packageId);
        return c != null ? c : 0;
    }

    private final RowMapper<CardPackage> rowMapper = (rs, rowNum) -> {
        CardPackage p = new CardPackage();
        p.setId(rs.getLong("id"));
        p.setProjectId(rs.getLong("project_id"));
        p.setPackageName(rs.getString("package_name"));
        p.setPackageCode(rs.getString("package_code"));
        p.setCardType(rs.getString("card_type"));
        p.setCountValue(rs.getInt("count_value"));
        p.setDurationDays(rs.getInt("duration_days"));
        p.setIsPermanent(rs.getInt("is_permanent") == 1);
        BigDecimal price = rs.getBigDecimal("price");
        p.setPrice(price);
        p.setStatus(rs.getString("status"));
        p.setSort(rs.getInt("sort"));
        p.setRemark(rs.getString("remark"));
        if (rs.getTimestamp("created_at") != null)
            p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null)
            p.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        try { p.setCardCount(rs.getInt("card_count")); } catch (SQLException ignored) {}
        return p;
    };

    private int bool(Boolean v) { return Boolean.TRUE.equals(v) ? 1 : 0; }
    private String str(String v, String fallback) { return v == null || v.isBlank() ? fallback : v; }
}
