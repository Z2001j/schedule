package com.common.schedule.executor.execute;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行器本地任务句柄注册表（内存）。
 */
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();

    public void register(JobHandler handler) {
        handlers.put(handler.getTaskId(), handler);
    }

    public JobHandler get(String taskId) {
        return handlers.get(taskId);
    }

    public boolean contains(String taskId) {
        return handlers.containsKey(taskId);
    }

    public Collection<JobHandler> all() {
        return handlers.values();
    }

    public int size() {
        return handlers.size();
    }

    public void startAll() {
        handlers.values().forEach(JobHandler::start);
    }

    public void stopAll() {
        handlers.values().forEach(JobHandler::stop);
    }
}