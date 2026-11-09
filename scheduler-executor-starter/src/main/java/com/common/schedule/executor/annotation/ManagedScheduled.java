package com.common.schedule.executor.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Spring Bean 的 public 无参方法上，启动时由执行器扫描并注册到调度中心。
 * 注解中的周期/启停/路由仅作为首次注册的默认值，运行期以调度中心数据库配置为准。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ManagedScheduled {

    /** 任务唯一标识，缺省为 appName:beanName.methodName */
    String id() default "";

    /** 显示名，缺省为方法名 */
    String name() default "";

    String description() default "";

    String group() default "default";

    ScheduleType type() default ScheduleType.CRON;

    /** type=CRON 时必填，Spring 6 位 cron（秒 分 时 日 月 周） */
    String cron() default "";

    /** type=FIXED_RATE 时使用，单位毫秒 */
    long fixedRate() default -1;

    /** type=FIXED_DELAY 时使用，单位毫秒 */
    long fixedDelay() default -1;

    /** 初始延迟，单位毫秒 */
    long initialDelay() default 0;

    /** 初始默认启停 */
    boolean enabled() default true;

    /** 注册默认路由策略 */
    RouteStrategy route() default RouteStrategy.FAILOVER;

    /** 注册默认阻塞策略 */
    BlockStrategy block() default BlockStrategy.SERIAL;
}