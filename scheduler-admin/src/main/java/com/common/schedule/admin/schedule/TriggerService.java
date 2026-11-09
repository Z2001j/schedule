package com.common.schedule.admin.schedule;

import com.common.schedule.admin.alarm.AlarmNotifier;
import com.common.schedule.admin.client.ExecutorClient;
import com.common.schedule.admin.model.ExecutorNode;
import com.common.schedule.admin.model.RouteTarget;
import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.model.TriggerParam;
import com.common.schedule.admin.route.ExecutorRouter;
import com.common.schedule.admin.storage.ExecutorDao;
import com.common.schedule.admin.storage.TaskConfigDao;
import com.common.schedule.admin.storage.TaskLogDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 单次触发编排：读配置 → 取在线节点 → 路由 → 逐目标落日志并下发。
 * 每个路由目标（含分片广播的每一片）独立一条日志，便于各自回调。
 */
@Service
public class TriggerService {

    private static final Logger log = LoggerFactory.getLogger(TriggerService.class);
    public static final String TYPE_SCHEDULE = "SCHEDULE";
    public static final String TYPE_MANUAL = "MANUAL";

    private final TaskConfigDao taskConfigDao;
    private final ExecutorDao executorDao;
    private final ExecutorRouter router;
    private final ExecutorClient client;
    private final TaskLogDao logDao;
    private final AlarmNotifier alarmNotifier;

    public TriggerService(TaskConfigDao taskConfigDao, ExecutorDao executorDao, ExecutorRouter router,
                          ExecutorClient client, TaskLogDao logDao, AlarmNotifier alarmNotifier) {
        this.taskConfigDao = taskConfigDao;
        this.executorDao = executorDao;
        this.router = router;
        this.client = client;
        this.logDao = logDao;
        this.alarmNotifier = alarmNotifier;
    }

    public void trigger(String taskId, String triggerType) {
        TaskConfig config = taskConfigDao.findById(taskId);
        if (config == null) {
            log.warn("trigger skipped, task not found: {}", taskId);
            return;
        }
        if (TYPE_SCHEDULE.equals(triggerType) && !config.isEnabled()) {
            return;
        }
        List<ExecutorNode> nodes = executorDao.findOnlineByApp(config.getAppName());
        List<RouteTarget> targets = router.route(config.getRouteStrategy(), nodes, taskId);
        if (targets.isEmpty()) {
            if (TYPE_MANUAL.equals(triggerType)) {
                long logId = logDao.insertTrigger(config, triggerType);
                logDao.markTriggerFail(logId, "无在线执行器实例");
            } else {
                log.warn("trigger skipped, no online executor for app={} task={}", config.getAppName(), taskId);
            }
            alarmNotifier.onTriggerFailure(config, triggerType, "无在线执行器实例");
            return;
        }
        for (RouteTarget target : targets) {
            dispatch(config, target, triggerType);
        }
    }

    private void dispatch(TaskConfig config, RouteTarget target, String triggerType) {
        long logId = logDao.insertTrigger(config, triggerType);
        TriggerParam param = new TriggerParam();
        param.setTaskId(config.getTaskId());
        param.setLogId(logId);
        param.setShardIndex(target.getShardIndex());
        param.setShardTotal(target.getShardTotal());
        param.setBlockStrategy(config.getBlockStrategy());
        param.setTriggerType(triggerType);

        boolean accepted = false;
        int attempts = Math.max(0, config.getRetryCount()) + 1;
        for (int i = 0; i < attempts && !accepted; i++) {
            accepted = client.run(target.getAddress(), param);
        }
        if (accepted) {
            logDao.markTriggered(logId, target.getAddress(), target.getShardIndex(), target.getShardTotal());
        } else {
            logDao.markTriggerFail(logId, "触发失败：执行器不可达或未受理");
            alarmNotifier.onTriggerFailure(config, triggerType, "触发失败：执行器不可达或未受理");
        }
    }
}