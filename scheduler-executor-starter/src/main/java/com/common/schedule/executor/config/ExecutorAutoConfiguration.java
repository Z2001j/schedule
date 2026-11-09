package com.common.schedule.executor.config;

import com.common.schedule.executor.client.AdminClient;
import com.common.schedule.executor.client.HeartbeatWorker;
import com.common.schedule.executor.endpoint.ExecutorController;
import com.common.schedule.executor.execute.JobHandlerRegistry;
import com.common.schedule.executor.registrar.ExecutorRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 执行器 SDK 自动装配。复制本包 + 注册文件并配置 scheduler.executor 即可启用。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "scheduler.executor", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ExecutorProperties.class)
public class ExecutorAutoConfiguration {

    @Bean
    public JobHandlerRegistry jobHandlerRegistry() {
        return new JobHandlerRegistry();
    }

    @Bean
    public AdminClient adminClient(ExecutorProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);
        return new AdminClient(restTemplate, properties.getAdminAddresses(), properties.getAccessToken());
    }

    @Bean
    public HeartbeatWorker heartbeatWorker(AdminClient adminClient, ExecutorProperties properties) {
        return new HeartbeatWorker(adminClient, properties.getAppName(), properties.getHeartbeatInterval());
    }

    @Bean
    public ExecutorRegistrar executorRegistrar(JobHandlerRegistry registry, AdminClient adminClient,
                                               HeartbeatWorker heartbeatWorker, ExecutorProperties properties) {
        return new ExecutorRegistrar(registry, adminClient, heartbeatWorker, properties);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ExecutorController executorController(JobHandlerRegistry registry, ExecutorProperties properties) {
        return new ExecutorController(registry, properties);
    }
}