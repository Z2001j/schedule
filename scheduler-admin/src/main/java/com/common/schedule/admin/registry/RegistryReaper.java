package com.common.schedule.admin.registry;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.storage.ExecutorDao;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 心跳超时剔除：周期性把超过 heartbeatTimeout 未续约的在线节点置为离线。
 */
@Component
public class RegistryReaper implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RegistryReaper.class);

    private final ExecutorDao executorDao;
    private final AdminProperties properties;
    private ScheduledExecutorService scheduler;

    public RegistryReaper(ExecutorDao executorDao, AdminProperties properties) {
        this.executorDao = executorDao;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "registry-reaper");
            t.setDaemon(true);
            return t;
        });
        int interval = Math.max(1, properties.getReaperInterval());
        scheduler.scheduleWithFixedDelay(this::reap, interval, interval, TimeUnit.SECONDS);
    }

    void reap() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(properties.getHeartbeatTimeout());
            int count = executorDao.markOfflineBefore(threshold);
            if (count > 0) {
                log.info("reaped {} offline executor(s)", count);
            }
        } catch (Exception e) {
            log.warn("registry reap failed", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}