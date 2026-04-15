# Agent 协作规则（Claude Rules）

## 适用范围
- 适用于 Ralph 主 Agent 与 `@dev` / `@architect` / `@qa` 的全流程协作。
- 目标：通过明确边界、交接协议和状态机，确保多 Agent 协作稳定可追踪。

## MUST（强制）

### 编排职责
- Ralph 主 Agent 只做调度与决策，不直接承担业务编码与测试实现。
- 每个阶段必须通过子 Agent 执行，确保上下文隔离。
- 阶段推进前必须验证前一阶段产物与 `handoff.md` 状态。

### 写作域边界
- `@dev`：业务代码、对应单测、设计与开发文档、schema 与 ADR。
- `@qa`：测试代码、测试设计、测试报告、交接文件。
- `@architect`：架构评审内容、交接文件、ADR。
- 写作域强制依赖 `guard-agent-scope.sh`，角色越权必须阻断。

### 交接与状态机
- 每次阶段交接必须更新 `{task-dir}/handoff.md`。
- `handoff.md` 必须包含：`task-id`、`from`、`to`、`status`、`pre-flight`、`summary`。
- 开发进入验收前必须通过三项预飞：`mvn test`、`mvn checkstyle:check`、`./scripts/entropy-check.sh`。
- `@qa` 验收时必须独立重跑三项检查，不直接信任上游标记。

## MUST NOT（禁止）
- 禁止子 Agent 修改其职责外文件（例如 `@qa` 修改 `src/main/java/`）。
- 禁止在 `pending-review + to:architect` 状态下继续编码。
- 禁止跳过 `handoff.md` 直接推进到下一阶段。
- 禁止无限返工循环，超过 3 轮必须升级为人工介入。

## 执行检查（每次阶段切换时）
1. 检查写作域是否越权（由 Hook + 人工双重确认）。
2. 检查 `handoff.md` 状态是否合法并与当前阶段匹配。
3. 检查三项预飞是否通过并可复现。
4. 若为 Ralph Loop，更新 `progress.md` 并保证本轮产物可追溯。
