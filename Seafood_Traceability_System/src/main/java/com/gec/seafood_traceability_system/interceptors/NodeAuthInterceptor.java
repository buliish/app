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
 * 节点端权限拦截器（第 2 道）
 * <p>
 * 挂载在 /user/**、/farm/**、/froz/**、/whol/**、/reta/** 上。
 * 这些接口内部用 token 里的 id 当作"当前企业编号"（currentNodeId()），
 * 如果放进来的却是管理员的 token，adminId 会被误当成企业编号，产生脏数据，
 * 所以这里必须挡掉。
 * <p>
 * 兼容说明：历史签发的 token 里没有 role 字段，此时放行，避免老 token 全部失效。
 */
@Component
public class NodeAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            InterceptorUtil.writeJson(response, Result.CODE_UNAUTHORIZED, "未登录或登录已过期，请重新登录");
            return false;
        }
        Object role = claims.get("role");
        // role 为 null 视为历史节点端 token，继续放行
        if (role != null && !"node".equals(role)) {
            InterceptorUtil.writeJson(response, Result.CODE_FORBIDDEN, "请使用企业账号登录后再操作");
            return false;
        }
        return true;
    }
}
