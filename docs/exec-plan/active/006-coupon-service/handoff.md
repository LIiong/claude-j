---
task-id: "006-coupon-service"
from: dev
to: architect
status: approved
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

**评审人**：@architect
**日期**：2026-04-14
**结论**：✅ 通过

设计整体符合 DDD 六边形架构规范，聚合边界清晰，值对象设计合理，API 和 DDL 均与已有模式一致。

**已修正 1 项问题**：
- 原设计 minOrderAmount 类型为 Money（order 聚合），calculateDiscount 参数也用 Money，形成跨聚合耦合。已修正为 BigDecimal + currency 组合。

**非阻塞建议 2 项**：
1. DDL 去掉 ENGINE=InnoDB 子句，与 t_order 等表保持一致
2. 懒过期查询路径的副作用需在代码中明确注释

详细评审意见见 requirement-design.md「架构评审」章节。

@dev 可执行 `/dev-build 006-coupon-service` 开始编码。

---

## 交接历史

### 2026-04-14 — @architect -> @dev
- 状态：approved
- 说明：架构评审通过。修正了 1 项跨聚合 Money 耦合问题（改用 BigDecimal + currency）。2 项非阻塞建议供 @dev 实现时参考。可开始编码。

### 2026-04-14 — @dev -> @architect
- 状态：pending-review
- 说明：优惠券服务 Spec 设计完成，提交架构评审。产出物包括 requirement-design.md 和 task-plan.md
