package com.common.schedule.executor.registrar;

import com.common.schedule.executor.annotation.ManagedScheduled;
import com.common.schedule.executor.client.AdminClient;
import com.common.schedule.executor.client.HeartbeatWorker;
import com.common.schedule.executor.config.ExecutorProperties;
import com.common.schedule.executor.execute.JobHandler;
import com.common.schedule.executor.execute.JobHandlerRegistry;
import com.common.schedule.executor.model.RegisterRequest;
import com.common.schedule.executor.model.TaskMeta;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 所有单例 Bean 就绪后扫描 @ManagedScheduled 方法，注册到本地句柄表并向调度中心注册，随后启动心跳。
 */
public class ExecutorRegistrar implements SmartInitializingSingleton, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(ExecutorRegistrar.class);

    private final JobHandlerRegistry registry;
    private final AdminClient adminClient;
    private final HeartbeatWorker heartbeatWorker;
    private final ExecutorProperties properties;

    private ApplicationContext applicationContext;

    public ExecutorRegistrar(JobHandlerRegistry registry, AdminClient adminClient,
                             HeartbeatWorker heartbeatWorker, ExecutorProperties properties) {
        this.registry = registry;
        this.adminClient = adminClient;
        this.heartbeatWorker = heartbeatWorker;
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        String appName = properties.getAppName();
        List<TaskMeta> metas = new ArrayList<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                continue;
            }
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                ManagedScheduled ann = AnnotatedElementUtils.findMergedAnnotation(method, ManagedScheduled.class);
                if (ann == null) {
                    continue;
                }
                validate(method, beanName);
                String taskId = ann.id().isEmpty()
                        ? appName + ":" + beanName + "." + method.getName()
                        : ann.id();
                method.setAccessible(true);
                registry.register(new JobHandler(taskId, bean, method, ann, adminClient));
                metas.add(TaskMeta.from(taskId, beanName, method.getName(), ann));
                log.info("[scheduler-executor] 纳管任务 {} -> {}.{}", taskId, targetClass.getSimpleName(), method.getName());
            }
        }

        registry.startAll();
        String address = resolveAddress();
        boolean registered = adminClient.register(new RegisterRequest(appName, address, metas));
        log.info("[scheduler-executor] 注册到调度中心 app={} address={} tasks={} result={}",
                appName, address, metas.size(), registered ? "成功" : "失败（心跳将持续重试）");
        heartbeatWorker.start(address);
    }

    private void validate(Method method, String beanName) {
        if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0) {
            throw new IllegalStateException("@ManagedScheduled 方法须为 public 无参: "
                    + beanName + "." + method.getName());
        }
    }

    private String resolveAddress() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            host = "127.0.0.1";
        }
        return "http://" + host + ":" + properties.getPort();
    }

    @PreDestroy
    public void destroy() {
        heartbeatWorker.stop();
        registry.stopAll();
        log.info("[scheduler-executor] 执行器已停止 app={}", properties.getAppName());
    }
}