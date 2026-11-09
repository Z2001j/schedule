# 通用定时任务调度系统 · 文档索引

一个**调度与执行分离**的轻量定时任务调度系统（模式二），由两个独立交付件组成：

- **scheduler-admin（调度中心）**：独立部署的 Spring Boot 服务 + Vue3 控制台，负责配置管理、到点触发、执行器注册发现、路由选择、日志聚合。
- **scheduler-executor-starter（执行器 SDK）**：可复制/自动装配的 starter，嵌入业务服务；方法加 `@ManagedScheduled` 即注册，接收触发后本地执行。

仓库另含 **scheduler-demo（集成示例）**：嵌入执行器 SDK 的最小业务服务，提供 `@ManagedScheduled` 示例任务（Cron/固定频率/分片广播），用于与调度中心端到端联调，非交付件。

业务服务加注解 → 执行器 HTTP 注册到调度中心 → 调度中心存配置并到点触发 → 路由选执行器 → HTTP 下发 → 执行器本地执行 → 回调结果。**改周期/启停只在调度中心，业务服务无感、无需重启；集群由调度中心统一路由协调。**

## 阅读顺序

| # | 文档 | 内容 | 面向 |
|---|---|---|---|
| 01 | [需求文档](./01-需求文档.md) | 背景、功能/非功能需求、注册/路由/通信用例、验收 | 全员 |
| 02 | [设计文档](./02-设计文档.md) | 双模块架构、注册/心跳/触发/回调、路由策略、存储、HA | 架构/后端 |
| 03 | [原型页面](./03-原型页面.html) | 调度中心控制台原型（任务管理/执行器监控/日志抽屉） | 产品/前端 |
| 04 | [UX方案](./04-UX方案.md) | 控制台信息架构、流程、路由交互、五态、视觉规范 | 设计/前端 |
| 05 | [开发设计](./05-开发设计.md) | admin/executor 包结构、类设计、REST 契约、路由实现、DDL | 后端 |
| 06 | [开发文档](./06-开发文档.md) | 部署调度中心、复制执行器、注解/路由示例、Vue3 控制台、FAQ | 集成/前端 |

## 关键决策

- **架构**：模式二，自研调度中心 + 执行器 SDK（调度集中、执行就地）。
- **通信**：HTTP REST（注册/心跳/触发/回调），accessToken 双向校验。
- **集群**：可配置路由策略（FIRST/LAST/ROUND/RANDOM/CONSISTENT_HASH/FAILOVER/SHARDING_BROADCAST）。
- **持久化**：仅数据库（调度中心侧 `sched_task`/`sched_executor`/`sched_task_log`）。
- **前端**：Vue3 + Element Plus 控制台（隶属调度中心）。
- **调度类型**：Cron + 固定频率 + 固定延迟。
- **基线**：Spring Boot 3.x / JDK 17（兼容 Boot 2.7 / JDK 8）。

## 核心概念速览

```
@ManagedScheduled（业务服务）
  → 执行器扫描 + HTTP 注册/心跳 → 调度中心
  → 调度中心 DB 存配置 + ThreadPoolTaskScheduler 到点触发
  → ExecutorRouter 路由选实例 → HTTP 下发 TriggerParam
  → 执行器 JobThread 本地执行（异常隔离/阻塞策略/分片上下文）
  → HTTP 回调结果 → 调度中心记日志 → Vue3 控制台治理
```

## 实现状态

| 模块 | 状态 | 验证 |
|---|---|---|
| scheduler-executor-starter（M2） | 已完成 | 12 项单元测试通过，jar 已 install 到本地仓库 |
| scheduler-admin（M3） | 已完成 | 57 项测试通过（含 H2 集成 + 全上下文 MockMvc + HA），可执行 jar |
| scheduler-demo + 端到端联调（M4） | 已完成 | admin+demo 真实 HTTP 联调：注册/心跳、Cron 触发→执行→回调、分片广播、固定频率启停、运行中改周期均验证通过 |
| Vue3 控制台落地（M5） | 已完成 | scheduler-console 工程（Vite5+Vue3+Element Plus）build 通过；浏览器联调：任务列表、编辑改周期即时生效（乐观锁版本递增）、手动触发→回调日志、执行器监控均验证 |
| 调度中心 HA（M6，AdminLeaderElection） | 已完成 | DB 单行锁+心跳选主；双 JVM 共享 H2 文件库验证：单 leader、follower 待机、leader 宕机自动接管；见 [设计文档](./02-设计文档.md) §9.2 |
| 安全与治理补全 | 已完成 | 管理 API 令牌鉴权（默认开启，匿名/错误令牌 401）；服务端过滤 appName/keyword/group/enabled；AlarmNotifier 告警扩展点（默认日志）；sched_leader 建表脚本补全。均经测试与真实 HTTP/浏览器验证 |

## 本地端到端运行

```bash
# 1. 安装执行器 SDK 到本地仓库
cd scheduler-executor-starter && mvn install -DskipTests

# 2. 启动调度中心（默认内嵌 H2，端口 8080，令牌 changeme）
cd ../scheduler-admin && mvn package -DskipTests
java -jar target/scheduler-admin.jar

# 3. 启动示例执行器（端口 9999，自动注册到调度中心）
cd ../scheduler-demo && mvn package -DskipTests
java -jar target/scheduler-demo.jar

# 4. 启动 Vue3 控制台（可选，端口 5173，/api 代理到 8080）
#    控制台默认携带令牌 changeme；生产用 VITE_SCHED_TOKEN 注入与 admin 一致的管理令牌
cd ../scheduler-console && npm install && npm run dev   # 浏览器访问 http://localhost:5173

# 5. 治理接口（管理 API 默认开启令牌鉴权，须携带 X-Access-Token）
curl -H "X-Access-Token: changeme" http://localhost:8080/api/tasks                 # 任务列表
curl -H "X-Access-Token: changeme" "http://localhost:8080/api/tasks?appName=demo-app&enabled=true"  # 服务端过滤
curl -H "X-Access-Token: changeme" http://localhost:8080/api/executors             # 执行器监控
curl -H "X-Access-Token: changeme" -X POST http://localhost:8080/api/tasks/demo-app:sampleJobs.shardingJob/trigger   # 手动触发
```

> 生产：调度中心切 `mysql` profile 并修改 `scheduler.admin.access-token`；执行器 `scheduler.executor.access-token` 与之保持一致，且 `scheduler.executor.port` 须等于业务服务 `server.port`（调度中心按此地址回调）。