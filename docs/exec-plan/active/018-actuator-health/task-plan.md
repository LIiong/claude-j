# 任务执行计划 — 018-actuator-health

## 任务性质说明

> **本任务为纯基础设施配置任务**，不涉及业务聚合，无需按传统 DDD 分层开发。
> 任务拆解聚焦于配置文件修改与集成测试验证。

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Start: application.yml 基础 actuator 配置 | dev | 单测通过 | 启用 probes + 健康组配置 |
| 2 | Start: application-dev.yml 端点扩展 | dev | 单测通过 | 添加 metrics/env 端点 |
| 3 | Start: application-staging.yml 端点调整 | dev | 单测通过 | 调整 show-details |
| 4 | Start: application-prod.yml 最小化端点 | dev | 单测通过 | 仅保留 health/liveness/readiness |
| 5 | Start: 集成测试 ActuatorHealthIntegrationTest | dev | 单测通过 | 6 个测试全部通过 |
| 6 | 全量 mvn test | dev | 单测通过 | 58 个测试全部通过 |
| 7 | QA: 测试用例设计 | qa | 待办 | |
| 8 | QA: 验收测试 + 代码审查 | qa | 待验收 | |

## 执行顺序

由于是配置任务，无业务代码分层依赖，可按环境配置顺序执行：
```
base-config → dev-config → staging-config → prod-config → integration-test → full-test → QA
```

## 原子任务分解（每项 10–15 分钟）

> **要求**：每个原子任务必填 5 个字段 — `文件路径`、`修改内容`、`验证命令`、`预期输出`、`commit 消息`。

### 1.1 application.yml 基础 actuator 配置

- **文件**：`claude-j-start/src/main/resources/application.yml`
- **修改内容**：
  ```yaml
  management:
    endpoints:
      web:
        base-path: /actuator
    endpoint:
      health:
        probes:
          enabled: true
        show-details: never
        group:
          liveness:
            include: livenessState
          readiness:
            include: readinessState,db
      info:
        enabled: true

  spring:
    lifecycle:
      timeout-per-shutdown-phase: 30s
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：编译通过，无 YAML 解析错误
- **commit**：`feat(start): actuator 基础配置启用 liveness/readiness`

### 1.2 application-dev.yml 端点扩展

- **文件**：`claude-j-start/src/main/resources/application-dev.yml`
- **修改内容**：
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,info,metrics,env,liveness,readiness
    endpoint:
      health:
        show-details: always
      metrics:
        enabled: true
      env:
        enabled: true
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：编译通过
- **commit**：`feat(start): dev 环境 actuator 端点扩展`

### 1.3 application-staging.yml 端点调整

- **文件**：`claude-j-start/src/main/resources/application-staging.yml`
- **修改内容**：
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,info,liveness,readiness
    endpoint:
      health:
        show-details: when-authorized
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：编译通过
- **commit**：`feat(start): staging 环境 actuator 端点调整`

### 1.4 application-prod.yml 最小化端点

- **文件**：`claude-j-start/src/main/resources/application-prod.yml`
- **修改内容**：
  ```yaml
  management:
    endpoints:
      web:
        exposure:
          include: health,liveness,readiness
    endpoint:
      health:
        show-details: never
  ```
- **验证命令**：`mvn compile -pl claude-j-start`
- **预期输出**：编译通过
- **commit**：`feat(start): prod 环境 actuator 最小化端点`

### 2.1 集成测试 ActuatorHealthIntegrationTest

- **文件**：`claude-j-start/src/test/java/com/claudej/actuator/ActuatorHealthIntegrationTest.java`
- **骨架**：
  ```java
  @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("dev")
  class ActuatorHealthIntegrationTest {

      @Autowired
      private TestRestTemplate restTemplate;

      @Test
      void should_return_200_when_actuator_health() {
          ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      }

      @Test
      void should_return_200_when_actuator_health_liveness() {
          ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/liveness", String.class);
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      }

      @Test
      void should_return_200_when_actuator_health_readiness() {
          ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);
          assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      }
  }
  ```
- **验证命令**：`mvn test -pl claude-j-start -Dtest=ActuatorHealthIntegrationTest`
- **预期输出**：`Tests run: 3, Failures: 0, Errors: 0`
- **commit**：`test(start): actuator 健康端点集成测试`

### 3.1 全量测试验证

- **验证命令**：`mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
- **预期输出**：
  - mvn test: Tests run: X, Failures: 0, Errors: 0
  - checkstyle: Exit 0
  - entropy-check: 12/12 checks passed
- **commit**：无需单独 commit（已在前面提交）

## 开发完成记录

<!-- dev 完成后填写 -->
- 全量 `mvn clean test`: 58/58 用例通过
- 架构合规检查: entropy-check 0 errors, 12 warnings
- checkstyle: 0 violations
- 通知 @qa 时间: 2026-04-24 07:21

## QA 验收记录

<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：x/x 用例通过
- 代码审查结果：
- 代码风格检查：
- 问题清单：详见 test-report.md
- **最终状态**：