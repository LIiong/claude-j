# 任务执行计划 — {task-id}-{task-name}

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: {聚合根} + 值对象 + 测试 | dev | 待办 | |
| 2 | Domain: Repository 端口 | dev | 待办 | |
| 3 | Domain: 领域服务端口（如有） | dev | 待办 | |
| 4 | Application: Command + DTO + Assembler | dev | 待办 | |
| 5 | Application: ApplicationService + 测试 | dev | 待办 | |
| 6 | Infrastructure: DO + Mapper + Converter | dev | 待办 | |
| 7 | Infrastructure: RepositoryImpl + 测试 | dev | 待办 | |
| 8 | Infrastructure: 领域服务实现 + 测试（如有） | dev | 待办 | |
| 9 | Adapter: Controller + Request/Response + 测试 | dev | 待办 | |
| 10 | Start: schema.sql DDL | dev | 待办 | |
| 11 | 全量 mvn test | dev | 待办 | |
| 12 | QA: 测试用例设计 | qa | 待办 | |
| 13 | QA: 验收测试 + 代码审查 | qa | 待办 | |
| 14 | QA: 接口集成测试 | qa | 待办 | |

<!-- 根据实际需求增减任务行，保持编号连续 -->

## 执行顺序
domain → application → infrastructure → adapter → start → 全量测试 → QA 验收 → 集成测试

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：x/x 用例通过
- 架构合规检查：
- 通知 @qa 时间：

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：x/x 用例通过
- 代码审查结果：
- 代码风格检查：
- 问题清单：详见 test-report.md
- **最终状态**：
