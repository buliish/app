package com.gec.seafood_traceability_system.interceptors;


import com.gec.seafood_traceability_system.utils.InterceptorUtil;
import com.gec.seafood_traceability_system.utils.JwtUtil;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 登录拦截器（第 1 道）
 * <p>
 * 只负责两件事：验证 token 是否合法、把解析出的 claims 放进 ThreadLocal。
 * 具体是"管理员"还是"节点企业"由后面两个角色拦截器判断。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //令牌验证（标准的 Authorization，与前端 axios 注入的请求头保持一致）
        String token = request.getHeader("Authorization");
        //验证登录
        try {
            Map<String,Object> claims = JwtUtil.parseToken(token);

            //把业务类型数据放到ThreadLocal中
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception e) {
            // http 响应状态码 401，同时返回 JSON 体，避免前端拿到空响应体
            InterceptorUtil.writeJson(response, 401, "未登录或登录已过期，请重新登录");
            //不放行
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        //清除ThreadLocal中的数据，防止内存泄漏
        ThreadLocalUtil.remove();
    }
}
