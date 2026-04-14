# 任务执行计划 — 005-user-management

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: User 聚合根 + 值对象 + 测试 | dev | 待办 | User, UserId, Username, Email, Phone, UserStatus, InviteCode |
| 2 | Domain: UserRepository 端口 + InviteCodeGenerator 端口 | dev | 待办 | |
| 3 | Application: Command + DTO + Assembler | dev | 待办 | CreateUserCommand, UserDTO, UserAssembler |
| 4 | Application: UserApplicationService + 测试 | dev | 待办 | 用户注册、查询、生成邀请链接 |
| 5 | Application: UserOrderQueryService + 测试 | dev | 待办 | 查询用户订单、分享订单链接 |
| 6 | Infrastructure: UserDO + Mapper + Converter | dev | 待办 | |
| 7 | Infrastructure: UserRepositoryImpl + InviteCodeGeneratorImpl + 测试 | dev | 待办 | |
| 8 | Adapter: UserController + Request/Response + 测试 | dev | 待办 | 用户管理API、邀请API |
| 9 | Adapter: UserOrderController + Request/Response + 测试 | dev | 待办 | 用户订单管理API |
| 10 | Start: schema.sql DDL | dev | 待办 | 添加 t_user 表 |
| 11 | 全量 mvn test | dev | 待办 | |
| 12 | QA: 测试用例设计 | qa | 待办 | |
| 13 | QA: 验收测试 + 代码审查 | qa | 待办 | |
| 14 | QA: 接口集成测试 | qa | 待办 | |

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
