---
task-id: "006-coupon-service"
from: dev
to: architect
status: pending-review
timestamp: "2026-04-14T12:00:00"
pre-flight:
  mvn-test: pending
  checkstyle: pending
  entropy-check: pending
artifacts:
  - requirement-design.md
  - task-plan.md
summary: "优惠券服务 Spec 完成：Coupon 聚合根 + 4 个值对象 + 5 个 REST API + t_coupon DDL，提交架构评审"
---

# 交接文档

> 每次 Agent 间交接时更新此文件。
> 状态流转：pending-review → approved / changes-requested

## 交接说明
本次提交优惠券服务（006-coupon-service）的 Spec 阶段产出物，请 @architect 评审。

**设计要点**：
1. Coupon 聚合根封装完整的优惠券生命周期（AVAILABLE -> USED / EXPIRED）
2. 支持两种折扣类型：固定金额（FIXED_AMOUNT）和百分比（PERCENTAGE）
3. DiscountValue 值对象根据折扣类型执行不同校验（金额 > 0，百分比 1-100）
4. 采用懒过期策略，查询时自动检测并转换过期优惠券状态
5. 避免跨聚合依赖，coupon 内金额字段使用 BigDecimal + currency 组合
6. 5 个 REST API：创建、使用、按ID查询、按用户查询全部、按用户查询可用

**需关注**：
- Money 值对象目前在 order 聚合下，长期建议提取到 common 包（不在本期范围）
- 优惠券与订单的实际集成（下单时扣减）属于后续 story

## 评审回复
{接收方填写：评审意见、问题清单、通过/待修改结论}

---

## 交接历史

### 2026-04-14 — @dev -> @architect
- 状态：pending-review
- 说明：优惠券服务 Spec 设计完成，提交架构评审。产出物包括 requirement-design.md 和 task-plan.md
