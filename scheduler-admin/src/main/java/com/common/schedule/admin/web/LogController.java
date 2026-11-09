package com.common.schedule.admin.web;

import com.common.schedule.admin.model.ApiResult;
import com.common.schedule.admin.service.LogService;
import com.common.schedule.admin.web.dto.CallbackRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 执行器结果回调接口（X-Access-Token 保护）。
 */
@RestController
@RequestMapping("/api/log")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping("/callback")
    public ApiResult<Void> callback(@RequestBody CallbackRequest request) {
        logService.callback(request);
        return ApiResult.ok();
    }
}