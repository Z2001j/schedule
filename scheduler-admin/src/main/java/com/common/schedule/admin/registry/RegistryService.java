package com.common.schedule.admin.registry;

import com.common.schedule.admin.model.BlockStrategy;
import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteStrategy;
import com.common.schedule.admin.model.ScheduleType;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.schedule.AdminScheduler;
import com.common.schedule.admin.storage.ExecutorDao;
import com.common.schedule.admin.storage.TaskConfigDao;
import com.common.schedule.admin.web.dto.RegisterRequest;
import com.common.schedule.admin.web.dto.TaskMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 执行器注册发现。DB 为任务配置权威来源：注册仅插入新任务，不覆盖已有配置，
 * 避免执行器重启把用户在控制台修改过的周期冲掉。
 */
@Service
public class RegistryService {

    private static final Logger log = LoggerFactory.getLogger(RegistryService.class);

    private final ExecutorDao executorDao;
    private final TaskConfigDao taskConfigDao;
    private final AdminScheduler adminScheduler;

    public RegistryService(ExecutorDao executorDao, TaskConfigDao taskConfigDao, AdminScheduler adminScheduler) {
        this.executorDao = executorDao;
        this.taskConfigDao = taskConfigDao;
        this.adminScheduler = adminScheduler;
    }

    public void register(RegisterRequest request) {
        executorDao.upsertOnline(request.getAppName(), request.getAddress());
        List<TaskMeta> tasks = request.getTasks();
        if (tasks == null) {
            return;
        }
        for (TaskMeta meta : tasks) {
            if (meta.getTaskId() == null || meta.getTaskId().isBlank()) {
                continue;
            }
            if (taskConfigDao.findById(meta.getTaskId()) != null) {
                continue;
            }
            TaskConfig config = fromMeta(request.getAppName(), meta);
            taskConfigDao.insert(config);
            if (config.isEnabled()) {
                adminScheduler.schedule(config);
            }
            log.info("registered new task {} ({})", config.getTaskId(), config.getTaskName());
        }
    }

    public void beat(String appName, String address) {
        executorDao.updateHeartbeat(appName, address);
    }

    public void remove(String appName, String address) {
        executorDao.markOffline(appName, address);
    }

    public List<ExecutorNode> onlineNodes(String appName) {
        return executorDao.findOnlineByApp(appName);
    }

    public List<ExecutorNode> allExecutors() {
        return executorDao.findAll();
    }

    private TaskConfig fromMeta(String appName, TaskMeta meta) {
        TaskConfig c = new TaskConfig();
        c.setTaskId(meta.getTaskId());
        c.setAppName(appName);
        c.setTaskName(meta.getName() == null ? meta.getTaskId() : meta.getName());
        c.setTaskGroup(meta.getGroup() == null ? "default" : meta.getGroup());
        c.setDescription(meta.getDescription());
        c.setScheduleType(meta.getType() == null ? ScheduleType.CRON : meta.getType());
        c.setCronExpression(meta.getCron());
        c.setFixedRate(meta.getFixedRate());
        c.setFixedDelay(meta.getFixedDelay());
        c.setInitialDelay(meta.getInitialDelay() == null ? 0L : meta.getInitialDelay());
        c.setEnabled(meta.isEnabled());
        c.setRouteStrategy(meta.getRoute() == null ? RouteStrategy.FAILOVER : meta.getRoute());
        c.setBlockStrategy(meta.getBlock() == null ? BlockStrategy.SERIAL : meta.getBlock());
        c.setRetryCount(0);
        return c;
    }
}