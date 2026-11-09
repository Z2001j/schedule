package com.common.schedule.admin.storage;

import com.common.schedule.admin.config.AdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

/**
 * 用独立内存 H2（MySQL 兼容模式）+ 自动建表支撑 DAO 集成测试，无需 Spring 上下文。
 */
abstract class AbstractDaoTest {

    protected JdbcTemplate jdbc;
    protected AdminProperties properties;

    @BeforeEach
    void initSchema() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        ds.setUsername("sa");
        ds.setPassword("");
        jdbc = new JdbcTemplate(ds);
        properties = new AdminProperties();
        properties.setAutoDdl(true);
        new SchemaInitializer(jdbc, properties).afterPropertiesSet();
    }
}