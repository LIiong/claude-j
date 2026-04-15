# 任务执行计划 — 007-shopping-cart

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: Cart 聚合根 + CartItem 实体 + Quantity/Money 值对象 + 测试 | dev | 待办 | 聚合根封装所有业务不变量 |
| 2 | Domain: CartRepository 端口 + ErrorCode 新增 | dev | 待办 | |
| 3 | Application: AddCartItemCommand + UpdateCartItemQuantityCommand | dev | 待办 | |
| 4 | Application: CartDTO + CartItemDTO + CartAssembler | dev | 待办 | MapStruct 接口 |
| 5 | Application: CartApplicationService + 测试 | dev | 待办 | Mock CartRepository |
| 6 | Infrastructure: CartDO + CartItemDO + CartMapper + CartItemMapper | dev | 待办 | MyBatis-Plus |
| 7 | Infrastructure: CartConverter + CartRepositoryImpl + 测试 | dev | 待办 | @SpringBootTest + H2 |
| 8 | Adapter: CartController + Request/Response 对象 + 测试 | dev | 待办 | @WebMvcTest |
| 9 | Start: schema.sql 新增 t_cart + t_cart_item DDL | dev | 待办 | |
| 10 | 全量 mvn test + checkstyle + entropy-check | dev | 待办 | |
| 11 | QA: 测试用例设计 | qa | 待办 | |
| 12 | QA: 验收测试 + 代码审查 | qa | 待办 | |
| 13 | QA: 接口集成测试 | qa | 待办 | |

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
