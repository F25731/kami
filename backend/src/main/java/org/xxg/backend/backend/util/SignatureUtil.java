package org.xxg.backend.backend.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 签名工具类
 */
public class SignatureUtil {

    /**
     * SHA256 签名
     * @param data 待签名数据
     * @return 签名结果（十六进制字符串）
     */
    public static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * 生成 API 签名
     * @param apiKey API Key
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param body 请求体
     * @param apiSecret API Secret
     * @return 签名字符串
     */
    public static String generateSignature(String apiKey, String timestamp, String nonce, String body, String apiSecret) {
        String data = apiKey + timestamp + nonce + (body != null ? body : "") + apiSecret;
        return sha256(data);
    }

    /**
     * 验证签名
     */
    public static boolean verifySignature(String signature, String apiKey, String timestamp, String nonce, String body, String apiSecret) {
        String expected = generateSignature(apiKey, timestamp, nonce, body, apiSecret);
        return expected.equals(signature);
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
