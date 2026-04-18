---
name: full-check
description: "运行三项完整验证：mvn test（含 ArchUnit）+ checkstyle + entropy-check，输出汇总报告。"
user-invocable: true
disable-model-invocation: true
allowed-tools: "Bash(mvn *) Bash(./scripts/*) Bash(./.claude/skills/full-check/scripts/*) Read"
---

# 三项完整验证

运行项目的三项核心质量检查，输出通过/失败汇总。
这是 @dev 交接 @qa 前和 @qa 独立验收时的必检项目。

**脚本位置**：本 skill 自带 `scripts/entropy-check.sh` 与 `scripts/quick-check.sh`。`scripts/` 下的同名文件为指向本目录的符号链接（向后兼容）。

## 执行步骤

### 1. 全部测试（含 ArchUnit 架构守护）
```bash
mvn clean test -B
```
**检查要点**：
- JUnit 5 单元测试全部通过
- ArchUnit 14 条架构规则全部通过（依赖方向、domain 纯净性、命名规范）
- 测试金字塔各层覆盖

### 2. 代码风格检查
```bash
mvn checkstyle:check -B
```
**检查要点**：
- Java 8 兼容性（禁止 var、records、text blocks）
- 命名规范
- import 规范（禁止 * import）

### 3. 熵检查（架构漂移检测）
```bash
./scripts/entropy-check.sh
# 或等价的规范路径：
# ./.claude/skills/full-check/scripts/entropy-check.sh
```
**检查要点**：
- 12 项检查：domain 纯净性、依赖方向、Java 8 兼容、DO 泄漏、代码整洁度、文档同步、测试覆盖、死代码、ADR 一致性、知识库一致性、过期任务、聚合列表同步

## 输出汇总

三项检查完成后，输出如下格式的汇总：

```
┌──────────────────────────────────────┐
│         三项验证结果汇总              │
├──────────────┬───────────────────────┤
│ mvn test     │ ✅ PASS / ❌ FAIL     │
│ checkstyle   │ ✅ PASS / ❌ FAIL     │
│ entropy-check│ ✅ PASS / ❌ FAIL     │
├──────────────┴───────────────────────┤
│ 总结：X/3 通过                       │
└──────────────────────────────────────┘
```

如有失败项，在汇总下方列出具体失败原因和修复建议。
