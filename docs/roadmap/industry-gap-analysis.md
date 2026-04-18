# claude-j 工业级功能缺口路线图

> **目的**：对照真实工业级电商系统，梳理 claude-j 现阶段缺失的能力，做成可增量落地的任务清单。本文档是 **长期可追踪** 的路线图，每项任务可单独拆出来走 `/ralph <task-id> <需求>` 流程实施。

> **状态**：半生产级升级路径 · 首次发布 2026-04-17

---

## 定位声明

claude-j 当前处于 **"DDD + 六边形架构教学/示范项目"** 阶段，目标升级为 **"半生产级示范"** —— 一个可真实部署、可对外演示、可作为企业内部 Java 工程脚手架参考的项目。

| 定位档 | 范围 | 对应本文档的取舍 |
|---|---|---|
| 教学项目（原定位） | Ralph 编排 + DDD 架构 + 14 ArchUnit + 基础 CRUD | 仅补 Top 10 少数项 |
| **半生产级示范（当前目标）** | P0 全做 + P1 必做 6 项（inventory/product/payment/领域事件/MQ/Testcontainers） | **本文档主线** |
| 真生产项目 | P0 + P1 全做 + 部分 P2 | 超出本阶段范围，本文档仅列不实施 |

**半生产级示范的完成标志**：任何人 clone 此项目 → 改 env → `docker-compose up` 就能得到一个带可观测性/授权/真库迁移/幂等下单/异步通知的完整电商后端，并能按 DDD 规范在上面加聚合。

---

## 现状快照

### ✅ 已具备
- **7 聚合**：shortlink / link / order / user / coupon / cart / auth
- **架构守护**：14 条 ArchUnit 规则 + 12 项 entropy-check + 3 层 Hook
- **API 基础**：`ApiResult<T>` 统一响应封装 + `GlobalExceptionHandler` + ~85 条 `ErrorCode`
- **认证**：JJWT 0.12.3 + BCrypt 密码哈希
- **CI**：GitHub Actions（ci.yml + entropy-check.yml + pr-review.yml）
- **测试**：JaCoCo 分层覆盖度阈值（domain 90% / application 80% / infra+adapter 70%）+ 10 个集成测试
- **容器**：Dockerfile（多阶段构建）+ docker-compose.yml（本地开发栈）
- **交付流程**：5 阶段 Ralph 编排 + 三子 Agent + 三条心智铁律 + 四项 Karpathy 原则

### ❌ 关键缺口（本文档主体）

按 5 大类组织，下节详述。

---

## 类别 A：业务功能补全

让"电商"真的像电商。

| # | 任务 | 优先级 | 工作量 | 价值 | 完成标志 |
|:---:|---|:---:|:---:|---|---|
| A1 | **补齐 Order ↔ Coupon 集成** | 🔴 P0 | 中 | 当前 Coupon 作为独立聚合存在但 Order 创建时 `couponId` 被忽略，是教学漏洞 | 创建订单能正确应用优惠、Coupon 标记 used、订单金额反映折扣 |
| A2 | **补齐 Order 状态机**（ship / deliver / refund） | 🔴 P0 | 中 | 仅 CREATED→PAID→CANCELLED，缺真实电商核心状态 | SHIPPED / DELIVERED / REFUNDED 状态 + API + 对应不变量测试 |
| A3 | **分页 & 排序**（所有 list 接口） | 🔴 P0 | 小 | 当前全表返回，真生产会 OOM | 基于 PageRequest / Pageable，Page\<T\> 响应，所有 listXxx 端点改造 |
| A4 | **库存聚合（Inventory）** | 🟡 P1-必做 | 大 | 电商缺库存等于没骨架；当前下单不扣库存 | 新建 `inventory` 聚合，订单创建时预占、支付时扣减、取消时回滚 |
| A5 | **订单项与产品聚合拆分** | 🟡 P1-必做 | 大 | 当前 `OrderItem` 手填 productId/price，无 Product 聚合 | 新建 `product` 聚合（SKU / 定价 / 上下架） |
| A6 | **支付聚合 + 支付状态回调** | 🟡 P1-必做 | 大 | 当前 `payOrder()` 只是改状态，无支付方/三方对接模型 | 新建 `payment` 聚合，抽象 PSP 接口（可 mock）+ 回调 webhook 端点 |
| A7 | **收货地址 / 物流跟踪** | 🟢 P2 | 中 | 订单缺收货信息是硬伤 | Address 值对象或 address 聚合，Order 持有收货信息 |
| A8 | **Shortlink 点击追踪 + 访问分析** | 🟢 P2 | 中 | Shortlink 只有创建/解析，无真实短链业务价值 | 点击日志聚合 + 统计接口 + 过期管理 API |
| A9 | **User 资料修改 + 密码重置流程完整化** | 🟡 P1-可选 | 小 | 邮箱/手机可改但无 API 暴露；Auth 密码重置的邮件验证码链路不清 | PATCH /users/{id}, 密码重置请求+确认 2 步 |
| A10 | **后台管理端点**（Admin API） | 🟢 P2 | 中 | 工业系统必有 /admin 命名空间 | 独立 `/api/v1/admin/**` 路由 + 角色校验 |
| A11 | **领域事件发布 + 跨聚合最终一致性** | 🟡 P1-必做 | 中 | 当前跨聚合靠 Application 直接调用，缺事件载体 | Spring Events 或 TransactionalOutbox，订单支付成功 → 库存扣减 / 优惠券核销由事件触发 |

