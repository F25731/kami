package org.xxg.backend.backend.util;

import java.security.SecureRandom;

/**
 * 令牌生成工具类
 */
public class TokenGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成7位项目随机码
     * 字符范围：大小写字母 + 数字（排除易混淆字符 0OIl1）
     * 示例：a8K39xQ, Wm72Pq9
     */
    public static String generate7DigitProjectToken() {
        StringBuilder token = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            token.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return token.toString();
    }

    /**
     * 生成32位API Key
     */
    public static String generateApiKey() {
        StringBuilder key = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            key.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return key.toString();
    }

    /**
     * 生成64位API Secret
     */
    public static String generateApiSecret() {
        StringBuilder secret = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            secret.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return secret.toString();
    }

    /**
     * 生成指定长度的随机字符串
     */
    public static String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return result.toString();
    }
}
