package com.example.kami.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 多项目卡密授权中心 - Java SDK
 * Version: 1.0.0
 *
 * <p>使用示例:
 * <pre>
 * KamiClient client = new KamiClient(
 *     "https://api.example.com",
 *     "abc123X",
 *     "your_api_key",
 *     "your_api_secret"
 * );
 *
 * // 验证卡密
 * Map<String, Object> result = client.verifyCard("ABCD-EFGH-IJKL");
 *
 * // 消费卡密
 * result = client.consumeCard("ABCD-EFGH-IJKL", "order_001", 1, null);
 * </pre>
 */
public class KamiClient {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final String projectToken;
    private final String apiKey;
    private final String apiSecret;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KamiClient(String baseUrl, String projectToken, String apiKey, String apiSecret) {
        this(baseUrl, projectToken, apiKey, apiSecret, 30);
    }

    public KamiClient(String baseUrl, String projectToken, String apiKey, String apiSecret, int timeoutSeconds) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.projectToken = projectToken;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();
    }

    // ========== 内部工具方法 ==========

    private String generateSignature(String apiKey, String timestamp, String nonce, String body, String apiSecret) {
        String data = apiKey + timestamp + nonce + body + apiSecret;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String generateNonce(int length) {
        StringBuilder nonce = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            nonce.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return nonce.toString();
    }

    private Map<String, Object> request(String endpoint, Map<String, Object> payload) {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = generateNonce(16);
        String body = "";

        try {
            if (payload != null) {
                body = objectMapper.writeValueAsString(payload);
            }

            String signature = generateSignature(apiKey, String.valueOf(timestamp), nonce, body, apiSecret);
            String url = baseUrl + "/api/p/" + projectToken + endpoint;

            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-API-KEY", apiKey)
                .addHeader("X-TIMESTAMP", String.valueOf(timestamp))
                .addHeader("X-NONCE", nonce)
                .addHeader("X-SIGN", signature)
                .post(RequestBody.create(body, JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "{}";
                return objectMapper.readValue(responseBody, Map.class);
            }
        } catch (IOException e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("code", "REQUEST_ERROR");
            errorResult.put("message", e.getMessage());
            return errorResult;
        }
    }

    // ========== 卡密相关接口 ==========

    /**
     * 生成卡密
     */
    public Map<String, Object> generateCards(int quantity, String cardType, int countValue,
                                              int durationDays, boolean isPermanent,
                                              String orderNo, Long packageId, String remark) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("quantity", quantity);
        payload.put("card_type", cardType);
        payload.put("count_value", countValue);
        payload.put("duration_days", durationDays);
        payload.put("is_permanent", isPermanent);
        if (orderNo != null) payload.put("order_no", orderNo);
        if (packageId != null) payload.put("package_id", packageId);
        if (remark != null) payload.put("remark", remark);

        return request("/cards/generate", payload);
    }

    /**
     * 验证卡密
     */
    public Map<String, Object> verifyCard(String cardKey) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_key", cardKey);
        return request("/cards/verify", payload);
    }

    /**
     * 查询卡密状态
     */
    public Map<String, Object> getCardStatus(String cardKey) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_key", cardKey);
        return request("/cards/status", payload);
    }

    /**
     * 消费卡密
     */
    public Map<String, Object> consumeCard(String cardKey, String bizId, int amount, String deviceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_key", cardKey);
        payload.put("biz_id", bizId);
        payload.put("amount", amount);
        if (deviceId != null) payload.put("device_id", deviceId);

        return request("/cards/consume", payload);
    }

    /**
     * 兑换卡密到用户权益
     */
    public Map<String, Object> redeemCard(String cardKey, String userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_key", cardKey);
        payload.put("user_id", userId);
        return request("/cards/redeem", payload);
    }

    /**
     * 解绑设备
     */
    public Map<String, Object> unbindDevice(String cardKey, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_key", cardKey);
        if (reason != null) payload.put("reason", reason);
        return request("/cards/unbind-device", payload);
    }

    /**
     * 退还消费
     */
    public Map<String, Object> refundConsume(String cardKey, String bizId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("card_key", cardKey);
        payload.put("biz_id", bizId);
        return request("/cards/refund-consume", payload);
    }

    // ========== 用户权益相关接口 ==========

    /**
     * 查询用户权益状态
     */
    public Map<String, Object> getEntitlementStatus(String userId, String entitlementType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("entitlement_type", entitlementType);
        return request("/entitlements/status", payload);
    }

    /**
     * 消费用户权益
     */
    public Map<String, Object> consumeEntitlement(String userId, String entitlementType,
                                                   String bizId, int amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("entitlement_type", entitlementType);
        payload.put("biz_id", bizId);
        payload.put("amount", amount);
        return request("/entitlements/consume", payload);
    }

    /**
     * 退还权益消费
     */
    public Map<String, Object> refundEntitlement(String userId, String entitlementType,
                                                  String bizId, int amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("entitlement_type", entitlementType);
        payload.put("biz_id", bizId);
        payload.put("amount", amount);
        return request("/entitlements/refund-consume", payload);
    }

    // ========== 使用示例 ==========

    public static void main(String[] args) {
        KamiClient client = new KamiClient(
            "https://api.example.com",
            "abc123X",
            "your_api_key_here",
            "your_api_secret_here"
        );

        // 示例1：生成卡密
        System.out.println("=== 生成卡密 ===");
        Map<String, Object> result = client.generateCards(
            5, "count", 100, 0, false,
            "ORDER20260621001", null, "测试批次"
        );
        System.out.println(result);

        // 示例2：验证卡密
        if (Boolean.TRUE.equals(result.get("success"))) {
            System.out.println("\n=== 验证卡密 ===");
            result = client.verifyCard("ABCD-EFGH-IJKL");
            System.out.println(result);

            // 示例3：消费卡密
            System.out.println("\n=== 消费卡密 ===");
            result = client.consumeCard("ABCD-EFGH-IJKL", "user123_001", 1, null);
            System.out.println(result);
        }
    }
}
