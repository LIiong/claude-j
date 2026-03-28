# 执行计划模板

每次新任务在 `docs/exec-plan/active/{task-id}-{task-name}/` 下创建以下文件：

| 文件 | 负责人 | 阶段 | 说明 |
|------|--------|------|------|
| `requirement-design.md` | @dev | 需求分析 | 需求拆分、领域分析、API 设计、影响范围 |
| `task-plan.md` | @dev | 任务拆解 | 任务列表、状态跟踪、执行顺序 |
| `dev-log.md` | @dev | 开发中 | 问题记录、决策原因、变更记录 |
| `test-case-design.md` | @qa | 测试设计 | 分层测试用例、集成测试用例、审查检查项 |
| `test-report.md` | @qa | 验收 | 测试结果、代码审查、风格检查、验收结论 |

## 使用方式

1. @dev 接到需求 → 复制模板到 `active/{task-id}-{task-name}/` → 去掉 `.template` 后缀
2. @dev 按模板填写 requirement-design.md 和 task-plan.md
3. @dev 开发过程中持续更新 dev-log.md 和 task-plan.md 状态
4. @dev 单测通过 → 标记"待验收" → 通知 @qa
5. @qa 按模板填写 test-case-design.md → 执行测试 → 填写 test-report.md
6. 验收通过 → 整个目录移动到 `archived/{task-id}-{task-name}/`

## 状态流转
```
待办 → 进行中 → 单测通过 → 待验收 → 验收通过
                                    ↘ 待修复 → 进行中（回归）
```

## task-id 编号规则
三位数递增：001, 002, 003...
