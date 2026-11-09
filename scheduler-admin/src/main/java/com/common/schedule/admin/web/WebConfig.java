package com.common.schedule.admin.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 令牌拦截器绑定：执行器通信接口（注册/心跳/注销/回调）用 accessToken；
 * 控制台管理接口（/api/tasks、/api/executors）用管理令牌鉴权（默认开启）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;
    private final ManagementAuthInterceptor managementAuthInterceptor;

    public WebConfig(TokenInterceptor tokenInterceptor, ManagementAuthInterceptor managementAuthInterceptor) {
        this.tokenInterceptor = tokenInterceptor;
        this.managementAuthInterceptor = managementAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor).addPathPatterns("/api/registry/**", "/api/log/**");
        registry.addInterceptor(managementAuthInterceptor).addPathPatterns("/api/tasks/**", "/api/executors/**");
    }
}