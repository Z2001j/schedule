package com.common.schedule.admin.service;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.model.ScheduleType;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/**
 * 周期合法性校验：Cron 表达式有效性、固定频率/延迟不低于配置下限。
 */
@Component
public class ScheduleValidator {

    private final AdminProperties properties;

    public ScheduleValidator(AdminProperties properties) {
        this.properties = properties;
    }

    public void validate(ScheduleType type, String cron, Long fixedRate, Long fixedDelay) {
        if (type == null) {
            throw new ScheduleException("调度类型不能为空");
        }
        switch (type) {
            case CRON:
                if (cron == null || cron.isBlank() || !CronExpression.isValidExpression(cron)) {
                    throw new ScheduleException("Cron 表达式非法：" + cron);
                }
                break;
            case FIXED_RATE:
                requirePeriod(fixedRate, "固定频率");
                break;
            case FIXED_DELAY:
                requirePeriod(fixedDelay, "固定延迟");
                break;
            default:
                throw new ScheduleException("不支持的调度类型：" + type);
        }
    }

    private void requirePeriod(Long period, String label) {
        long min = properties.getMinPeriodMs();
        if (period == null || period < min) {
            throw new ScheduleException(label + "不能小于 " + min + " 毫秒");
        }
    }

    public void validateRetry(int retryCount) {
        if (retryCount < 0 || retryCount > 10) {
            throw new ScheduleException("重试次数须在 0~10 之间");
        }
    }
}