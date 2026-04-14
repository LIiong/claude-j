# 执行计划模板

每次新任务在 `docs/exec-plan/active/{task-id}-{task-name}/` 下创建以下文件：

| 文件 | 负责人 | 阶段 | 说明 |
|------|--------|------|------|
| `requirement-design.md` | @dev | 需求分析 | 需求拆分、领域分析、API 设计、影响范围 |
| `task-plan.md` | @dev | 任务拆解 | 任务列表、状态跟踪、执行顺序 |
| `dev-log.md` | @dev | 开发中 | 问题记录、决策原因、变更记录 |
| `test-case-design.md` | @qa | 测试设计 | 分层测试用例、集成测试用例、审查检查项 |
| `test-report.md` | @qa | 验收 | 测试结果、代码审查、风格检查、验收结论 |
| `handoff.md` | @dev/@qa/@architect | 交接 | Agent 间结构化交接（状态、预飞检查、评审） |
| `progress.md` | @dev/@qa | Ralph Loop | 任务进度跟踪（Ralph Loop 核心状态文件） |

## 使用方式

1. @dev 接到需求 → 复制模板到 `active/{task-id}-{task-name}/` → 去掉 `.template` 后缀
2. @dev 按模板填写 requirement-design.md 和 task-plan.md
3. @dev 创建 handoff.md（to: architect）→ @architect 设计评审
4. @architect 评审通过 → @dev 开始编码
5. @dev 开发过程中持续更新 dev-log.md 和 task-plan.md 状态
6. @dev 三项检查通过 → 更新 handoff.md（to: qa）→ 通知 @qa
7. @qa 独立重跑检查 → 按模板填写 test-case-design.md → 执行测试 → 填写 test-report.md
8. 验收通过 → 整个目录移动到 `archived/{task-id}-{task-name}/`

### Ralph Loop 模式（大型任务推荐）
```bash
./scripts/ralph-init.sh docs/exec-plan/active/{task-id}/   # 初始化 progress.md
./scripts/ralph-loop.sh dev docs/exec-plan/active/{task-id}/  # Agent 循环运行
```

## 状态流转
```
待办 → 进行中 → 单测通过 → 待验收 → 验收通过
                                    ↘ 待修复 → 进行中（回归）
```

## task-id 编号规则
三位数递增：001, 002, 003...
