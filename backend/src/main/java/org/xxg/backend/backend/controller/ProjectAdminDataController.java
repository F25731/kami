package org.xxg.backend.backend.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/admin/projects/{projectId}")
@CrossOrigin
public class ProjectAdminDataController {
    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    public ProjectAdminDataController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@PathVariable Long projectId) {
        Map<String, Object> data = new LinkedHashMap<>();
        int totalCards = count("SELECT COUNT(*) FROM cards WHERE project_id = ?", projectId);
        int newCardsToday = count("SELECT COUNT(*) FROM cards WHERE project_id = ? AND DATE(create_time) = CURDATE()", projectId);
        int totalCalls = count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ?", projectId);
        int callsToday = count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND DATE(created_at) = CURDATE()", projectId);
        int successCalls = count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND (status = 'success' OR success = 1 OR response_code BETWEEN 200 AND 299)", projectId);
        Long avgLatency = queryLong("SELECT AVG(COALESCE(duration_ms, cost_ms, 0)) FROM api_call_logs WHERE project_id = ?", projectId);

        data.put("totalCards", totalCards);
        data.put("newCardsToday", newCardsToday);
        data.put("totalApiCalls", totalCalls);
        data.put("apiCallsToday", callsToday);
        data.put("successRate", totalCalls == 0 ? 100 : Math.round(successCalls * 10000.0 / totalCalls) / 100.0);
        data.put("avgLatency", avgLatency == null ? 0 : avgLatency);
        data.put("p50Latency", 0);
        data.put("p95Latency", 0);
        data.put("p99Latency", 0);
        return ok(data);
    }

    @GetMapping("/cards/usage")
    public Map<String, Object> cardUsage(@PathVariable Long projectId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unused", count("SELECT COUNT(*) FROM cards WHERE project_id = ? AND status = 0", projectId));
        data.put("active", count("SELECT COUNT(*) FROM cards WHERE project_id = ? AND status = 1", projectId));
        data.put("expired", count("SELECT COUNT(*) FROM cards WHERE project_id = ? AND status = 2", projectId));
        data.put("depleted", count("SELECT COUNT(*) FROM cards WHERE project_id = ? AND status IN (3, 4)", projectId));
        return ok(data);
    }

    @GetMapping("/cards")
    public Map<String, Object> cards(@PathVariable Long projectId,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) Long packageId) {
        StringBuilder where = new StringBuilder(" WHERE c.project_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(projectId);
        if (search != null && !search.isBlank()) {
            where.append(" AND c.card_key LIKE ?");
            args.add("%" + search + "%");
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND c.status = ?");
            args.add(Integer.parseInt(status));
        }
        if (packageId != null) {
            where.append(" AND c.package_id = ?");
            args.add(packageId);
        }
        int total = count("SELECT COUNT(*) FROM cards c" + where, args.toArray());
        args.add(Math.max(1, size));
        args.add(Math.max(0, (page - 1) * size));
        List<Map<String, Object>> list = query("SELECT c.*, p.package_name AS package_name, p.package_code AS package_code FROM cards c LEFT JOIN card_packages p ON p.id = c.package_id" + where + " ORDER BY c.create_time DESC LIMIT ? OFFSET ?", args.toArray());
        return ok(pageData(list, total));
    }

    @DeleteMapping("/cards/{cardId}")
    public Map<String, Object> deleteCard(@PathVariable Long projectId, @PathVariable Long cardId) {
        jdbcTemplate.update("DELETE FROM cards WHERE project_id = ? AND id = ?", projectId, cardId);
        return okMessage("Card deleted");
    }

    @PostMapping("/cards/{cardId}/unbind-device")
    public Map<String, Object> unbindCard(@PathVariable Long projectId, @PathVariable Long cardId) {
        jdbcTemplate.update("UPDATE cards SET device_id = NULL, machine_code = NULL, bind_device_id = NULL, bind_time = NULL WHERE project_id = ? AND id = ?", projectId, cardId);
        return okMessage("Device unbound");
    }

    @PostMapping("/cards/generate")
    public Map<String, Object> generateCards(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        Long packageId = asLong(body.get("packageId"));
        int quantity = Math.max(1, Math.min(1000, asInt(body.get("quantity"), 1)));
        Map<String, Object> pkg = queryOne("SELECT * FROM card_packages WHERE project_id = ? AND id = ?", projectId, packageId);
        if (pkg == null) return fail("Package not found");

        String cardType = str(pkg.get("cardType"), "count");
        int duration = asInt(pkg.get("durationDays"), 0);
        int countValue = asInt(pkg.get("countValue"), 0);
        LocalDateTime expireTime = parseDateTime(body.get("expireTime"));
        String prefix = str(body.get("prefix"), "");
        List<Object[]> rows = new ArrayList<>();
        List<String> cardKeys = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            String cardKey = (prefix == null || prefix.isBlank() ? "" : prefix + "-") + randomToken(16);
            cardKeys.add(cardKey);
            rows.add(new Object[]{projectId, cardKey, cardKey, cardType, duration, countValue, countValue, 0, expireTime == null ? null : Timestamp.valueOf(expireTime), "none", "plain", 0, Timestamp.valueOf(LocalDateTime.now()), "admin", 1L, "admin", packageId, str(body.get("orderNo"), null), "admin"});
        }
        jdbcTemplate.batchUpdate("INSERT INTO cards (project_id, card_key, encrypted_key, card_type, duration, total_count, remaining_count, status, expire_time, verify_method, encryption_type, allow_reverify, create_time, creator_type, creator_id, creator_name, package_id, order_no, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows);
        return ok(Map.of("count", quantity, "cards", cardKeys));
    }

    @GetMapping("/packages")
    public Map<String, Object> packages(@PathVariable Long projectId) {
        return ok(query("SELECT p.*, p.count_value AS total_count, p.duration_days AS total_days, p.remark AS description, (SELECT COUNT(*) FROM cards c WHERE c.package_id = p.id) AS card_count FROM card_packages p WHERE p.project_id = ? ORDER BY p.sort, p.id", projectId));
    }

    @PostMapping("/packages")
    public Map<String, Object> createPackage(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        String packageCode = str(body.get("packageCode"), null);
        if (packageCode == null) return fail("Package code is required");
        if (count("SELECT COUNT(*) FROM card_packages WHERE project_id = ? AND package_code = ?", projectId, packageCode) > 0) {
            return fail("Package code already exists");
        }
        jdbcTemplate.update("INSERT INTO card_packages (project_id, package_name, package_code, card_type, count_value, duration_days, is_permanent, price, status, sort, remark, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'enabled', 0, ?, NOW())",
                projectId, body.get("packageName"), packageCode, str(body.get("cardType"), "count"), asInt(body.get("totalCount"), 0), asInt(body.get("totalDays"), 0), 0, body.get("price"), body.get("description"));
        return okMessage("Package created");
    }

    @PutMapping("/packages/{packageId}")
    public Map<String, Object> updatePackage(@PathVariable Long projectId, @PathVariable Long packageId, @RequestBody Map<String, Object> body) {
        String packageCode = str(body.get("packageCode"), null);
        if (packageCode == null) return fail("Package code is required");
        if (count("SELECT COUNT(*) FROM card_packages WHERE project_id = ? AND package_code = ? AND id <> ?", projectId, packageCode, packageId) > 0) {
            return fail("Package code already exists");
        }
        jdbcTemplate.update("UPDATE card_packages SET package_name=?, package_code=?, card_type=?, count_value=?, duration_days=?, price=?, remark=? WHERE project_id=? AND id=?",
                body.get("packageName"), packageCode, str(body.get("cardType"), "count"), asInt(body.get("totalCount"), 0), asInt(body.get("totalDays"), 0), body.get("price"), body.get("description"), projectId, packageId);
        return okMessage("Package updated");
    }

    @DeleteMapping("/packages/{packageId}")
    public Map<String, Object> deletePackage(@PathVariable Long projectId, @PathVariable Long packageId) {
        jdbcTemplate.update("DELETE FROM card_packages WHERE project_id = ? AND id = ?", projectId, packageId);
        return okMessage("Package deleted");
    }

    @GetMapping("/orders")
    public Map<String, Object> orders(@PathVariable Long projectId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) Integer limit) {
        int actualSize = limit != null ? limit : size;
        int total = count("SELECT COUNT(*) FROM orders WHERE project_id = ?", projectId);
        List<Map<String, Object>> list = query("SELECT *, order_no AS orderNo, package_id AS packageId, create_time AS createdAt, pay_time AS completedAt FROM orders WHERE project_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?", projectId, actualSize, Math.max(0, (page - 1) * actualSize));
        return ok(limit != null ? list : pageData(list, total));
    }

    @GetMapping("/orders/{orderId}/cards")
    public Map<String, Object> orderCards(@PathVariable Long projectId, @PathVariable Long orderId) {
        return ok(query("SELECT * FROM cards WHERE project_id = ? AND order_no = (SELECT order_no FROM orders WHERE id = ? LIMIT 1)", projectId, orderId));
    }

    @GetMapping("/entitlements")
    public Map<String, Object> entitlements(@PathVariable Long projectId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        int total = count("SELECT COUNT(*) FROM user_entitlements WHERE project_id = ?", projectId);
        List<Map<String, Object>> list = query("SELECT *, total_count AS totalCount, remaining_count AS remainingCount, expire_time AS expireTime FROM user_entitlements WHERE project_id = ? ORDER BY id DESC LIMIT ? OFFSET ?", projectId, size, Math.max(0, (page - 1) * size));
        return ok(pageData(list, total));
    }

    @GetMapping("/entitlements/{id}/consume-logs")
    public Map<String, Object> entitlementLogs(@PathVariable Long projectId, @PathVariable Long id) {
        return ok(query("SELECT * FROM consume_logs WHERE project_id = ? ORDER BY id DESC LIMIT 100", projectId));
    }

    @GetMapping("/api-calls/stats")
    public Map<String, Object> apiCallStats(@PathVariable Long projectId) {
        int total = count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ?", projectId);
        int success = count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND (status = 'success' OR success = 1 OR response_code BETWEEN 200 AND 299)", projectId);
        Long avg = queryLong("SELECT AVG(COALESCE(duration_ms, cost_ms, 0)) FROM api_call_logs WHERE project_id = ?", projectId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCalls", total);
        data.put("successCalls", success);
        data.put("failedCalls", Math.max(0, total - success));
        data.put("avgLatency", avg == null ? 0 : avg);
        return ok(data);
    }

    @GetMapping("/api-calls")
    public Map<String, Object> apiCalls(@PathVariable Long projectId,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        int total = count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ?", projectId);
        List<Map<String, Object>> list = query("SELECT *, COALESCE(client_ip, request_ip) AS clientIp, COALESCE(duration_ms, cost_ms) AS durationMs FROM api_call_logs WHERE project_id = ? ORDER BY id DESC LIMIT ? OFFSET ?", projectId, size, Math.max(0, (page - 1) * size));
        return ok(pageData(list, total));
    }

    @GetMapping("/api-calls/trend")
    public Map<String, Object> apiCallTrend(@PathVariable Long projectId, @RequestParam(defaultValue = "24h") String range) {
        int days = "30d".equals(range) ? 30 : "7d".equals(range) ? 7 : 1;
        List<Map<String, Object>> rows = query("SELECT DATE_FORMAT(created_at, '%m-%d') AS label, COUNT(*) AS value FROM api_call_logs WHERE project_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) GROUP BY DATE_FORMAT(created_at, '%m-%d') ORDER BY MIN(created_at)", projectId, days);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("labels", rows.stream().map(r -> String.valueOf(r.get("label"))).toList());
        data.put("values", rows.stream().map(r -> r.get("value")).toList());
        return ok(data);
    }

    @GetMapping("/api-calls/latency-distribution")
    public Map<String, Object> latency(@PathVariable Long projectId) {
        List<String> ranges = List.of("0-50ms", "50-100ms", "100-200ms", "200-500ms", "500ms+");
        List<Integer> counts = List.of(
                count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND COALESCE(duration_ms, cost_ms, 0) < 50", projectId),
                count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND COALESCE(duration_ms, cost_ms, 0) >= 50 AND COALESCE(duration_ms, cost_ms, 0) < 100", projectId),
                count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND COALESCE(duration_ms, cost_ms, 0) >= 100 AND COALESCE(duration_ms, cost_ms, 0) < 200", projectId),
                count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND COALESCE(duration_ms, cost_ms, 0) >= 200 AND COALESCE(duration_ms, cost_ms, 0) < 500", projectId),
                count("SELECT COUNT(*) FROM api_call_logs WHERE project_id = ? AND COALESCE(duration_ms, cost_ms, 0) >= 500", projectId)
        );
        return ok(Map.of("ranges", ranges, "counts", counts));
    }

    private Map<String, Object> ok(Object data) {
        return Map.of("success", true, "data", data);
    }

    private Map<String, Object> okMessage(String message) {
        return Map.of("success", true, "message", message);
    }

    private Map<String, Object> fail(String message) {
        return Map.of("success", false, "message", message);
    }

    private Map<String, Object> pageData(List<Map<String, Object>> list, int total) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", total);
        return data;
    }

    private List<Map<String, Object>> query(String sql, Object... args) {
        try {
            return camelize(jdbcTemplate.queryForList(sql, args));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Object> queryOne(String sql, Object... args) {
        List<Map<String, Object>> rows = query(sql, args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int count(String sql, Object... args) {
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return value == null ? 0 : value;
        } catch (Exception e) {
            return 0;
        }
    }

    private Long queryLong(String sql, Object... args) {
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
            return value == null ? null : value.longValue();
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> camelize(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                item.put(toCamel(entry.getKey()), entry.getValue());
            }
            result.add(item);
        }
        return result;
    }

    private String toCamel(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char ch : lower.toCharArray()) {
            if (ch == '_') {
                upper = true;
            } else if (upper) {
                out.append(Character.toUpperCase(ch));
                upper = false;
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        if (value == null) return fallback;
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return fallback; }
    }

    private Long asLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return null;
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return null; }
    }

    private String str(Object value, String fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value);
        return s.isBlank() ? fallback : s;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        try { return LocalDateTime.parse(text.replace(" ", "T")); } catch (Exception ignored) {}
        try { return java.time.OffsetDateTime.parse(text).toLocalDateTime(); } catch (Exception ignored) {}
        try { return java.time.Instant.parse(text).atZone(java.time.ZoneId.of("Asia/Shanghai")).toLocalDateTime(); } catch (Exception ignored) {}
        return null;
    }

    private String randomToken(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(TOKEN_CHARS.charAt(random.nextInt(TOKEN_CHARS.length())));
        return sb.toString();
    }
}