---

## 类别 B：可观测性 & 可靠性

生产不可少。

| # | 任务 | 优先级 | 工作量 | 价值 | 完成标志 |
|:---:|---|:---:|:---:|---|---|
| B1 | **Spring Boot Actuator + 健康检查** | 🔴 P0 | 小 | K8s 就绪/存活探针的基础 | `/actuator/health`, `/actuator/info` 开放；liveness/readiness 分离 |
| B2 | **Micrometer + Prometheus 指标端点** | 🔴 P0 | 小 | 所有 JVM/HTTP/DB 指标标准化 | `/actuator/prometheus` + 关键业务指标（下单量、失败率） |
| B3 | **结构化日志 + MDC + TraceId** | 🔴 P0 | 中 | 当前只有 logback 默认 | JSON 日志 logback-spring.xml；filter 生成 requestId 写入 MDC；响应头回传 `X-Request-Id` |
| B4 | **分布式追踪**（OpenTelemetry / Sleuth） | 🟡 P1-可选 | 中 | 真微服务跨服务链路追踪 | 添加 OTel agent，所有 Controller/Service 有 span；Jaeger/Tempo 后端（docker-compose 里起） |
| B5 | **Resilience4j 熔断 + 重试 + 超时** | 🟡 P1-推荐 | 中 | 对支付 PSP 等外部依赖必须有 | `@CircuitBreaker @Retry @TimeLimiter` 在外部适配器方法上 |
| B6 | **API 限流**（bucket4j 或 Resilience4j RateLimiter） | 🟡 P1-可选 | 小 | 防刷接口 | 按 IP / userId 粒度，登录/下单/优惠券领取都有默认上限 |
| B7 | **优雅关闭**（graceful shutdown） | 🟢 P2 | 极小 | Spring Boot 内建，只需配置 | `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase` |
| B8 | **APM 接入示例**（SkyWalking / Pinpoint 文档） | 🟢 P2 | 小 | 教学示范 | `docs/ops/apm-skywalking.md` + docker-compose 可选 agent |

---

## 类别 C：安全 & 数据

