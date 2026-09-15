package com.gec.seafood_traceability_system.config;

import com.gec.seafood_traceability_system.interceptors.AdminAuthInterceptor;
import com.gec.seafood_traceability_system.interceptors.LoginInterceptor;
import com.gec.seafood_traceability_system.interceptors.NodeAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Autowired
    private NodeAuthInterceptor nodeAuthInterceptor;

    /**
     * 三个拦截器按注册顺序依次执行，顺序不能调换：
     * <ol>
     *   <li>LoginInterceptor  —— 验签并把 claims 放进 ThreadLocal（后两个依赖它）</li>
     *   <li>AdminAuthInterceptor —— 只拦 /admin/**，要求 role=admin</li>
     *   <li>NodeAuthInterceptor  —— 只拦节点端前缀，要求 role=node</li>
     * </ol>
     * 注意：/region/** 刻意不做角色校验，因为管理端页面（省市区联动）也要用它，
     * 而管理端发请求时带的是 adminToken。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 登录校验：登录接口、消费者溯源接口、管理端登录接口不拦截
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns("/user/login", "/user/logout", "/admin/login", "/trace/**");

        // 2. 管理端权限：必须是管理员 token
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");

        // 3. 节点端权限：必须是企业 token（防止管理员 token 被当作企业编号使用）
        //    /inspection/** 是四个环节共用的检测记录接口，用 stageType 区分批号表，
        //    归属校验在 Controller 内委托给 OwnedBatchRegistry，但鉴权必须先在这里拦住
        registry.addInterceptor(nodeAuthInterceptor)
                .addPathPatterns("/user/**", "/farm/**", "/froz/**", "/whol/**", "/reta/**", "/inspection/**")
                .excludePathPatterns("/user/login", "/user/logout");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")               // 允许所有路径跨域
                .allowedOriginPatterns("*")      // 允许所有来源（配合 allowCredentials 使用）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
