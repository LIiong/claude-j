---
description: "Java 测试规则：测试分层、命名规范、AAA 结构、断言策略。编辑 src/test/java 下的测试代码时生效。"
globs:
  - "**/src/test/java/**/*.java"
  - "**/*Test.java"
  - "**/*IT.java"
alwaysApply: false
---

# Java 测试规则

## 适用范围
- **生效时机**：编辑 `src/test/java/` 下的测试代码时自动注入。
- **目标**：保障分层测试策略一致、行为可验证、测试可维护。

## MUST（强制）

### 测试分层
| 层 | 框架 | Spring 上下文 | 关注点 |
|---|------|---------------|--------|
| Domain | JUnit 5 + AssertJ | 禁止 | 不变量、状态转换、值对象相等性 |
| Application | JUnit 5 + Mockito | 禁止 | 编排顺序、命令校验 |
| Infrastructure | `@SpringBootTest` + H2 | 必须 | 持久化与 DO↔Domain 往返 |
| Adapter | `@WebMvcTest` + MockMvc | 部分 | HTTP 契约、入参校验、响应结构 |

### 测试设计
- 测试文件必须与生产代码目录结构镜像对应。
- 测试方法命名必须采用 `should_{预期行为}_when_{条件}` 格式（由 ArchUnit 强制）。
- 测试结构必须使用 AAA（Arrange / Act / Assert）。
- 关键业务规则必须覆盖：状态转换、不变量、异常场景、边界值。

### 断言与验证
- 必须断言业务结果和副作用（如仓储保存是否发生）。
- Web 层必须断言响应结构与状态码（200/400/404/500 等）。
- 集成测试必须断言 DO ↔ Domain 映射准确性。

### 集成测试适量（防爆 start 模块）

> **背景**：010-secret-externalize 的 `JwtSecretIntegrationTest` 写了 6 个 `@SpringBootTest` 全链路用例，其中 3 个实际在覆盖 009-auth-system 的 Auth 功能（重复覆盖），违反 Karpathy 原则②（简洁优先）。

- 单任务新增的 `@SpringBootTest` 全链路测试 **≤ 3 个**，覆盖：
  1. **Happy path**（核心成功流程）
  2. **关键分支 1**（任务新增的关键异常/边界）
  3. **关键分支 2**（可选）
- 其余场景用更轻量的切片测试：
  - `@WebMvcTest` + MockMvc（HTTP 契约）
  - `@DataJpaTest` / `@MybatisPlusTest` + H2（持久化）
  - 纯 JUnit + Mockito（Service 层）
  - `ApplicationContextRunner`（配置绑定 / 启动失败）
- **禁止**在新任务的集成测试里重复覆盖已有聚合的功能（例如 010 的集成测试不应包含"注册→登录→刷新"全流程验证——这是 009 的责任）。
- **反模式**：用集成测试做"我顺手验证一下 Auth/Order/Cart 也没坏"。回归职责归 `mvn test` 全量与 ArchUnit，不归单个任务的集成测试。

## MUST NOT（禁止）
- 禁止跨层测试依赖（例如 Domain 测试依赖 Infrastructure）。
- 禁止在 Domain/Application 单测中使用 Spring 上下文。
- 禁止在单测中连接真实数据库（除 Infrastructure 的 H2 场景）。
- 禁止 `Thread.sleep()`、`@Order`、共享可变测试状态。
- 禁止直接测试私有方法（通过公开行为验证）。
- 禁止用反射访问被测类的 `private` 字段注入值（根因通常是生产代码用了字段注入 `@Value`，改为构造函数注入后测试无需反射，详见 `java-dev.md` 依赖注入规则）。
- 禁止单任务新增超过 3 个 `@SpringBootTest` 全链路集成测试。
- 禁止在新任务的集成测试中重复覆盖其他已完成聚合的功能。

## TDD 铁律

```
NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
```

任何 `src/main/java/` 下的业务代码必须先有一个**看到过红**的测试。
先写实现再补测试 = 删除重来，不讨论、不妥协。

### Red-Green-Refactor 循环
1. **Red** — 先写失败测试，运行 `mvn test` 确认**真的失败**（且失败原因符合预期，不是编译错误/找不到类）
2. **Green** — 写最小实现让测试通过，再跑 `mvn test` 确认转绿
3. **Refactor** — 在所有测试保持绿的前提下清理代码

没有 Red → 没有 Green。没有看到失败 → 没有证据证明测试有效。

## TDD 反模式对照表

| 借口 | 真相 | 在 claude-j 中的本土化 |
|------|------|----------------------|
| "我先写 Controller 再补测试" | = 删除重来 | Adapter 层代码删除，先写 `@WebMvcTest` |
| "我手工 curl 测过了" | 手工验证 ≠ 回归测试 | 必须有可重复的 `should_xxx_when_yyy` 测试 |
| "这段代码太简单不用测" | 简单代码也有不变量 | 值对象、ErrorCode 都必须有测试 |
| "TDD 太教条了" | 教条是对过去失败的总结 | ArchUnit 强制 + 规则明确，不教条 |
| "先提交一版占位后面补测试" | 未测试代码进主干 = 债务 | post-commit-check Hook 会阻断无测试的 Java commit |
| "改动太小不用红绿" | 任何改动都可能破坏不变量 | 即使修一个字段默认值也要先写 Red |
| "我复制了类似聚合的测试结构" | 复制 ≠ 验证新逻辑 | 新聚合的不变量必须有针对性测试 |
| "作为参考保留，不运行" | 未运行的测试 = 0 价值 | `@Disabled` / 注释掉的测试不算数 |
| "Mock 太多写不出来" | Mock 多 = 设计有问题 | 重新思考 Application 层边界 |
| "集成测试覆盖了，单测省略" | 集成测试发现问题代价高 | Domain/Application 必须独立单测 |
| "重构不需要新测试" | 重构需要现有测试保护 | 先确认现有测试能捕获回归再重构 |

**违反任一条 → 当前改动视为未完成，回 Red 重来。**

## 执行检查（每次改动后）
1. 新增功能时同步补齐对应层测试。
2. 运行 `mvn test`，确保所有测试与 ArchUnit 14 条规则通过。
3. 若是 API 改动，重点检查 `adapter` 测试是否覆盖请求校验与错误响应。
4. 对照 "TDD 反模式对照表" 自检：是否在任何一条上鬆懈。
