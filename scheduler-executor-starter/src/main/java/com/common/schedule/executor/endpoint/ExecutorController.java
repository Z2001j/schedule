package com.common.schedule.executor.endpoint;

import com.common.schedule.executor.config.ExecutorProperties;
import com.common.schedule.executor.execute.JobHandler;
import com.common.schedule.executor.execute.JobHandlerRegistry;
import com.common.schedule.executor.model.ApiResult;
import com.common.schedule.executor.model.TriggerParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 执行器内部 HTTP 端点：接收调度中心触发指令与空闲探测。建议仅内网可达 + accessToken 校验。
 */
@RestController
@RequestMapping("/scheduler-executor")
public class ExecutorController {

    private static final String TOKEN_HEADER = "X-Access-Token";

    private final JobHandlerRegistry registry;
    private final ExecutorProperties properties;

    public ExecutorController(JobHandlerRegistry registry, ExecutorProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @PostMapping("/run")
    public ApiResult<String> run(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                 @RequestBody TriggerParam param) {
        checkToken(token);
        JobHandler handler = registry.get(param.getTaskId());
        if (handler == null) {
            return ApiResult.fail("任务未注册: " + param.getTaskId());
        }
        handler.invoke(param);
        return ApiResult.ok("accepted");
    }

    @PostMapping("/beat")
    public ApiResult<Boolean> beat(@RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        checkToken(token);
        boolean idle = registry.all().stream().allMatch(h -> h.getJobThread().isIdle());
        return ApiResult.ok(idle);
    }

    private void checkToken(String token) {
        String expected = properties.getAccessToken();
        if (expected != null && !expected.isEmpty() && !expected.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "accessToken 校验失败");
        }
    }
}