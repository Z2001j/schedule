package com.common.schedule.admin.alarm;

import com.common.schedule.admin.model.TaskConfig;

/**
 * 触发告警扩展点。无在线实例、执行器不可达等触发失败时回调。
 * 默认 {@link LogAlarmNotifier} 仅记录日志；业务方可注入自定义 Bean（钉钉/邮件/短信）覆盖。
 */
public interface AlarmNotifier {

    void onTriggerFailure(TaskConfig config, String triggerType, String reason);
}