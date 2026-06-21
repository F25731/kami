package org.xxg.backend.backend.mapper;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.xxg.backend.backend.entity.Card;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 鍗″瘑鏁版嵁璁块棶灞?
 */
@Repository
public class CardMapper {

    private final JdbcTemplate jdbcTemplate;

    public CardMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureColumnsExist();
    }

    private void ensureColumnsExist() {
                try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN project_id BIGINT");
        } catch (Exception e) {
            // Ignore if column exists
        }
try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN device_id VARCHAR(255)");
        } catch (Exception e) {
            // Ignore if column exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN ip_address VARCHAR(255)");
        } catch (Exception e) {
            // Ignore if column exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN api_key_id BIGINT");
        } catch (Exception e) {
            // Ignore if column exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN machine_code VARCHAR(255)");
        } catch (Exception e) {
            // Ignore if column exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE cards ADD INDEX idx_machine_code (machine_code)");
        } catch (Exception e) {
            // Ignore if index exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN stack_time_if_same_machine TINYINT(1) NOT NULL DEFAULT 0");
        } catch (Exception e) {
            // exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN merged_into_card_id BIGINT NULL");
        } catch (Exception e) {
            // exists
        }
        try {
            jdbcTemplate.execute("ALTER TABLE cards ADD COLUMN allow_self_unbind TINYINT(1) NOT NULL DEFAULT 0 COMMENT '鍏佽鐢ㄦ埛鍦ㄩ椤佃嚜鍔╄В缁戞満鍣ㄧ爜'");
        } catch (Exception e) {
            // exists
        }
        System.out.println("Successfully updated cards table columns.");
    }

    /**
     * 鏇存柊鍗″瘑鐘舵€?
     * @param ids 鍗″瘑ID鍒楄〃
     * @param status 鏂扮姸鎬?
     */
    public void updateStatus(List<Long> ids, int status) {
        if (ids == null || ids.isEmpty()) return;
        
        String sql = "UPDATE cards SET status = ? WHERE id IN (" + 
                     String.join(",", java.util.Collections.nCopies(ids.size(), "?")) + ")";
        
        Object[] args = new Object[ids.size() + 1];
        args[0] = status;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 1] = ids.get(i);
        }
        
        jdbcTemplate.update(sql, args);
    }

    /**
     * 缁熻鎵€鏈夊崱瀵嗘暟閲?
     */
    public int countTotalCards() {
        String sql = "SELECT COUNT(*) FROM cards";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * 缁熻宸蹭娇鐢ㄥ崱瀵嗘暟閲?
     */
    public int countUsedCards() {
        String sql = "SELECT COUNT(*) FROM cards WHERE status IN (1, 4)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * 缁熻鏈夋晥鍗″瘑鏁伴噺
     */
    public int countActiveCards() {
        String sql = "SELECT COUNT(*) FROM cards WHERE status = 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    public int countTotalCardsByProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cards WHERE project_id = ?", Integer.class, projectId);
        return count != null ? count : 0;
    }

    public int countUsedCardsByProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cards WHERE project_id = ? AND status IN (1, 4)", Integer.class, projectId);
        return count != null ? count : 0;
    }

    public int countActiveCardsByProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cards WHERE project_id = ? AND status = 0", Integer.class, projectId);
        return count != null ? count : 0;
    }

    /**
     * 鑾峰彇鏈€杩慛澶╃殑鍗″瘑浣跨敤瓒嬪娍
     * @param days 澶╂暟
     * @return 姣忔棩浣跨敤鏁伴噺鍒楄〃
     */
    public List<Map<String, Object>> getUsageTrend(int days) {
        String sql = "SELECT DATE(use_time) as date, COUNT(*) as count " +
                     "FROM cards " +
                     "WHERE use_time >= DATE_SUB(CURDATE(), INTERVAL ? DAY) " +
                     "AND status IN (1, 4) " +
                     "GROUP BY DATE(use_time) " +
                     "ORDER BY date ASC";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", rs.getDate("date").toString());
            map.put("count", rs.getInt("count"));
            return map;
        }, days);
    }

    /**
     * 鏌ユ壘鍙敤鍗″瘑
     * @param type 鍗″瘑绫诲瀷 (time/count)
     * @param value 瑙勬牸鍊?(duration or total_count)
     * @param limit 鏁伴噺
     * @return 鍗″瘑鍒楄〃
     */
    public List<Card> findAvailableCards(String type, int value, int limit) {
        String sql;
        if ("time".equals(type)) {
            sql = "SELECT * FROM cards WHERE card_type = ? AND duration = ? AND status = 0 LIMIT ?";
        } else {
            sql = "SELECT * FROM cards WHERE card_type = ? AND total_count = ? AND status = 0 LIMIT ?";
        }
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Card card = new Card();
            card.setId(rs.getLong("id"));
            try {
                long projectId = rs.getLong("project_id");
                if (!rs.wasNull()) card.setProjectId(projectId);
            } catch (SQLException ignored) {
            }
            card.setCardKey(rs.getString("card_key"));
            // ... other fields if needed
            return card;
        }, type, value, limit);
    }

    /**
     * 鏇存柊鍗″瘑浣跨敤淇℃伅
     * @param id 鍗″瘑ID
     * @param useTime 浣跨敤鏃堕棿
     * @param deviceId 璁惧ID
     * @param ipAddress IP鍦板潃
     */
    public void updateUsage(Long id, java.time.LocalDateTime useTime, String deviceId, String ipAddress) {
        String sql = "UPDATE cards SET status = 1, use_time = ?, device_id = ?, ip_address = ? WHERE id = ?";
        jdbcTemplate.update(sql, Timestamp.valueOf(useTime), deviceId, ipAddress, id);
    }

    /**
     * 鎸?encrypted_key 鍚屾涓昏〃锛堥珮绾у崱瀵嗘牳閿€鐢級銆傛椂闂村崱闇€鍐欏叆 expire_time锛岀鐞嗙鍒楄〃涓庡€掕鏃朵緷璧栬瀛楁銆?
     */
    public void updateUsageByHash(String cardHash, java.time.LocalDateTime useTime, int status, int remainingCount,
                                  java.time.LocalDateTime expireTime, String machineCode) {
        String sql = "UPDATE cards SET status = ?, use_time = ?, remaining_count = ?, expire_time = ?, machine_code = ? WHERE encrypted_key = ?";
        jdbcTemplate.update(sql, status, Timestamp.valueOf(useTime), remainingCount,
                expireTime != null ? Timestamp.valueOf(expireTime) : null, machineCode, cardHash);
    }

    /**
     * 鏇存柊鍗″瘑淇℃伅
     */
    public void update(Card card) {
        String sql = "UPDATE cards SET status = ?, use_time = ?, expire_time = ?, remaining_count = ?, " +
                     "device_id = ?, ip_address = ?, machine_code = ?, duration = ?, total_count = ?, allow_reverify = ?, " +
                     "stack_time_if_same_machine = ?, allow_self_unbind = ?, merged_into_card_id = ?, " +
                     "package_id = ?, order_no = ?, source = ?, bind_device_id = ?, bind_time = ?, bind_type = ?, " +
                     "redeemed_user_id = ?, redeemed_at = ?, activated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
            card.getStatus(),
            card.getUseTime() != null ? Timestamp.valueOf(card.getUseTime()) : null,
            card.getExpireTime() != null ? Timestamp.valueOf(card.getExpireTime()) : null,
            card.getRemainingCount(),
            card.getDeviceId(),
            card.getIpAddress(),
            card.getMachineCode(),
            card.getDuration(),
            card.getTotalCount(),
            card.getAllowReverify(),
            Boolean.TRUE.equals(card.getStackTimeIfSameMachine()) ? 1 : 0,
            Boolean.TRUE.equals(card.getAllowSelfUnbind()) ? 1 : 0,
            card.getMergedIntoCardId(),
            card.getPackageId(),
            card.getOrderNo(),
            card.getSource(),
            card.getBindDeviceId(),
            card.getBindTime() != null ? Timestamp.valueOf(card.getBindTime()) : null,
            card.getBindType(),
            card.getRedeemedUserId(),
            card.getRedeemedAt() != null ? Timestamp.valueOf(card.getRedeemedAt()) : null,
            card.getActivatedAt() != null ? Timestamp.valueOf(card.getActivatedAt()) : null,
            card.getId()
        );
    }

    /**
     * 鏍规嵁鍗″瘑鏌ユ壘
     * @param cardKey 鍗″瘑
     * @return Card瀵硅薄
     */
    public Card findByCardKey(String cardKey) {
        String sql = "SELECT * FROM cards WHERE card_key = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new CardRowMapper(), cardKey);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    /**
     * 鏍规嵁鍗″瘑鍒楄〃鏌ユ壘
     * @param cardKeys 鍗″瘑鍒楄〃
     * @return Card瀵硅薄鍒楄〃
     */
    public Card findByProjectIdAndCardKey(Long projectId, String cardKey) {
        String sql = "SELECT * FROM cards WHERE project_id = ? AND card_key = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new CardRowMapper(), projectId, cardKey);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Card> findByCardKeys(List<String> cardKeys) {
        if (cardKeys == null || cardKeys.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = "SELECT * FROM cards WHERE card_key IN (" + 
                     String.join(",", Collections.nCopies(cardKeys.size(), "?")) + ")";
        return jdbcTemplate.query(sql, new CardRowMapper(), cardKeys.toArray());
    }

    /**
     * 鎵归噺鎻掑叆鍗″瘑
     */
    public void batchInsert(List<Card> cards) {
        String sql = "INSERT INTO cards (project_id, card_key, encrypted_key, card_type, duration, total_count, remaining_count, status, verify_method, encryption_type, allow_reverify, create_time, creator_type, creator_id, creator_name, api_key_id, stack_time_if_same_machine, allow_self_unbind, merged_into_card_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Card card = cards.get(i);
                if (card.getProjectId() != null) ps.setLong(1, card.getProjectId()); else ps.setNull(1, java.sql.Types.BIGINT);
                ps.setString(2, card.getCardKey());
                ps.setString(3, card.getEncryptedKey());
                ps.setString(4, card.getCardType());
                ps.setInt(5, card.getDuration());
                ps.setInt(6, card.getTotalCount());
                ps.setInt(7, card.getRemainingCount());
                ps.setInt(8, card.getStatus());
                ps.setString(9, card.getVerifyMethod());
                ps.setString(10, card.getEncryptionType());
                ps.setInt(11, card.getAllowReverify());
                ps.setTimestamp(12, Timestamp.valueOf(card.getCreateTime()));
                ps.setString(13, card.getCreatorType());
                ps.setLong(14, card.getCreatorId());
                ps.setString(15, card.getCreatorName());
                if (card.getApiKeyId() != null) ps.setLong(16, card.getApiKeyId()); else ps.setNull(16, java.sql.Types.BIGINT);
                ps.setInt(17, Boolean.TRUE.equals(card.getStackTimeIfSameMachine()) ? 1 : 0);
                ps.setInt(18, Boolean.TRUE.equals(card.getAllowSelfUnbind()) ? 1 : 0);
                if (card.getMergedIntoCardId() != null) ps.setLong(19, card.getMergedIntoCardId()); else ps.setNull(19, java.sql.Types.BIGINT);
            }

            @Override
            public int getBatchSize() {
                return cards.size();
            }
        });
    }

    public List<Card> findByApiKeyId(Long apiKeyId) {
        String sql = "SELECT * FROM cards WHERE api_key_id = ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new CardRowMapper(), apiKeyId);
    }

    public List<Card> findAll() {
        String sql = "SELECT * FROM cards ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new CardRowMapper());
    }

    public List<Card> findByProjectId(Long projectId) {
        String sql = "SELECT * FROM cards WHERE project_id = ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new CardRowMapper(), projectId);
    }

    /**
     * 鏌ユ壘鍚屾満鍣ㄧ爜涓婂彲浣滀负銆屾椂闀垮彔鍔犻敋鐐广€嶇殑鍊欓€夋椂闂村崱锛堥渶缁撳悎 card_status / 鏈夋晥鏈熷湪涓氬姟灞傜瓫閫夛級銆?
     */
    public List<Card> listStackAnchorCandidates(String machineCode, Long excludeCardId) {
        if (machineCode == null || machineCode.isEmpty()) {
            return new ArrayList<>();
        }
        Long ex = excludeCardId != null ? excludeCardId : -1L;
        String sql = "SELECT * FROM cards WHERE machine_code = ? AND card_type = 'time' AND status = 1 AND id <> ?";
        return jdbcTemplate.query(sql, new CardRowMapper(), machineCode, ex);
    }

    public void updateExpireTimeById(Long cardId, java.time.LocalDateTime expireTime) {
        jdbcTemplate.update("UPDATE cards SET expire_time = ? WHERE id = ?",
                expireTime != null ? Timestamp.valueOf(expireTime) : null, cardId);
    }

    /** 鏍搁攢銆岀画鏈熷彔鍔犮€嶇殑鍗★細鏍囪鍚堝苟鐘舵€佸苟璁板綍缁湡鍒板摢寮犱富鍗?*/
    public void markCardMergedInto(Long consumedCardId, Long anchorCardId, java.time.LocalDateTime useTime,
                                   String machineCode, String deviceId, String ipAddress) {
        jdbcTemplate.update(
                "UPDATE cards SET status = 4, merged_into_card_id = ?, use_time = ?, expire_time = NULL, machine_code = ?, device_id = ?, ip_address = ? WHERE id = ?",
                anchorCardId,
                Timestamp.valueOf(useTime),
                machineCode,
                deviceId,
                ipAddress,
                consumedCardId);
    }

    public void delete(Long id) {
        jdbcTemplate.update("DELETE FROM cards WHERE id = ?", id);
    }

    /** 浠呮洿鏂?status锛岀敤浜庣鐞嗗憳鍚敤/鏆傚仠 */
    public void updateStatusOnly(Long id, int status) {
        jdbcTemplate.update("UPDATE cards SET status = ? WHERE id = ?", status, id);
    }

    /** 鎸?encrypted_key 琛ュ啓 expire_time锛堥珮绾ф椂闂村崱鍘嗗彶鏁版嵁淇锛?*/
    public void updateExpireTimeByHash(String cardHash, java.time.LocalDateTime expireTime) {
        jdbcTemplate.update(
                "UPDATE cards SET expire_time = ? WHERE encrypted_key = ?",
                expireTime != null ? Timestamp.valueOf(expireTime) : null,
                cardHash);
    }

    public Card findById(Long id) {
        String sql = "SELECT * FROM cards WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new CardRowMapper(), id);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static class CardRowMapper implements RowMapper<Card> {
        @Override
        public Card mapRow(ResultSet rs, int rowNum) throws SQLException {
            Card card = new Card();
            card.setId(rs.getLong("id"));
            try {
                long projectId = rs.getLong("project_id");
                if (!rs.wasNull()) card.setProjectId(projectId);
            } catch (SQLException ignored) {
            }
            card.setCardKey(rs.getString("card_key"));
            card.setEncryptedKey(rs.getString("encrypted_key"));
            card.setCardType(rs.getString("card_type"));
            card.setDuration(rs.getInt("duration"));
            card.setTotalCount(rs.getInt("total_count"));
            card.setRemainingCount(rs.getInt("remaining_count"));
            card.setStatus(rs.getInt("status"));
            card.setVerifyMethod(rs.getString("verify_method"));
            card.setEncryptionType(rs.getString("encryption_type"));
            card.setAllowReverify(rs.getInt("allow_reverify"));
            if (rs.getTimestamp("create_time") != null) {
                card.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            }
            if (rs.getTimestamp("use_time") != null) {
                card.setUseTime(rs.getTimestamp("use_time").toLocalDateTime());
            }
            if (rs.getTimestamp("expire_time") != null) {
                card.setExpireTime(rs.getTimestamp("expire_time").toLocalDateTime());
            }
            card.setCreatorType(rs.getString("creator_type"));
            card.setCreatorId(rs.getLong("creator_id"));
            card.setCreatorName(rs.getString("creator_name"));
            try {
                card.setDeviceId(rs.getString("device_id"));
                card.setIpAddress(rs.getString("ip_address"));
            } catch (SQLException e) {
                // Ignore
            }
            try {
                long apiKeyId = rs.getLong("api_key_id");
                if (!rs.wasNull()) {
                    card.setApiKeyId(apiKeyId);
                }
            } catch (SQLException e) {
                // Ignore
            }
            try {
                card.setMachineCode(rs.getString("machine_code"));
            } catch (SQLException e) {
                // Ignore
            }
            try {
                card.setStackTimeIfSameMachine(rs.getInt("stack_time_if_same_machine") == 1);
            } catch (SQLException ignored) {
            }
            try {
                card.setAllowSelfUnbind(rs.getInt("allow_self_unbind") == 1);
            } catch (SQLException ignored) {
            }
            try {
                long merged = rs.getLong("merged_into_card_id");
                if (!rs.wasNull()) {
                    card.setMergedIntoCardId(merged);
                }
            } catch (SQLException ignored) {
            }
            // ─── multi-project new fields ───────────────────────────────────────
            try {
                long pkgId = rs.getLong("package_id");
                if (!rs.wasNull()) card.setPackageId(pkgId);
            } catch (SQLException ignored) {}
            try { card.setOrderNo(rs.getString("order_no")); } catch (SQLException ignored) {}
            try { card.setSource(rs.getString("source")); } catch (SQLException ignored) {}
            try { card.setBindDeviceId(rs.getString("bind_device_id")); } catch (SQLException ignored) {}
            try {
                if (rs.getTimestamp("bind_time") != null)
                    card.setBindTime(rs.getTimestamp("bind_time").toLocalDateTime());
            } catch (SQLException ignored) {}
            try { card.setBindType(rs.getString("bind_type")); } catch (SQLException ignored) {}
            try { card.setRedeemedUserId(rs.getString("redeemed_user_id")); } catch (SQLException ignored) {}
            try {
                if (rs.getTimestamp("redeemed_at") != null)
                    card.setRedeemedAt(rs.getTimestamp("redeemed_at").toLocalDateTime());
            } catch (SQLException ignored) {}
            try {
                if (rs.getTimestamp("activated_at") != null)
                    card.setActivatedAt(rs.getTimestamp("activated_at").toLocalDateTime());
            } catch (SQLException ignored) {}
            return card;
        }
    }
}

