# 任务执行计划 — 002-order-service

## 任务状态跟踪

| # | 任务 | 层 | 负责人 | 状态 | 备注 |
|---|------|---|--------|------|------|
| 1 | 值对象：OrderId, CustomerId, OrderStatus, Money | domain | dev | 待办 | |
| 2 | 实体：OrderItem | domain | dev | 待办 | |
| 3 | 聚合根：Order（含工厂方法、状态机、金额计算） | domain | dev | 待办 | |
| 4 | Repository 端口：OrderRepository 接口 | domain | dev | 待办 | |
| 5 | 域层测试：值对象 + OrderItem + Order 单元测试 | domain | dev | 待办 | |
| 6 | Command + DTO：CreateOrderCommand, OrderDTO, OrderItemDTO | application | dev | 待办 | |
| 7 | Assembler：OrderAssembler（MapStruct） | application | dev | 待办 | |
| 8 | ApplicationService：OrderApplicationService + 测试 | application | dev | 待办 | |
| 9 | DO：OrderDO, OrderItemDO | infrastructure | dev | 待办 | |
| 10 | Mapper：OrderMapper, OrderItemMapper | infrastructure | dev | 待办 | |
| 11 | Converter：OrderConverter（DO ↔ Domain） | infrastructure | dev | 待办 | |
| 12 | RepositoryImpl：OrderRepositoryImpl + 测试 | infrastructure | dev | 待办 | |
| 13 | Request/Response：CreateOrderRequest, OrderResponse 等 | adapter | dev | 待办 | |
| 14 | Controller：OrderController + 测试 | adapter | dev | 待办 | |
| 15 | 全量 mvn test + checkstyle + entropy-check | dev | dev | 待办 | |
| 16 | QA：测试用例设计 | qa | qa | 待办 | |
| 17 | QA：验收测试 + 代码审查 | qa | qa | 待办 | |
| 18 | QA：集成测试 | qa | qa | 待办 | |

## 执行顺序
domain(1-5) → application(6-8) → infrastructure(9-12) → adapter(13-14) → 验证(15) → QA(16-18)

## 开发完成记录
- 全量 `mvn clean test`：
- 架构合规检查：
- 通知 @qa 时间：

## QA 验收记录
- 全量测试（含集成测试）：
- 代码审查结果：
- 代码风格检查：
- 问题清单：详见 test-report.md
- **最终状态**：
