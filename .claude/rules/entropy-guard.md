---
description: "三层守护与熵管理：交付前必须通过 mvn test、checkstyle、entropy-check。操作 Java 源码与交付脚本时生效。"
globs:
  - "**/*.java"
  - "scripts/**/*.sh"
  - "scripts/hooks/**"
alwaysApply: false
---

# 熵管理规则

## 适用范围
- **生效时机**：编辑 Java 源码或交付/守护脚本时自动注入。
- **目标**：防止架构漂移，保证规则在 Hook、测试、全局扫描三层一致生效。

## 纵深防御体系
| 层级 | 工具 | 触发时机 | 覆盖范围 |
|------|------|---------|---------|
| **L1** | `guard-java-layer.sh` Hook | 每次 Edit/Write | 分层依赖 / Domain 纯净 / DO 泄漏 / Java 8 |
| **L2** | ArchUnit（14 条） | `mvn test` | 架构规则 + 测试命名 |
| **L3** | `entropy-check.sh`（12 项） | 交付前 / CI | 全局一致性扫描 |

三层覆盖重叠关键项，任一失效时其他层仍能兜底。

## MUST（强制）

### 三层守护必须保持开启
- L1：`guard-java-layer.sh`（编辑阶段快速阻断）。
- L2：ArchUnit（`mvn test` 阶段验证架构规则）。
- L3：`./scripts/entropy-check.sh`（全局一致性扫描）。

### 交付前必须通过三项检查
1. `mvn test`
2. `mvn checkstyle:check`
3. `./scripts/entropy-check.sh`

### FAIL 级问题必须立即修复
- Domain 纯净性违规
- 依赖方向违规
- Java 8 兼容性违规
- DO 泄漏违规
- 测试命名违规（`should_xxx_when_yyy`）

## MUST NOT（禁止）
- 禁止跳过 `entropy-check.sh` 直接交接或合并。
- 禁止忽略 FAIL 级检查结果继续开发。
- 禁止仅修单层规则而不做三层同步（Hook / ArchUnit / entropy）。
- 禁止禁用或绕过守护 Hook（`guard-*.sh`）。

## 执行检查（每次改动后）
1. 开发中优先运行 `./scripts/quick-check.sh` 做快速反馈。
2. 交接前必须完整运行三项检查并记录结果。
3. 若出现 WARN（测试缺失、文档失配、死代码等），在本轮尽量清理并在交接中说明。
