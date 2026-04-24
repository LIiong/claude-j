# 需求拆分设计 — 019-structured-logging

## 需求描述

实现结构化日志与请求追踪能力，为生产环境调试提供可追溯的日志链路：
1. JSON 格式日志输出（logback-spring.xml 配置）
2. Filter 生成 requestId 写入 MDC
3. 响应头回传 X-Request-Id
4. 关键业务日志自动携带 traceId

## 基础设施分析

### 任务性质
本任务为基础设施配置任务，不涉及 DDD 聚合建模。主要工作：
- logback-spring.xml 配置文件编写
- TraceIdFilter 实现（javax.servlet.Filter）
- 集成测试验证

### 现有依赖
- Spring Boot 2.7.18（内置 logback 1.2.x）
- slf4j-api（Spring Boot 自动引入）
- javax.servlet-api（Spring Boot 2.7 使用 javax 包，非 jakarta）
- logback-classic（Spring Boot starter 内置）

### 环境差异
| 环境 | 日志格式 | 日志级别 | 输出目标 |
|------|---------|---------|---------|
| dev | JSON（便于调试） | debug | console |
| staging | JSON | info | console |
| prod | JSON | warn | console + file（可选） |

## 技术方案

### 1. TraceId 生成策略
使用 UUID 替换 `-` 为空字符，生成 32 字符唯一标识：
```
requestId = UUID.randomUUID().toString().replace("-", "")
```
**备选方案**：
- NanoId（更短，但需引入依赖）— 不采用，保持最小依赖
- 时间戳 + 随机数（可能冲突）— 不采用，UUID 更可靠

### 2. MDC 注入点
在 `TraceIdFilter` 中：
- 请求进入时：生成 requestId，写入 MDC，设置响应头
- 请求结束时：清理 MDC（防止线程池复用导致的污染）

**关键代码结构**：
```java
public class TraceIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String requestId = generateRequestId();
        MDC.put("requestId", requestId);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("X-Request-Id", requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}
```

### 3. logback-spring.xml 配置
使用 logback 内建的 JSON encoder（logback-classic 1.2.x 无原生 JSON 支持，需引入 `logback-logstash-encoder`）。

**依赖选型**：
- `logback-logstash-encoder`（6.x）— 生产主流方案
- 自定义 JsonLayout — 不采用，维护成本高

**JSON 字段设计**：
| 字段 | 来源 | 说明 |
|------|------|------|
| timestamp | logback | ISO-8601 格式 |
| level | logback | INFO/WARN/ERROR 等 |
| logger | logback | 类名缩写 |
| message | logback | 日志内容 |
| requestId | MDC | 请求唯一标识 |
| thread | logback | 线程名 |
| mdc | MDC | 其他 MDC 字段 |

### 4. Filter 注册
通过 `FilterRegistrationBean` 显式注册，确保：
- 优先级最高（Order = -1）
- 排除 actuator 内部路径（可选）

## API 设计

本任务无新增 REST API，仅增加响应头：

| 响应头 | 值 | 描述 |
|--------|---|------|
| X-Request-Id | 32 字符 hex | 每个请求的唯一标识 |

## 配置设计

### application.yml 调整
无需新增配置项，日志格式由 logback-spring.xml 按 profile 控制。

### 新增文件
| 文件 | 位置 | 说明 |
|------|------|------|
| logback-spring.xml | claude-j-start/src/main/resources/ | 日志配置 |
| TraceIdFilter.java | claude-j-start/src/main/java/com/claudej/config/ | 请求追踪 Filter |
| TraceIdConfig.java | claude-j-start/src/main/java/com/claudej/config/ | Filter 注册配置 |