| # | 任务 | 优先级 | 工作量 | 价值 | 完成标志 |
|:---:|---|:---:|:---:|---|---|
| C1 | **Spring Security + 授权** | 🔴 P0 | 中 | JWT 已有认证但无授权；登录后无任何访问控制，严重漏洞 | JwtAuthenticationFilter + `@PreAuthorize("hasRole('ADMIN')")` + 一套角色模型 |
| C2 | **密钥外置化** | 🔴 P0 | 小 | 现在 JWT secret 在 application.yml 明文 | 环境变量 / Spring Cloud Config / Vault 示例（可先做环境变量） |
| C3 | **CORS 配置** | 🔴 P0 | 极小 | 前后端分离必须 | `CorsConfigurationSource` bean + 可配置白名单 |
| C4 | **Flyway 数据库迁移** | 🔴 P0 | 小 | 当前手写 schema.sql，无版本管理 | `db/migration/V1__init.sql` 起步，CI 校验 migration 可从零重放 |
| C5 | **Redis 缓存接入 + @Cacheable 示例** | 🟡 P1-可选 | 中 | Session / 热点数据必要 | spring-data-redis + docker-compose 加 redis + Coupon/User 缓存示例 |
| C6 | **Session 管理改用 Redis**（Auth 聚合） | 🟡 P1-可选 | 中 | 当前 session 在 DB，多实例难 | Spring Session + Redis |
| C7 | **HikariCP 连接池调优 + 监控** | 🟢 P2 | 极小 | Production 必须调 maxPoolSize/leakDetection | application.yml 显式配置 + Micrometer 暴露连接池指标 |
| C8 | **请求幂等性支持** | 🟡 P1-推荐 | 中 | 下单/支付必备，防重复提交 | `Idempotency-Key` header + Redis 存储 + Filter |
| C9 | **OWASP Dependency Check / Snyk 扫描接入 CI** | 🟡 P1-推荐 | 小 | 供应链安全 | GitHub Actions 里新 job，发现 CVE 阻断构建 |
| C10 | **敏感数据脱敏** | 🟡 P1-可选 | 小 | 日志/响应中 phone / email / password 合规 | logback filter + Jackson `@JsonSerialize(using = SensitiveSerializer.class)` |
| C11 | **XSS / SQL 注入防护审计** | 🟢 P2 | 小 | MyBatis-Plus 默认安全但需审计 XML Mapper `${}` 用法 | 扫描 + ArchUnit 规则禁止 `${}` |

---

## 类别 D：消息、异步 & 集成

| # | 任务 | 优先级 | 工作量 | 价值 | 完成标志 |
|:---:|---|:---:|:---:|---|---|
| D1 | **消息队列接入** | 🟡 P1-必做 | 大 | 异步解耦必备；选 RocketMQ / Kafka / RabbitMQ 之一（实施时再定） | docker-compose 起 broker + 生产/消费示例（订单创建 → 通知服务） |
| D2 | **Transactional Outbox 模式** | 🟡 P1-可选 | 中 | 保证"本地事务 + 发消息"原子性 | outbox 表 + 定时/CDC 投递 + A11 事件基础 |
| D3 | **定时任务**（@Scheduled 或 Quartz） | 🟢 P2 | 小 | 优惠券过期清理 / 未支付订单取消 | 1 个 @Scheduled 示例 |
| D4 | **@Async 异步方法 + 专用线程池** | 🟢 P2 | 极小 | 发邮件/发短信不阻塞主流程 | `TaskExecutor` bean + Auth 注册场景改异步 |

---

## 类别 E：API 契约、测试、CI/CD、部署、文档

