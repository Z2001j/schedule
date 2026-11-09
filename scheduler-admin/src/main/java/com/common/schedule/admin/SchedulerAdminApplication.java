package com.common.schedule.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度中心启动类。独立部署，业务服务通过执行器 SDK 接入。
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class SchedulerAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerAdminApplication.class, args);
    }
}