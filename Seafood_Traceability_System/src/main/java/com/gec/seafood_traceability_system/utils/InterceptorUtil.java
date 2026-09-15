package com.gec.seafood_traceability_system.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gec.seafood_traceability_system.pojo.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 拦截器响应工具
 * <p>
 * 拦截器在进入 Controller 之前就中断了请求，拿不到 GlobalExceptionHandler 的处理，
 * 所以需要自己往 response 里写标准 Result 结构的 JSON。
 */
public final class InterceptorUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InterceptorUtil() {
    }

    /**
     * 写出 {code, message, data} 结构的 JSON，并设置 HTTP 状态码
     */
    public static void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(MAPPER.writeValueAsString(Result.error(code, message)));
    }
}
