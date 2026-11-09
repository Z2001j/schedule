-- 调度中心建表脚本（MySQL；H2 可用 MODE=MySQL 兼容）。表名默认，可配置。
CREATE TABLE IF NOT EXISTS sched_task (
  task_id          VARCHAR(160) NOT NULL,
  app_name         VARCHAR(64)  NOT NULL,
  task_name        VARCHAR(128) NULL,
  task_group       VARCHAR(64)  NULL,
  schedule_type    VARCHAR(16)  NOT NULL,
  cron_expression  VARCHAR(128) NULL,
  fixed_rate       BIGINT       NULL,
  fixed_delay      BIGINT       NULL,
  initial_delay    BIGINT       NOT NULL DEFAULT 0,
  enabled          TINYINT      NOT NULL DEFAULT 1,
  route_strategy   VARCHAR(24)  NOT NULL DEFAULT 'FAILOVER',
  block_strategy   VARCHAR(16)  NOT NULL DEFAULT 'SERIAL',
  retry_count      INT          NOT NULL DEFAULT 0,
  description      VARCHAR(512) NULL,
  version          INT          NOT NULL DEFAULT 0,
  create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (task_id),
  KEY idx_app (app_name)
);

CREATE TABLE IF NOT EXISTS sched_executor (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  app_name        VARCHAR(64)  NOT NULL,
  address         VARCHAR(128) NOT NULL,
  status          VARCHAR(16)  NOT NULL DEFAULT 'ONLINE',
  last_heartbeat  TIMESTAMP    NOT NULL,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_app_addr (app_name, address)
);

CREATE TABLE IF NOT EXISTS sched_task_log (
  id               BIGINT        NOT NULL AUTO_INCREMENT,
  task_id          VARCHAR(160)  NOT NULL,
  trigger_time     TIMESTAMP     NOT NULL,
  trigger_type     VARCHAR(16)   NOT NULL,
  route_strategy   VARCHAR(24)   NULL,
  executor_address VARCHAR(128)  NULL,
  shard_index      INT           NULL,
  shard_total      INT           NULL,
  trigger_result   VARCHAR(16)   NULL,
  exec_start       TIMESTAMP     NULL,
  exec_end         TIMESTAMP     NULL,
  cost_ms          BIGINT        NULL,
  success          TINYINT       NULL,
  error_msg        VARCHAR(2000) NULL,
  PRIMARY KEY (id),
  KEY idx_task_time (task_id, trigger_time)
);

-- HA 选主锁表（scheduler.admin.ha-enabled=true 时使用；单实例部署可忽略）。
-- 与 SchemaInitializer 自动建表定义一致；多 admin 须共享同一数据库。
CREATE TABLE IF NOT EXISTS sched_leader (
  lock_name    VARCHAR(32) NOT NULL,
  instance_id  VARCHAR(64) NOT NULL,
  heartbeat    TIMESTAMP   NOT NULL,
  PRIMARY KEY (lock_name)
);