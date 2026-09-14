package com.gec.seafood_traceability_system.interceptors;


import com.gec.seafood_traceability_system.utils.JwtUtil;
import com.gec.seafood_traceability_system.utils.ThreadLocalUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //令牌验证（修正拼写为标准的 Authorization，与前端 axios 注入的请求头保持一致）
        String token = request.getHeader("Authorization");
        //验证登录
        try {
            Map<String,Object> claims = JwtUtil.parseToken(token);

            //把业务类型数据放到ThreadLocal中
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception e) {
            //http响应状态码是401
            response.setStatus(401);
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