| # | 任务 | 优先级 | 工作量 | 价值 | 完成标志 |
|:---:|---|:---:|:---:|---|---|
| E1 | **OpenAPI / Swagger 文档**（springdoc-openapi） | 🔴 P0 | 小 | 对接方必备 | `/swagger-ui.html` + `/v3/api-docs`；每 Controller 有 `@Operation` |
| E2 | **API 版本策略文档** | 🟢 P2 | 极小 | 已有 `/v1` 前缀但策略未文档化 | `docs/standards/api-versioning.md` |
| E3 | **staging / prod 多环境配置** | 🔴 P0 | 小 | 当前仅 dev | application-staging.yml + application-prod.yml 占位 + `docs/ops/profiles.md` |
| E4 | **Kubernetes 部署清单** | 🟡 P1-可选 | 中 | 云原生示范；Deployment / Service / Ingress / ConfigMap / Secret | `deploy/k8s/*.yaml` + HPA 示例 |
| E5 | **Helm Chart** | 🟢 P2 | 中 | 更完整部署体验 | `deploy/helm/claude-j/` 含 values.yaml |
| E6 | **Testcontainers** 替代 H2 | 🟡 P1-必做 | 中 | H2 兼容问题会掩盖真 MySQL 行为 | Infrastructure 层集成测试切 Testcontainers MySQL 8 |
| E7 | **契约测试**（Spring Cloud Contract 或 Pact） | 🟢 P2 | 中 | 多服务演化必备 | Producer 端 `@AutoConfigureStubRunner` 示例 |
| E8 | **Gatling / K6 压测脚本** | 🟢 P2 | 中 | 性能基线 | `perf/gatling/OrderCreateSimulation.scala` + CI nightly job |
| E9 | **Dependabot 或 Renovate** | 🟡 P1-可选 | 极小 | 依赖更新自动化 | `.github/dependabot.yml` |
| E10 | **Docker 镜像发布到 GHCR / Docker Hub** | 🟡 P1-可选 | 小 | CI 产物沉淀 | CI 里 `docker buildx` + push tag（main/tag 触发） |
| E11 | **CHANGELOG.md + 语义化版本** | 🟢 P2 | 小 | 规范化发布 | 采用 Keep a Changelog 格式 + release-please 或人工维护 |
| E12 | **LICENSE / CONTRIBUTING.md / CODE_OF_CONDUCT.md** | 🟢 P2 | 极小 | 开源项目标配 | 三文件就位 |
| E13 | **Issue / PR 模板** | 🟢 P2 | 极小 | 协作效率 | `.github/ISSUE_TEMPLATE/*` + `pull_request_template.md` |
| E14 | **JMH 基准测试模块** | 🟢 P2 | 中 | 性能敏感代码基线 | `claude-j-benchmark/` 模块 + 1 个基准示例 |
| E15 | **变异测试（PIT）** | 🟢 P2 | 小 | 测试有效性度量 | pom.xml 添加 PIT plugin + CI 可选 job |

---

## 优先级汇总

### 🔴 P0（12 项）— 必做

`A1 / A2 / A3 / B1 / B2 / B3 / C1 / C2 / C3 / C4 / E1 / E3`

**理由**：安全底线（授权 / 密钥 / CORS）+ 可观测基线（health / metrics / 日志）+ 数据迁移（Flyway）+ 业务功能完备性（Coupon 集成、订单状态机、分页）+ API 自文档（OpenAPI）。没有这批，任何真实部署都不能谈。

### 🟡 P1 — 半生产定位下分两档

**P1-必做（6 项）**：`A5 / A4 / A6 / A11 / D1 / E6`

构成 "半生产电商骨架" 的必要条件：三聚合（产品/库存/支付）+ 领域事件 + 消息队列 + Testcontainers 替代 H2。

**P1-推荐（3 项）**：`B5 / C8 / C9`（熔断 / 幂等 / 依赖扫描）

分布式可靠性最小集；做完这 3 项 + P1-必做 = 可对外演示的"生产雏形"。

**P1-可选（7 项）**：`A9 / B4 / B6 / C5 / C6 / C10 / D2 / E4 / E9 / E10`

按真实部署压力决定是否补。

### 🟢 P2（15 项）— 按需

`A7 / A8 / A10 / B7 / B8 / C7 / C11 / D3 / D4 / E2 / E5 / E7 / E8 / E11 / E12 / E13 / E14 / E15`

半生产定位下建议仅补 `E11 / E12`（CHANGELOG / LICENSE 等治理文档），其余暂缓。

---

## 🎯 Top 10 必做视图（浓缩快速决策）

按「价值密度 / 风险 / 依赖顺序」排序的 10 项最高回报任务。**做完这 10 项即达到 "生产级 DDD 示范" 标杆**，约 1.5–2.5 周工作量，ROI 最高。

