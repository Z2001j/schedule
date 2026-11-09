package com.common.schedule.admin.ha;

import com.common.schedule.admin.config.AdminProperties;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 调度中心选主：基于 DB 单行锁 + 心跳。仅 leader 实例运行到点触发，follower  standby，
 * leader 心跳超时后由 follower 原子接管。HA 关闭（单实例）时恒为 leader。
 *
 * 接管原子性靠条件 UPDATE：仅当锁属于自己或已过期才续约/抢占；并发 INSERT 由主键去重。
 */
@Component
@DependsOn("schemaInitializer")
public class AdminLeaderElection implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AdminLeaderElection.class);
    private static final String LOCK_NAME = "ADMIN";

    private final AdminProperties properties;
    private final JdbcTemplate jdbc;
    private final String instanceId = UUID.randomUUID().toString();

    private volatile boolean leader;
    private ScheduledExecutorService scheduler;

    public AdminLeaderElection(AdminProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
    }

    public boolean isLeader() {
        return !properties.isHaEnabled() || leader;
    }

    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.isHaEnabled()) {
            leader = true;
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "admin-leader-election");
            t.setDaemon(true);
            return t;
        });
        tryAcquireSafe();
        int interval = Math.max(1, properties.getElectionInterval());
        scheduler.scheduleWithFixedDelay(this::tryAcquireSafe, interval, interval, TimeUnit.SECONDS);
    }

    private void tryAcquireSafe() {
        try {
            tryAcquire();
        } catch (Exception e) {
            log.warn("[scheduler-admin] 选主异常，暂保持当前状态 leader={}", leader, e);
        }
    }

    void tryAcquire() {
        if (!properties.isHaEnabled()) {
            leader = true;
            return;
        }
        String table = properties.getLeaderTableName();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp threshold = Timestamp.valueOf(LocalDateTime.now().minusSeconds(properties.getLeaderTimeout()));
        int rows = jdbc.update("UPDATE " + table + " SET instance_id=?,heartbeat=? "
                        + "WHERE lock_name=? AND (instance_id=? OR heartbeat<?)",
                instanceId, now, LOCK_NAME, instanceId, threshold);
        if (rows == 0) {
            try {
                rows = jdbc.update("INSERT INTO " + table + " (lock_name,instance_id,heartbeat) VALUES (?,?,?)",
                        LOCK_NAME, instanceId, now);
            } catch (DuplicateKeyException e) {
                rows = 0;
            }
        }
        boolean was = leader;
        leader = rows > 0;
        if (was != leader) {
            log.info("[scheduler-admin] 选主状态变更 instance={} leader={}", instanceId, leader);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (properties.isHaEnabled() && leader) {
            try {
                jdbc.update("DELETE FROM " + properties.getLeaderTableName() + " WHERE lock_name=? AND instance_id=?",
                        LOCK_NAME, instanceId);
            } catch (Exception e) {
                log.warn("[scheduler-admin] 释放 leader 锁失败", e);
            }
        }
    }
}