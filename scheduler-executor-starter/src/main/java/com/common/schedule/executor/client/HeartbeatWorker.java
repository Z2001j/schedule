package com.common.schedule.executor.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 周期心跳续约；优雅停机时主动注销。
 */
public class HeartbeatWorker {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatWorker.class);

    private final AdminClient adminClient;
    private final String appName;
    private final int intervalSeconds;

    private volatile String address;
    private ScheduledExecutorService scheduler;

    public HeartbeatWorker(AdminClient adminClient, String appName, int intervalSeconds) {
        this.adminClient = adminClient;
        this.appName = appName;
        this.intervalSeconds = intervalSeconds <= 0 ? 30 : intervalSeconds;
    }

    public synchronized void start(String address) {
        this.address = address;
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sched-heartbeat");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::beat, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("[scheduler-executor] 心跳已启动 app={} address={} interval={}s", appName, address, intervalSeconds);
    }

    private void beat() {
        try {
            adminClient.beat(appName, address);
        } catch (Throwable t) {
            log.warn("[scheduler-executor] 心跳异常 app={} : {}", appName, t.getMessage());
        }
    }

    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (address != null) {
            try {
                adminClient.remove(appName, address);
            } catch (Throwable t) {
                log.warn("[scheduler-executor] 注销异常 app={} : {}", appName, t.getMessage());
            }
        }
    }
}