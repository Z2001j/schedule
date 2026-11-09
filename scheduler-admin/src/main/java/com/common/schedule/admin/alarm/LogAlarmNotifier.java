package com.common.schedule.admin.alarm;

import com.common.schedule.admin.model.TaskConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认告警实现：仅记录错误日志。生产可替换为钉钉/邮件等自定义 {@link AlarmNotifier}。
 */
public class LogAlarmNotifier implements AlarmNotifier {

    private static final Logger log = LoggerFactory.getLogger(LogAlarmNotifier.class);

    @Override
    public void onTriggerFailure(TaskConfig config, String triggerType, String reason) {
        log.error("[scheduler-alarm] task={} app={} triggerType={} reason={}",
                config.getTaskId(), config.getAppName(), triggerType, reason);
    }
}