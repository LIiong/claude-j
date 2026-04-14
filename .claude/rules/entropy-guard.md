# 熵管理规则

## 什么是熵检查
`./scripts/entropy-check.sh` 是项目的架构漂移检测工具，包含 12 项检查，覆盖代码纯净性、依赖方向、兼容性、文档一致性等。

## 纵深防御体系（三层守护分工）

部分规则由多套机制重复检查，这是**有意的纵深防御设计**，不是冗余：

| 层 | 机制 | 时机 | 作用 |
|---|------|------|------|
| **L1 实时门禁** | `guard-java-layer.sh` Hook | 每次 Edit/Write 前 | 最快反馈，阻止违规代码写入 |
| **L2 编译守护** | ArchUnit 14 条规则 | `mvn test` 时 | 捕获 Hook 遗漏的边缘情况（如已有代码的违规） |
| **L3 全局体检** | `entropy-check.sh` 12 项 | 手动/CI/交接前 | 覆盖 Hook 和 ArchUnit 不检查的 8 项额外规则 |

**重叠覆盖矩阵**：

| 规则 | L1 Hook | L2 ArchUnit | L3 entropy-check |
|------|:-------:|:-----------:|:----------------:|
| Domain 纯净性 | ✅ | ✅ | ✅ |
| 依赖方向 | ✅ | ✅ | ✅ |
| DO 泄漏 | ✅ | ✅ | ✅ |
| Java 8 兼容 | ✅ | ❌ | ✅ |
| 代码整洁度 | ❌ | ❌ | ✅ |
| 文档同步 | ❌ | ❌ | ✅ |
| 测试覆盖 | ❌ | ❌ | ✅ |
| 死代码 | ❌ | ❌ | ✅ |
| ADR 一致性 | ❌ | ❌ | ✅ |
| 知识库一致性 | ❌ | ❌ | ✅ |
| 过期任务 | ❌ | ❌ | ✅ |
| 聚合列表同步 | ❌ | ❌ | ✅ |

**原则**：L1 拦截 > L2 编译时兜底 > L3 全局审计。维护规则时三层同步更新。

## 何时触发
- **开发中**：`./scripts/quick-check.sh`（编译 + 单测 + 风格，~30s）
- **提交前**：Git pre-commit hook 自动运行编译 + Checkstyle
- **推送前**：Git pre-push hook 自动运行全量测试 + Checkstyle + entropy-check
- **交接前**：@dev 标记"待验收"前必须手动运行并通过
- **每周一**：GitHub Actions 定期运行，失败自动创建 Issue

## 12 项检查说明

| # | 检查项 | 类型 | 说明 |
|---|--------|------|------|
| 1 | Domain 层纯净性 | FAIL | domain 模块禁止 Spring/MyBatis-Plus import |
| 2 | 依赖方向 | FAIL | 检测跨层 import 违规 |
| 3 | Java 8 兼容性 | FAIL | 禁止 var、List.of()、Map.of() |
| 4 | DO 泄漏 | FAIL | DO 对象不得出现在 infrastructure 之外 |
| 5 | 代码整洁度 | WARN | 检测 * import |
| 6 | 文档同步 | INFO | schema.sql 表数量 + CLAUDE.md 存在性 |
| 7 | 测试覆盖 | WARN | Service/Repository/Controller 需有对应测试 |
| 8 | 死代码 | WARN | 无引用的类 |
| 9 | ADR 一致性 | WARN | ADR 文件需有状态/背景/决策三节 |
| 10 | 知识库一致性 | WARN | 文档中引用的文件路径需存在 |
| 11 | 过期活跃任务 | WARN | 已验收但未归档的任务 |
| 12 | 聚合列表同步 | WARN | domain 层聚合目录 vs CLAUDE.md 聚合表 |

## 检查失败修复指南

### FAIL 类（必须修复）
- **Domain 纯净性**：将 Spring 依赖移到 infrastructure 层，domain 中只定义接口
- **依赖方向**：检查 import 语句，遵循 adapter→application→domain←infrastructure
- **Java 8 兼容**：`var` → 显式类型；`List.of()` → `Arrays.asList()`；`Map.of()` → 手动构建
- **DO 泄漏**：在 infrastructure 层用 Converter 转为 Domain 对象后再传出

### WARN 类（建议修复）
- **\* import**：IDE 设置展开通配符 import
- **缺失测试**：按 java-test.md 规范补充对应测试
- **死代码**：确认无用则删除，有用则检查为何未被引用
- **ADR 格式**：按 `docs/architecture/decisions/000-template.md` 模板补充缺失章节
- **文件引用**：更新文档中的路径或删除过时引用
- **过期任务**：将 active/ 中已验收任务移到 archived/
- **聚合列表**：更新 CLAUDE.md 中的聚合列表表格
