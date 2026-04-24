# 任务执行计划 — 019-structured-logging

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | pom.xml 添加 logstash-logback-encoder 依赖 | dev | 待办 | |
| 2 | logback-spring.xml 配置 JSON 格式日志 | dev | 待办 | |
| 3 | TraceIdFilter 实现 requestId 生成与 MDC 注入 | dev | 待办 | |
| 4 | TraceIdConfig 注册 Filter Bean | dev | 待办 | |
| 5 | TraceIdIntegrationTest 集成测试 | dev | 待办 | |
| 6 | 全量 mvn test | dev | 待办 | |
| 7 | mvn checkstyle:check | dev | 待办 | |
| 8 | ./scripts/entropy-check.sh | dev | 待办 | |
| 9 | QA: 测试用例设计 | qa | 待办 | |
| 10 | QA: 验收测试 + 代码审查 | qa | 待办 | |

## 执行顺序

依赖配置 → 日志配置 → Filter 实现 → Filter 注册 → 集成测试 → 三项验证 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

### 1.1 pom.xml 添加依赖
- **文件**：`claude-j-start/pom.xml`
- **骨架**：
  ```xml
  <dependency>
      <groupId>net.logstash.logback</groupId>
      <artifactId>logstash-logback-encoder</artifactId>
      <version>6.6</version>
  </dependency>
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：BUILD SUCCESS
- **commit**：`feat(start): 添加 logstash-logback-encoder 依赖`

### 2.1 logback-spring.xml 配置
- **文件**：`claude-j-start/src/main/resources/logback-spring.xml`
- **骨架**：
  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <configuration>
    <!-- Spring profile 区分 -->
    <springProfile name="dev,staging,prod">
      <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
          <includeMdcKeyName>requestId</includeMdcKeyName>
          <fieldNames>
            <timestamp>timestamp</timestamp>
            <level>level</timestamp>
            <logger>logger</logger>
            <message>message</message>
            <thread>thread</thread>
          </fieldNames>
        </encoder>
      </appender>
      <root level="${LOG_LEVEL:-info}">
        <appender-ref ref="JSON_CONSOLE"/>
      </root>
    </springProfile>
  </configuration>
  ```
- **验证命令**：`mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev`（手动验证 JSON 输出）
- **预期输出**：启动日志为 JSON 格式
- **commit**：`feat(start): JSON 格式日志配置`

### 3.1 TraceIdFilter 实现
- **文件**：`claude-j-start/src/main/java/com/claudej/config/TraceIdFilter.java`
- **骨架**：
  ```java
  package com.claudej.config;

  import javax.servlet.Filter;
  import javax.servlet.FilterChain;
  import javax.servlet.FilterConfig;
  import javax.servlet.ServletException;
  import javax.servlet.ServletRequest;
  import javax.servlet.ServletResponse;
  import javax.servlet.http.HttpServletResponse;
  import java.util.UUID;
  import org.slf4j.MDC;

  public class TraceIdFilter implements Filter {
      private static final String REQUEST_ID_HEADER = "X-Request-Id";
      private static final String REQUEST_ID_MDC_KEY = "requestId";

      @Override
      public void init(FilterConfig filterConfig) throws ServletException {}

      @Override
      public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
              throws java.io.IOException, ServletException {
          String requestId = generateRequestId();
          MDC.put(REQUEST_ID_MDC_KEY, requestId);
          HttpServletResponse httpResponse = (HttpServletResponse) response;
          httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
          try {
              chain.doFilter(request, response);
          } finally {
              MDC.remove(REQUEST_ID_MDC_KEY);
          }
      }

      @Override
      public void destroy() {}

      private String generateRequestId() {
          return UUID.randomUUID().toString().replace("-", "");
      }
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：BUILD SUCCESS
- **commit**：`feat(start): TraceIdFilter 实现 requestId 生成与 MDC 注入`

### 4.1 TraceIdConfig 注册 Filter
- **文件**：`claude-j-start/src/main/java/com/claudej/config/TraceIdConfig.java`
- **骨架**：
  ```java
  package com.claudej.config;

  import org.springframework.boot.web.servlet.FilterRegistrationBean;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.core.Ordered;

  @Configuration
  public class TraceIdConfig {

      @Bean
      public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
          FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
          registration.setFilter(new TraceIdFilter());
          registration.addUrlPatterns("/*");
          registration.setName("traceIdFilter");
          registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
          return registration;
      }
  }
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：BUILD SUCCESS
- **commit**：`feat(start): TraceIdConfig 注册 Filter Bean`

### 5.1 TraceIdIntegrationTest 集成测试
- **文件**：`claude-j-start/src/test/java/com/claudej/logging/TraceIdIntegrationTest.java`
- **骨架**：
  ```java
  package com.claudej.logging;

  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.boot.test.web.client.TestRestTemplate;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ResponseEntity;
  import org.springframework.test.context.ActiveProfiles;

  import static org.assertj.core.api.Assertions.assertThat;

  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("dev")
  class TraceIdIntegrationTest {

      @Autowired
      private TestRestTemplate restTemplate;

      @Test
      void should_return_x_request_id_header_when_any_request() {
          ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
          assertThat(response.getHeaders().containsKey("X-Request-Id")).isTrue();
          String requestId = response.getHeaders().getFirst("X-Request-Id");
          assertThat(requestId).hasSize(32);
          assertThat(requestId).matches("[a-f0-9]{32}");
      }

      @Test
      void should_have_different_request_id_for_different_requests() {
          ResponseEntity<String> response1 = restTemplate.getForEntity("/actuator/health", String.class);
          ResponseEntity<String> response2 = restTemplate.getForEntity("/actuator/health", String.class);
          String requestId1 = response1.getHeaders().getFirst("X-Request-Id");
          String requestId2 = response2.getHeaders().getFirst("X-Request-Id");
          assertThat(requestId1).isNotEqualTo(requestId2);
      }
  }
  ```
- **验证命令**：`mvn test -pl claude-j-start -Dtest=TraceIdIntegrationTest`
- **预期输出**：Tests run: 2, Failures: 0, Errors: 0
- **commit**：`test(start): TraceId 集成测试`

### 6.1 全量 mvn test
- **验证命令**：`mvn test`
- **预期输出**：所有测试通过（含 ArchUnit）
- **commit**：无需单独 commit（已在各原子任务 commit）

### 7.1 mvn checkstyle:check
- **验证命令**：`mvn checkstyle:check`
- **预期输出**：BUILD SUCCESS
- **commit**：无需单独 commit

### 8.1 entropy-check.sh
- **验证命令**：`./scripts/entropy-check.sh`
- **预期输出**：12/12 checks passed
- **commit**：无需单独 commit

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：待填写
- 架构合规检查：待填写
- 通知 @qa 时间：待填写

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：待填写
- 代码审查结果：待填写
- 代码风格检查：待填写
- 问题清单：详见 test-report.md
- **最终状态**：待填写