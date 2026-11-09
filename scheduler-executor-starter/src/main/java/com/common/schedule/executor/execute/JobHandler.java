package com.common.schedule.executor.execute;

import com.common.schedule.executor.annotation.ManagedScheduled;
import com.common.schedule.executor.client.AdminClient;
import com.common.schedule.executor.model.TriggerParam;

import java.lang.reflect.Method;

/**
 * 一个受管任务的句柄：持有目标 bean/method、注解元数据与专属执行线程。
 */
public class JobHandler {

    private final String taskId;
    private final Object bean;
    private final Method method;
    private final ManagedScheduled meta;
    private final JobThread jobThread;

    public JobHandler(String taskId, Object bean, Method method, ManagedScheduled meta, AdminClient adminClient) {
        this.taskId = taskId;
        this.bean = bean;
        this.method = method;
        this.meta = meta;
        this.jobThread = new JobThread(taskId, bean, method, adminClient);
    }

    public void start() {
        jobThread.start();
    }

    public void invoke(TriggerParam param) {
        jobThread.push(param);
    }

    public void stop() {
        jobThread.shutdown();
    }

    public String getTaskId() {
        return taskId;
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

    public ManagedScheduled getMeta() {
        return meta;
    }

    public JobThread getJobThread() {
        return jobThread;
    }
}