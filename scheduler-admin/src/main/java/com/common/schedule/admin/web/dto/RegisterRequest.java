package com.common.schedule.admin.web.dto;

import java.util.List;

/**
 * 执行器注册请求。
 */
public class RegisterRequest {

    private String appName;
    private String address;
    private List<TaskMeta> tasks;

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