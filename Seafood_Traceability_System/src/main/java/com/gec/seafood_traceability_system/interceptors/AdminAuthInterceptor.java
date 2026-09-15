package com.gec.seafood_traceability_system.interceptors;

import com.gec.seafood_traceability_system.pojo.Result;
import com.gec.seafood_traceability_system.utils.InterceptorUtil;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 管理端权限拦截器（第 2 道）
 * <p>
 * 只挂载在 /admin/** 上，要求 token 里的 role 必须是 admin。
 * 这样即使有人拿养殖企业的 token 去调管理端接口，也会被挡下来。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || !"admin".equals(claims.get("role"))) {
            InterceptorUtil.writeJson(response, Result.CODE_FORBIDDEN, "无管理员权限，请使用管理端账号登录");
            return false;
        }
        return true;
    }
}