| 顺序 | 任务 | 类型 | 价值 | 依赖 |
|:---:|---|---|---|---|
| **1** | **C2 密钥外置化** | 安全 | 现在 JWT secret 硬编码在 YAML 里，最高风险项，1 小时能消灭 | 无 |
| **2** | **C4 Flyway 迁移** | 数据 | 没这个不可能多环境部署；schema.sql 手写是倒退 | 无 |
| **3** | **E3 多 profile**（dev/staging/prod） | 部署 | 分开配置是所有后续配置项的前提 | 无 |
| **4** | **B1 Actuator + B2 Prometheus** | 可观测 | K8s 探针与指标采集的基础，一起做只半天 | 无 |
| **5** | **B3 结构化日志 + TraceId** | 可观测 | 没有 requestId 贯穿，生产 bug 不可查 | 无 |
| **6** | **C1 Spring Security + 授权** | 安全 | 登录了但没授权 = 裸奔；接入 JWT Filter + @PreAuthorize | C2 |
| **7** | **E1 OpenAPI 文档** | API | 前后端分离/合作方对接必备，springdoc 配一次即可 | 无 |
| **8** | **A3 分页** | 业务 | 现在全表返回，上生产第一个 OOM 现场 | 无 |
| **9** | **A1 Coupon↔Order 集成** | 业务 | 当前教学漏洞（couponId 被忽略）；补上才算完整电商 | 无 |
| **10** | **A2 订单完整状态机**（SHIPPED/DELIVERED/REFUNDED） | 业务 | 电商核心；不做就不算电商 | A1 |

**做完 Top 10 之后**，按批 3（A4/A5/A6 三聚合）升级至"半生产电商骨架"。

---

## 建议实施批次

| 批次 | 范围 | 项数 | 工作量估算 |
|:---:|---|:---:|:---:|
| 批 1 | P0 基础设施：E3 → C2 → C3 → C4 → B1 → B2 → B3 → E1 → C1 | 9 | 5–7 天 |
| 批 2 | P0 业务修补：A3 → A1 → A2 | 3 | 3–4 天 |
| 批 3 | P1 必做 电商骨架：A5 → A4 → A6 → A11 → D1 → E6 | 6 | 10–14 天 |
| 批 4 | P1 推荐：B5 → C8 → C9 | 3 | 3–4 天 |
| 批 5 | P1 可选 / P2 | 按需 | 按业务方向挑选 |

---

## 如何用 Ralph 实施

每一行任务映射为一个 task，按现有 Ralph 流程落地：

```bash
# 示例：落地 Top 10 第 1 项（C2 密钥外置化）
/ralph 010-secret-externalize "将 JWT secret 从 application.yml 外置化：
优先从环境变量 JWT_SECRET 读取，YAML 中只保留占位符；
本地开发配 application-dev.yml 默认值；
补一份 docs/ops/secrets.md 说明生产接入 Vault/配置中心的扩展路径。
验收条件：
1. 删除 application.yml 中明文 secret 后应用仍可启动（通过 env 变量）
2. 三项预飞（mvn test / checkstyle / entropy-check）全过
3. 新增 docs/ops/secrets.md"

# 示例：落地 P1 必做 A5 Product 聚合
/ralph 011-product-aggregate "新建 product 聚合（SKU / 定价 / 上下架状态机）：
- Domain: Product 聚合根 + SKU 值对象 + ProductStatus 枚举
- Application: 上架/下架/调价/查询 用例
- Infrastructure: ProductRepository + DO + Converter + MyBatis Mapper
- Adapter: REST 端点 /api/v1/products/**
- DDL: t_product 表，挂到 Flyway 迁移
- 与 Order.OrderItem 解耦：OrderItem 仅持 productId + 下单快照价，不耦合 Product 状态"
```

**原则**：
- 每个任务独立 Spec→Review→Build→Verify→Ship
- 每项 ≤ 15 min 原子任务切分（沿用现有 task-plan 模板）
- 三项预飞通过 + JaCoCo 阈值不下滑才可交接
- 批次间可穿插，但批次内顺序建议保持（批 1/批 3 有依赖关系）

---

## 变更记录

| 日期 | 变更 | 触发 |
|---|---|---|
| 2026-04-17 | 首次发布。基于对照"工业级电商系统"的 3 Agent 并行探查结果，给出 A/B/C/D/E 五类共 49 项任务 + Top 10 视图 + 5 批实施顺序。 | 初稿 |

<!-- 之后每补完一项，将 ✅ 状态与 commit hash 写到「变更记录」，并在任务表「完成标志」列打勾 -->
