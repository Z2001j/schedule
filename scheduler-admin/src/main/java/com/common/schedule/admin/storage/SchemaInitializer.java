package com.common.schedule.admin.storage;

import com.common.schedule.admin.config.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 自动建表（auto-ddl=true）。表名取自配置。生产建议关闭并由 DBA 预建表。
 */
@Component
public class SchemaInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final JdbcTemplate jdbc;
    private final AdminProperties properties;

    public SchemaInitializer(JdbcTemplate jdbc, AdminProperties properties) {
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (!properties.isAutoDdl()) {
            return;
        }
        String task = properties.getTableName();
        String executor = properties.getExecutorTableName();
        String logTable = properties.getLogTableName();

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + task + " ("
                + "task_id VARCHAR(160) NOT NULL,"
                + "app_name VARCHAR(64) NOT NULL,"
                + "task_name VARCHAR(128) NULL,"
                + "task_group VARCHAR(64) NULL,"
                + "schedule_type VARCHAR(16) NOT NULL,"
                + "cron_expression VARCHAR(128) NULL,"
                + "fixed_rate BIGINT NULL,"
                + "fixed_delay BIGINT NULL,"
                + "initial_delay BIGINT NOT NULL DEFAULT 0,"
                + "enabled TINYINT NOT NULL DEFAULT 1,"
                + "route_strategy VARCHAR(24) NOT NULL DEFAULT 'FAILOVER',"
                + "block_strategy VARCHAR(16) NOT NULL DEFAULT 'SERIAL',"
                + "retry_count INT NOT NULL DEFAULT 0,"
                + "description VARCHAR(512) NULL,"
                + "version INT NOT NULL DEFAULT 0,"
                + "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (task_id))");

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + executor + " ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "app_name VARCHAR(64) NOT NULL,"
                + "address VARCHAR(128) NOT NULL,"
                + "status VARCHAR(16) NOT NULL DEFAULT 'ONLINE',"
                + "last_heartbeat TIMESTAMP NOT NULL,"
                + "create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "PRIMARY KEY (id),"
                + "UNIQUE (app_name, address))");

        jdbc.execute("CREATE TABLE IF NOT EXISTS " + logTable + " ("
                + "id BIGINT NOT NULL AUTO_INCREMENT,"
                + "task_id VARCHAR(160) NOT NULL,"
                + "trigger_time TIMESTAMP NOT NULL,"
                + "trigger_type VARCHAR(16) NOT NULL,"
                + "route_strategy VARCHAR(24) NULL,"
                + "executor_address VARCHAR(128) NULL,"
                + "shard_index INT NULL,"
                + "shard_total INT NULL,"
                + "trigger_result VARCHAR(16) NULL,"
                + "exec_start TIMESTAMP NULL,"
                + "exec_end TIMESTAMP NULL,"
                + "cost_ms BIGINT NULL,"
                + "success TINYINT NULL,"
                + "error_msg VARCHAR(2000) NULL,"
                + "PRIMARY KEY (id))");

        log.info("[scheduler-admin] 自动建表完成: {}, {}, {}", task, executor, logTable);

        if (properties.isHaEnabled()) {
            String leader = properties.getLeaderTableName();
            jdbc.execute("CREATE TABLE IF NOT EXISTS " + leader + " ("
                    + "lock_name VARCHAR(32) NOT NULL,"
                    + "instance_id VARCHAR(64) NOT NULL,"
                    + "heartbeat TIMESTAMP NOT NULL,"
                    + "PRIMARY KEY (lock_name))");
            log.info("[scheduler-admin] HA 选主锁表完成: {}", leader);
        }
    }
}