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
 * 校验执行器→调度中心请求的 X-Access-Token。未配置令牌时放行（本地开发）。
 */
@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "X-Access-Token";

    private final AdminProperties properties;
    private final ObjectMapper objectMapper;

    public TokenInterceptor(AdminProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String expected = properties.getAccessToken();
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