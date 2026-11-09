package com.common.schedule.admin.ha;

import com.common.schedule.admin.config.AdminProperties;
import com.common.schedule.admin.storage.SchemaInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLeaderElectionTest {

    private JdbcTemplate jdbc;
    private AdminProperties properties;

    @BeforeEach
    void setup() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        ds.setUsername("sa");
        ds.setPassword("");
        jdbc = new JdbcTemplate(ds);
        properties = new AdminProperties();
        properties.setAutoDdl(true);
        properties.setHaEnabled(true);
        properties.setLeaderTimeout(30);
        new SchemaInitializer(jdbc, properties).afterPropertiesSet();
    }

    private AdminLeaderElection newElection() {
        return new AdminLeaderElection(properties, jdbc);
    }

    @Test
    void firstInstanceBecomesLeader() {
        AdminLeaderElection a = newElection();
        a.tryAcquire();
        assertThat(a.isLeader()).isTrue();
    }

    @Test
    void secondInstanceCannotStealFreshLock() {
        AdminLeaderElection a = newElection();
        AdminLeaderElection b = newElection();
        a.tryAcquire();
        b.tryAcquire();
        assertThat(a.isLeader()).isTrue();
        assertThat(b.isLeader()).isFalse();
    }

    @Test
    void renewKeepsLeadership() {
        AdminLeaderElection a = newElection();
        a.tryAcquire();
        a.tryAcquire();
        assertThat(a.isLeader()).isTrue();
    }

    @Test
    void followerTakesOverAfterLeaderHeartbeatExpires() {
        AdminLeaderElection a = newElection();
        AdminLeaderElection b = newElection();
        a.tryAcquire();
        assertThat(a.isLeader()).isTrue();

        // 模拟 leader 宕机：心跳停更，超过 leaderTimeout
        jdbc.update("UPDATE " + properties.getLeaderTableName() + " SET heartbeat=? WHERE lock_name='ADMIN'",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(120)));

        b.tryAcquire();
        assertThat(b.isLeader()).isTrue();

        // 旧 leader 恢复后发现锁已易主，不再是 leader
        a.tryAcquire();
        assertThat(a.isLeader()).isFalse();
    }

    @Test
    void disabledHaIsAlwaysLeaderWithoutDb() {
        properties.setHaEnabled(false);
        AdminLeaderElection a = newElection();
        assertThat(a.isLeader()).isTrue();
        a.tryAcquire();
        assertThat(a.isLeader()).isTrue();
    }
}