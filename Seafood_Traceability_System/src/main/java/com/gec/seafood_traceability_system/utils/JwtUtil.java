package com.gec.seafood_traceability_system.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.gec.seafood_traceability_system.pojo.BizException;
import com.gec.seafood_traceability_system.pojo.Result;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String KEY = "seafood";

    //接收业务数据生成token并返回
    public static String genToken(Map<String, Object> claims){
        return JWT.create()
                .withClaim("claims",claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))
                .sign(Algorithm.HMAC256(KEY));

    }

    //接收token, 验证token, 并返回业务数据
    public static  Map<String,Object> parseToken(String token){
        // 显式判空：否则 JWT.verify(null) 抛出的 IllegalArgumentException 在日志里
        // 无法区分"没带 token"和"签名错误"，排查困难
        if (token == null || token.isBlank()) {
            throw new BizException(Result.CODE_UNAUTHORIZED, "未携带登录凭证");
        }
        // 兼容将来可能加上的 "Bearer " 前缀
        String raw = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(raw)
                .getClaim("claims")
                .asMap();
    }
}
