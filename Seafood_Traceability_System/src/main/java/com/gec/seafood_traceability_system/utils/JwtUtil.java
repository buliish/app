package com.gec.seafood_traceability_system.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Result;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT 工具
 * <p>
 * 密钥与有效期来自 application.yaml 的 jwt.secret / jwt.ttl-hours，
 * 支持通过环境变量 JWT_SECRET 覆盖，避免密钥硬编码在源码里。
 * <p>
 * 注意：改密钥后此前签发的 token 会全部失效，需要重新登录。
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.ttl-hours:12}")
    private long ttlHours;

    private static String key;
    private static long ttlMillis;

    @PostConstruct
    public void init() {
        key = this.secret;
        ttlMillis = this.ttlHours * 60 * 60 * 1000;
    }

    /** 接收业务数据生成token并返回 */
    public static String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + ttlMillis))
                .sign(Algorithm.HMAC256(key));
    }

    /** 接收token, 验证token, 并返回业务数据 */
    public static Map<String, Object> parseToken(String token) {
        // 显式判空：否则 JWT.verify(null) 抛出的 IllegalArgumentException 在日志里
        // 无法区分"没带 token"和"签名错误"，排查困难
        if (token == null || token.isBlank()) {
            throw new BizException(Result.CODE_UNAUTHORIZED, "未携带登录凭证");
        }
        // 兼容将来可能加上的 "Bearer " 前缀
        String raw = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        return JWT.require(Algorithm.HMAC256(key))
                .build()
                .verify(raw)
                .getClaim("claims")
                .asMap();
    }
}
