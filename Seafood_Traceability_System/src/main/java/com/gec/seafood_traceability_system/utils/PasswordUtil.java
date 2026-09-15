package com.gec.seafood_traceability_system.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具（BCrypt）
 * <p>
 * 采取"平滑升级"策略：库里已有的是历史明文密码，仍然允许登录；
 * 一旦登录成功，调用方会把明文改写为 BCrypt 哈希，之后就走哈希比对了。
 * 这样不需要一次性迁移全部账号数据。
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    /**
     * 校验密码。stored 是 BCrypt 哈希则用哈希比对，否则按历史明文直接比较。
     */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (isHashed(stored)) {
            return ENCODER.matches(raw, stored);
        }
        return stored.equals(raw);
    }

    /** 加密（新建账号时使用） */
    public static String encode(String raw) {
        return ENCODER.encode(raw);
    }

    /** 判断库里存的是否已经是 BCrypt 哈希 */
    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith("$2");
    }
}
