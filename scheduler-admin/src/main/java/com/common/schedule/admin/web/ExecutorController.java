package com.common.schedule.admin.web;

import com.common.schedule.admin.model.ApiResult;
import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.registry.RegistryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 控制台执行器监控接口。
 */
@RestController
@RequestMapping("/api/executors")
public class ExecutorController {

    private final RegistryService registryService;

    public ExecutorController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @GetMapping
    public ApiResult<List<ExecutorNode>> list() {
        return ApiResult.ok(registryService.allExecutors());
    }
}