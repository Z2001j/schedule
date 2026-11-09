package com.common.schedule.admin.web.dto;

/**
 * 执行器注册/心跳/注销的地址标识。
 */
public class NodeAddressRequest {

    private String appName;
    private String address;

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
}