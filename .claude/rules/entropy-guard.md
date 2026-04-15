# 熵管理规则（Claude Rules）

## 适用范围
- 适用于所有代码交付前的质量守护流程（本地、CI、交接）。
- 目标：防止架构漂移，保证规则在 Hook、测试、全局扫描三层一致生效。

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

## MUST NOT（禁止）
- 禁止跳过 `entropy-check.sh` 直接交接或合并。
- 禁止忽略 FAIL 级检查结果继续开发。
- 禁止只修单层规则而不做三层同步（Hook / ArchUnit / entropy）。

## 执行检查（每次改动后）
1. 开发中优先运行 `./scripts/quick-check.sh` 做快速反馈。
2. 交接前必须完整运行三项检查并记录结果。
3. 若出现 WARN（测试缺失、文档失配、死代码等），在本轮尽量清理并在交接中说明。
