package com.common.schedule.admin.web;

import com.common.schedule.admin.model.ApiResult;
import com.common.schedule.admin.registry.RegistryService;
import com.common.schedule.admin.web.dto.NodeAddressRequest;
import com.common.schedule.admin.web.dto.RegisterRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 执行器注册发现接口（X-Access-Token 保护）。路径与执行器 SDK AdminClient 对齐。
 */
@RestController
@RequestMapping("/api/registry")
public class RegistryController {

    private final RegistryService registryService;

    public RegistryController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@RequestBody RegisterRequest request) {
        registryService.register(request);
        return ApiResult.ok();
    }

    @PostMapping("/beat")
    public ApiResult<Void> beat(@RequestBody NodeAddressRequest request) {
        registryService.beat(request.getAppName(), request.getAddress());
        return ApiResult.ok();
    }

    @PostMapping("/remove")
    public ApiResult<Void> remove(@RequestBody NodeAddressRequest request) {
        registryService.remove(request.getAppName(), request.getAddress());
        return ApiResult.ok();
    }
}