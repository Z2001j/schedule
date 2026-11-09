package com.common.schedule.admin.web;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.model.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 控制台管理 API（/api/tasks、/api/executors）令牌鉴权，默认开启，杜绝匿名改周期/启停/触发。
 * 期望令牌优先取 managementToken（与执行器 accessToken 分离），为空则回退 accessToken；
 * 两者均未配置时放行（本地开发）。如需角色级鉴权，可关闭本拦截器并接入 Spring Security/网关。
 */
@Component
public class ManagementAuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "X-Access-Token";

    private final AdminProperties properties;
    private final ObjectMapper objectMapper;

    public ManagementAuthInterceptor(AdminProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isManagementAuthEnabled()) {
            return true;
        }
        String expected = properties.getManagementToken();
        if (expected == null || expected.isEmpty()) {
            expected = properties.getAccessToken();
        }
        if (expected == null || expected.isEmpty()) {
            return true;
        }
        if (expected.equals(request.getHeader(TOKEN_HEADER))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResult.fail(40100, "非法访问令牌")));
        return false;
    }
}