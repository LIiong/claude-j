# 任务执行计划 — 006-coupon-service

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: Coupon 聚合根 + 值对象（CouponId, DiscountType, DiscountValue, CouponStatus） + 测试 | dev | 待办 | 聚合根封装所有业务不变量 |
| 2 | Domain: CouponRepository 端口接口 | dev | 待办 | |
| 3 | Domain: ErrorCode 新增优惠券错误码 | dev | 待办 | |
| 4 | Application: CreateCouponCommand + UseCouponCommand + CouponDTO + CouponAssembler | dev | 待办 | |
| 5 | Application: CouponApplicationService + 测试 | dev | 待办 | |
| 6 | Infrastructure: CouponDO + CouponMapper + CouponConverter | dev | 待办 | |
| 7 | Infrastructure: CouponRepositoryImpl + 测试 | dev | 待办 | |
| 8 | Adapter: CouponController + CreateCouponRequest + UseCouponRequest + CouponResponse + 测试 | dev | 待办 | |
| 9 | Start: schema.sql 新增 t_coupon DDL | dev | 待办 | |
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
