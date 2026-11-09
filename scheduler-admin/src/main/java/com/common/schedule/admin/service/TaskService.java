package com.common.schedule.admin.service;

import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.model.TaskLog;
import com.common.schedule.admin.model.TaskView;
import com.common.schedule.admin.schedule.AdminScheduler;
import com.common.schedule.admin.schedule.TriggerService;
import com.common.schedule.admin.storage.ExecutorDao;
import com.common.schedule.admin.storage.TaskConfigDao;
import com.common.schedule.admin.storage.TaskLogDao;
import com.common.schedule.admin.web.dto.UpdateRouteRequest;
import com.common.schedule.admin.web.dto.UpdateScheduleRequest;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 控制台任务管理。写操作走乐观锁（version），冲突抛 {@link ScheduleException}。
 * 周期/启停变更后同步重建调度器触发器；路由变更无需重建（触发时即时读取）。
 */
@Service
public class TaskService {

    private final TaskConfigDao taskConfigDao;
    private final TaskLogDao taskLogDao;
    private final ExecutorDao executorDao;
    private final AdminScheduler adminScheduler;
    private final TriggerService triggerService;
    private final ScheduleValidator validator;

    public TaskService(TaskConfigDao taskConfigDao, TaskLogDao taskLogDao, ExecutorDao executorDao,
                       AdminScheduler adminScheduler, TriggerService triggerService, ScheduleValidator validator) {
        this.taskConfigDao = taskConfigDao;
        this.taskLogDao = taskLogDao;
        this.executorDao = executorDao;
        this.adminScheduler = adminScheduler;
        this.triggerService = triggerService;
        this.validator = validator;
    }

    public List<TaskView> list(String appName, String keyword, String group, Boolean enabled) {
        List<TaskView> views = new ArrayList<>();
        for (TaskConfig config : taskConfigDao.findAll(appName, keyword, group, enabled)) {
            views.add(toView(config));
        }
        return views;
    }

    public TaskView detail(String taskId) {
        return toView(require(taskId));
    }

    public TaskView updateSchedule(String taskId, UpdateScheduleRequest req, int version) {
        validator.validate(req.getType(), req.getCron(), req.getFixedRate(), req.getFixedDelay());
        TaskConfig config = require(taskId);
        config.setScheduleType(req.getType());
        config.setCronExpression(req.getCron());
        config.setFixedRate(req.getFixedRate());
        config.setFixedDelay(req.getFixedDelay());
        config.setInitialDelay(req.getInitialDelay() == null ? 0L : req.getInitialDelay());
        TaskConfig updated = persist(config, version);
        adminScheduler.reschedule(updated);
        return toView(updated);
    }

    public TaskView updateRoute(String taskId, UpdateRouteRequest req, int version) {
        validator.validateRetry(req.getRetryCount());
        TaskConfig config = require(taskId);
        if (req.getRouteStrategy() != null) {
            config.setRouteStrategy(req.getRouteStrategy());
        }
        if (req.getBlockStrategy() != null) {
            config.setBlockStrategy(req.getBlockStrategy());
        }
        config.setRetryCount(req.getRetryCount());
        return toView(persist(config, version));
    }

    public TaskView start(String taskId, int version) {
        TaskConfig config = require(taskId);
        config.setEnabled(true);
        TaskConfig updated = persist(config, version);
        adminScheduler.schedule(updated);
        return toView(updated);
    }

    public TaskView stop(String taskId, int version) {
        TaskConfig config = require(taskId);
        config.setEnabled(false);
        TaskConfig updated = persist(config, version);
        adminScheduler.stop(taskId);
        return toView(updated);
    }

    public void trigger(String taskId) {
        require(taskId);
        triggerService.trigger(taskId, TriggerService.TYPE_MANUAL);
    }

    private TaskConfig require(String taskId) {
        TaskConfig config = taskConfigDao.findById(taskId);
        if (config == null) {
            throw new ScheduleException("任务不存在：" + taskId);
        }
        return config;
    }

    private TaskConfig persist(TaskConfig config, int expectedVersion) {
        config.setVersion(expectedVersion);
        int rows = taskConfigDao.updateWithVersion(config);
        if (rows == 0) {
            throw new ScheduleException("配置已被他人修改，请刷新后重试");
        }
        return taskConfigDao.findById(config.getTaskId());
    }

    private TaskView toView(TaskConfig config) {
        TaskView view = TaskView.from(config);
        TaskLog latest = taskLogDao.findLatestByTask(config.getTaskId());
        if (latest != null) {
            view.setLastExecTime(latest.getTriggerTime());
            view.setLastExecSuccess(latest.getSuccess());
            view.setLastError(latest.getErrorMsg());
        }
        view.setOnlineInstances(executorDao.findOnlineByApp(config.getAppName()).size());
        view.setNextExecTime(nextExecTime(config));
        return view;
    }

    private LocalDateTime nextExecTime(TaskConfig config) {
        if (!config.isEnabled()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            if (config.getScheduleType() == ScheduleType.CRON) {
                if (config.getCronExpression() == null || !CronExpression.isValidExpression(config.getCronExpression())) {
                    return null;
                }
                return CronExpression.parse(config.getCronExpression()).next(now);
            }
            Long period = config.getScheduleType() == ScheduleType.FIXED_RATE ? config.getFixedRate() : config.getFixedDelay();
            return period == null ? null : now.plusNanos(period * 1_000_000L);
        } catch (Exception e) {
            return null;
        }
    }
}