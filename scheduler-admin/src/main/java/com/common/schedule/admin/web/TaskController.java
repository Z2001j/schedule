package com.common.schedule.admin.web;

import com.common.schedule.admin.model.ApiResult;
import com.common.schedule.admin.model.TaskLog;
import com.common.schedule.admin.model.TaskView;
import com.common.schedule.admin.service.LogService;
import com.common.schedule.admin.service.TaskService;
import com.common.schedule.admin.web.dto.UpdateRouteRequest;
import com.common.schedule.admin.web.dto.UpdateScheduleRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 控制台任务管理接口。写操作携带 version 实现乐观锁。
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final LogService logService;

    public TaskController(TaskService taskService, LogService logService) {
        this.taskService = taskService;
        this.logService = logService;
    }

    @GetMapping
    public ApiResult<List<TaskView>> list(@RequestParam(required = false) String appName,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String group,
                                          @RequestParam(required = false) Boolean enabled) {
        return ApiResult.ok(taskService.list(appName, keyword, group, enabled));
    }

    @GetMapping("/{taskId}")
    public ApiResult<TaskView> detail(@PathVariable String taskId) {
        return ApiResult.ok(taskService.detail(taskId));
    }

    @PutMapping("/{taskId}/schedule")
    public ApiResult<TaskView> updateSchedule(@PathVariable String taskId,
                                              @RequestBody UpdateScheduleRequest request,
                                              @RequestParam int version) {
        return ApiResult.ok(taskService.updateSchedule(taskId, request, version));
    }

    @PutMapping("/{taskId}/route")
    public ApiResult<TaskView> updateRoute(@PathVariable String taskId,
                                           @RequestBody UpdateRouteRequest request,
                                           @RequestParam int version) {
        return ApiResult.ok(taskService.updateRoute(taskId, request, version));
    }

    @PostMapping("/{taskId}/start")
    public ApiResult<TaskView> start(@PathVariable String taskId, @RequestParam int version) {
        return ApiResult.ok(taskService.start(taskId, version));
    }

    @PostMapping("/{taskId}/stop")
    public ApiResult<TaskView> stop(@PathVariable String taskId, @RequestParam int version) {
        return ApiResult.ok(taskService.stop(taskId, version));
    }

    @PostMapping("/{taskId}/trigger")
    public ApiResult<Void> trigger(@PathVariable String taskId) {
        taskService.trigger(taskId);
        return ApiResult.ok();
    }

    @GetMapping("/{taskId}/logs")
    public ApiResult<List<TaskLog>> logs(@PathVariable String taskId,
                                         @RequestParam(defaultValue = "20") int limit,
                                         @RequestParam(defaultValue = "0") int offset) {
        return ApiResult.ok(logService.query(taskId, limit, offset));
    }
}