package com.common.schedule.admin.schedule;

import com.common.schedule.admin.model.TaskConfig;
import com.common.schedule.admin.storage.TaskConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动恢复：调度中心重启后从 DB 重新加载所有启用任务的触发器。
 */
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final TaskConfigDao taskConfigDao;
    private final AdminScheduler adminScheduler;

    public AdminBootstrap(TaskConfigDao taskConfigDao, AdminScheduler adminScheduler) {
        this.taskConfigDao = taskConfigDao;
        this.adminScheduler = adminScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        List<TaskConfig> configs = taskConfigDao.findAll();
        int loaded = 0;
        for (TaskConfig config : configs) {
            if (config.isEnabled()) {
                adminScheduler.schedule(config);
                loaded++;
            }
        }
        log.info("scheduler-admin ready, loaded {} enabled task(s) from {} total", loaded, configs.size());
    }
}