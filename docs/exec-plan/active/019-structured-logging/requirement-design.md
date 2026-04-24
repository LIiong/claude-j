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