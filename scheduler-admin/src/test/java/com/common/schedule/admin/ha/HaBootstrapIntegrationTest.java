package com.common.schedule.admin.ha;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用独立空库启动 HA，验证建表先于选主（Bean 初始化顺序），单实例应成为 leader。
 */
@SpringBootTest(properties = {
        "scheduler.admin.ha-enabled=true",
        "scheduler.admin.election-interval=2",
        "scheduler.admin.leader-timeout=6",
        "spring.datasource.url=jdbc:h2:mem:hatest;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
class HaBootstrapIntegrationTest {

    @Autowired
    private AdminLeaderElection election;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void bootsOnEmptyDbAndBecomesLeader() {
        assertThat(election.isLeader()).isTrue();
        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM sched_leader WHERE lock_name='ADMIN'", Integer.class);
        assertThat(rows).isEqualTo(1);
    }
}