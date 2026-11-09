package com.common.schedule.executor.model;

import java.util.List;

/**
 * 执行器注册请求：上报 appName、地址与任务清单。
 */
public class RegisterRequest {

    private String appName;
    private String address;
    private List<TaskMeta> tasks;

    public RegisterRequest() {
    }

    public RegisterRequest(String appName, String address, List<TaskMeta> tasks) {
        this.appName = appName;
        this.address = address;
        this.tasks = tasks;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<TaskMeta> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskMeta> tasks) {
        this.tasks = tasks;
    }
}