### pom.xml 调整
新增依赖（claude-j-start/pom.xml）：
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>6.6</version>
</dependency>
```

## 影响范围

本任务为基础设施配置，不涉及业务层：

- **domain**: 无变更
- **application**: 无变更
- **infrastructure**: 无变更
- **adapter**: 无变更（响应头通过 Filter 自动注入）
- **start**:
  - 新增 `logback-spring.xml`
  - 新增 `TraceIdFilter.java`
  - 新增 `TraceIdConfig.java`
  - 修改 `pom.xml`（添加 logstash-encoder 依赖）
  - 新增 `TraceIdIntegrationTest.java`

## 假设与待确认

1. **假设**：requestId 使用 UUID 生成，无需外部分布式 ID 服务
2. **假设**：仅输出 console，暂不实现文件滚动（prod 可后续扩展）
3. **假设**：actuator 端点也携带 requestId（便于健康检查调试）
4. **待确认**：是否需要支持客户端传入 `X-Request-Id` header 并复用（当前设计为服务端强制生成）

## 验收条件

1. 启动后日志为 JSON 格式，含 timestamp/level/logger/message/requestId 字段
2. 每个 HTTP 请求有唯一 requestId，响应头可见 X-Request-Id
3. 三项预飞（mvn test / checkstyle / entropy-check）全过
4. 集成测试验证 requestId 贯穿请求生命周期

### 测试用例映射
| 验收条件 | 测试方法 |
|----------|---------|
| JSON 格式日志 | 手动启动验证 + 集成测试检查日志输出 |
| requestId 响应头 | `should_return_x_request_id_header_when_any_request` |
| requestId 贯穿 | `should_same_request_id_in_mdc_through_request_lifecycle` |
| 三项预飞 | mvn test + checkstyle + entropy-check |

## 架构评审

**评审人**：@architect
**日期**：2026-04-24
**结论**：✅ 通过（含 2 项修正建议）

### 基线确认

**entropy-check.sh 退出码**：0（PASS）
**结果摘要**：0 FAIL, 12 WARN — 无阻塞性问题

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（本任务不涉及聚合，为基础设施配置任务）
- [x] 值对象识别充分（无新增值对象）
- [x] Repository 端口粒度合适（无新增 Repository）
- [x] 与已有聚合无循环依赖（仅改动 start 模块，不依赖 domain/application/infrastructure 业务层）
- [x] DDL 设计与领域模型一致（无新增表）
- [x] API 设计符合 RESTful 规范（无新增 REST API，仅增加响应头）
- [x] 对象转换链正确（无涉及 DO/DTO/Domain 转换）

**需求质量（3 项）**
- [x] 需求无歧义：JSON 日志格式、requestId 生成规则（UUID 32字符）、响应头名称（X-Request-Id）均有明确定义
- [x] 验收条件可验证：4 条验收条件均可转化为 `should_xxx_when_yyy` 测试
- [x] 业务规则完备：requestId 唯一性规则、MDC 清理时机（请求结束）、响应头设置时机均已明确

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：8 个原子任务，每项含文件路径 + 验证命令 + 预期输出 + commit 消息
- [x] 依赖顺序正确：pom 依赖 → logback 配置 → Filter 实现 → Filter 注册 → 集成测试 → 三项验证（顺序合理）
- ⚠️ **修正建议 1**：task-plan.md 的原子任务格式建议补齐「测试」字段（参考模板 `task-plan.template.md` 5 字段要求：文件路径、骨架片段、验证命令、预期输出、commit 消息）

**可测性保障（3 项）**
- [x] **AC 自动化全覆盖**：验收条件映射表 4 条均有测试方法（2 个 `@SpringBootTest` + 手动验证）
- [x] **可测的注入方式**：TraceIdConfig 使用 `FilterRegistrationBean` 构造函数注入，无需字段注入
- [x] **配置校验方式合规**：本任务不涉及敏感配置校验（不属于 ADR-005 覆盖范围）

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：设计仅包含必要组件（logback-spring.xml、TraceIdFilter、TraceIdConfig），无过度抽象
- [x] **外科性**：仅改动 start 模块，不涉及 domain/application/infrastructure/adapter 业务层
- [x] **假设显性**：requirement-design.md「假设与待确认」已列出 4 项假设 + 1 项待确认

### 评审意见

#### 1. 版本兼容性确认（✅）
- Spring Boot 2.7.18 → logback-classic:1.2.12（实测确认）
- logstash-logback-encoder 6.6 → 要求 logback 1.2+（官方兼容）
- **结论**：版本兼容性无问题

#### 2. Filter 注册方式确认（✅）
- 使用 `FilterRegistrationBean` + `new TraceIdFilter()` 是正确的
- TraceIdFilter 不依赖其他 Spring Bean，无需 `@Bean` 注入方式
- `Ordered.HIGHEST_PRECEDENCE` 确保 requestId 在其他 Filter 之前生成

#### 3. 与 actuator 配置兼容性（✅）
- TraceIdFilter 应用到 `/*`，包括 `/actuator/*`
- 符合设计意图「actuator 端点也携带 requestId（便于健康检查调试）」
- 无冲突：actuator 配置位于 application.yml，日志配置位于 logback-spring.xml，职责分离

#### 4. 设计文档错误（⚠️ 修正建议）
- **task-plan.md 中 logback-spring.xml 骨架有 XML 错误**：
  ```xml
  <!-- 错误 -->
  <level>level</timestamp>
  <!-- 应改为 -->
  <level>level</level>
  ```
- **影响**：Build 阶段复制此骨架会导致运行时日志级别字段被错误命名为 `timestamp`
- **建议**：@dev 在 Build 阶段实现时修正此错误，无需打回 Spec 阶段

#### 5. 集成测试数量合规（✅）
- 仅 2 个 `@SpringBootTest`（小于 3 个限制）
- 测试命名符合 `should_xxx_when_yyy` 规范

#### 6. Java 8 语法合规（✅）
- 使用 `UUID.randomUUID().toString().replace("-", "")` 而非 `var`
- 使用 `javax.servlet.Filter`（Spring Boot 2.7.x 使用 javax 包，非 jakarta）

### 需要新增的 ADR

**不需要新增 ADR**。本任务为基础设施配置任务，不涉及架构决策变更。logstash-logback-encoder 选型为生产主流方案，无需特殊记录。

### 修正要求

1. **Build 阶段修正**：task-plan.md 中 logback-spring.xml 的 `<level>level</timestamp>` 错误，@dev 实现时自行修正
2. **可选优化**：task-plan.md 原子任务格式补齐「测试」字段（轻微问题，不阻塞）