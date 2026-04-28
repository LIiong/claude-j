# 开发日志 — 026-cors-config

## 问题记录

<!--
每条问题必须四段齐全（Issue / Root Cause / Fix / Verification），不得只记"决策"。
理由：违反 VERIFICATION 铁律的举证精神——没有 Verification 行的条目等于未证实。
Build 阶段 handoff 前请 self-check 所有条目；若缺 Verification 行 → 不得提交 handoff。
-->

### 1. Spec 阶段初始化
- **Issue**：当前阶段仅执行设计，不允许编写 `src/main/java` 代码；需要先明确 CORS 应落在安全链而不是控制器或领域层。
- **Root Cause**：CORS 属于 HTTP 横切配置，若设计阶段未先固定边界，Build 阶段容易出现把跨域规则散落到 `@CrossOrigin` 或 `WebMvcConfigurer` 的偏移实现。
- **Fix**：在 `requirement-design.md` 中明确采用 `SecurityFilterChain + CorsConfigurationSource + @ConfigurationProperties` 方案，并标注 domain/application/infrastructure 零影响。
- **Verification**：`git diff -- /Users/macro.li/aiProject/claude-j/docs/exec-plan/active/026-cors-config` → `仅包含 Spec 文档产出，无业务代码改动`

## 待确认
- 生产环境是否需要支持通配子域白名单（如 `https://*.example.com`）。
- 是否需要暴露自定义响应头；当前按最小暴露设计，不额外配置 exposed headers。
- 运维文档最终落点由 Build 阶段结合评审结果确认。

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
<!-- 格式：变更内容 + 变更原因 -->
- 无与原设计不一致的变更。
