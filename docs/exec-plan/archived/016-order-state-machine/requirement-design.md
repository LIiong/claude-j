# 需求拆分设计 — 016-order-state-machine

## 需求描述
补齐 Order 状态机的发货（ship）、送达（deliver）、退款（refund）完整链路。
当前 OrderStatus 枚举有 CREATED/PAID/SHIPPED/DELIVERED/CANCELLED，缺少 REFUNDED；Order 聚合根已有 ship()/deliver() 方法但缺 refund()；Application 和 Adapter 层缺少对应用例方法和 REST 端点。退款时需回滚已使用的优惠券。

## 领域分析

### 聚合根: Order（变更）
- status (OrderStatus) — 新增 REFUNDED 枚举值，新增 canRefund()/toRefunded() 转换规则
- refund() 方法 — 退款：调用 status.toRefunded()，更新 updateTime
- isRefunded() 便捷方法 — 判断是否已退款

### 值对象
- **OrderStatus**: 新增 REFUNDED("已退款") 枚举值，新增 canRefund() / toRefunded() 方法
  - canRefund() 规则：PAID / SHIPPED / DELIVERED 可退款
  - toRefunded() 规则：仅 canRefund() 为 true 时允许转换，否则抛 BusinessException(ErrorCode.INVALID_ORDER_STATUS_TRANSITION)

### 领域服务
- 无需新增领域服务。退款时的优惠券回滚由 Application 层编排（与 cancelOrder 模式一致）。

### 端口接口
- **OrderRepository**: 无变更（现有 save/findByOrderId 已满足）
- **CouponRepository**: 无变更（现有 findByCouponId/save 已满足）

## 关键算法/技术方案

### 状态机合法转换（完整视图）
```
CREATED ──pay──→ PAID
CREATED ──cancel──→ CANCELLED
PAID ──ship──→ SHIPPED
PAID ──cancel──→ CANCELLED
PAID ──refund──→ REFUNDED       [新增]
SHIPPED ──deliver──→ DELIVERED
SHIPPED ──refund──→ REFUNDED    [新增]
DELIVERED ──refund──→ REFUNDED  [新增]
```

### 退款优惠券回滚
退款流程与 cancelOrder 的优惠券回滚逻辑模式一致：
1. 查找订单
2. 如果订单使用了优惠券且优惠券已核销（USED 状态），调用 coupon.unuse() 回滚
3. 订单调用 removeCoupon() 移除关联
4. 订单调用 refund() 变更状态
5. 保存订单和优惠券

### 假设与待确认
- **假设 1**：REFUNDED 状态的订单不允许再做任何状态转换（终态），与 CANCELLED 类似
- **假设 2**：退款不需要额外入参（如退款原因、退款金额），仅做状态变更 + 优惠券回滚。如需退款金额部分退，属于未来需求
- **假设 3**：refund 接口为 POST /{orderId}/refund，无请求体，与现有 pay/cancel 端点风格一致

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/orders/{orderId}/ship | 发货 | — | `{ "success": true, "data": { "orderId": "...", "status": "SHIPPED", ... } }` |
| POST | /api/v1/orders/{orderId}/deliver | 送达 | — | `{ "success": true, "data": { "orderId": "...", "status": "DELIVERED", ... } }` |
| POST | /api/v1/orders/{orderId}/refund | 退款 | — | `{ "success": true, "data": { "orderId": "...", "status": "REFUNDED", ... } }` |

错误场景：
- 订单不存在 → 404 ORDER_NOT_FOUND
- 状态不允许转换 → 400 INVALID_ORDER_STATUS_TRANSITION
- 优惠券回滚失败（优惠券不存在）→ 404 COUPON_NOT_FOUND

## 数据库设计
无 DDL 变更。t_order.status 列为 VARCHAR(32)，已支持存储 "REFUNDED" 字符串。

## 影响范围
- **domain**:
  - `OrderStatus.java` — 新增 REFUNDED 枚举值 + canRefund() + toRefunded()
  - `Order.java` — 新增 refund() + isRefunded() 方法
  - `OrderStatusTest.java` — 新增退款转换测试
  - `OrderTest.java` — 新增退款场景测试
- **application**:
  - `OrderApplicationService.java` — 新增 shipOrder() / deliverOrder() / refundOrder() 方法
  - `OrderApplicationServiceTest.java` — 新增对应测试
- **infrastructure**: 无变更（OrderConverter 已通过 OrderStatus.valueOf() 支持新枚举值）
- **adapter**:
  - `OrderController.java` — 新增 ship / deliver / refund 端点
  - `OrderControllerTest.java` — 新增对应测试
- **start**: 无 DDL 变更

## 架构评审

**评审人**：@architect
**日期**：2026-04-22
**结论**：通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）
  - Order 聚合内聚：refund() 封装在 Order 聚合根，状态转换规则在 OrderStatus 值对象。退款优惠券回滚涉及 Coupon 聚合的跨聚合操作，由 Application 层编排（与 cancelOrder 模式一致），符合"跨聚合协作由 Application 编排"的架构约定。
- [x] 值对象识别充分（金额、标识符等应为 VO）
  - REFUNDED 作为 OrderStatus 枚举值，遵循既有模式。OrderId/CouponId/Money 等值对象已就位，无遗漏。
- [x] Repository 端口粒度合适（方法不多不少）
  - OrderRepository 和 CouponRepository 无变更，现有 save/findByOrderId/findByCouponId 已满足需求。
- [x] 与已有聚合无循环依赖
  - Order 聚合依赖 Coupon 聚合的 CouponId 值对象（已存在 import），无新增依赖方向。Application 层跨聚合编排由 OrderApplicationService 完成，依赖方向正确。
- [x] DDL 设计与领域模型一致（字段映射、索引合理）
  - 无 DDL 变更。t_order.status 为 VARCHAR(32)，可存储 "REFUNDED"。OrderConverter 通过 OrderStatus.valueOf() 自动支持新枚举值。
- [x] API 设计符合 RESTful 规范
  - POST /{orderId}/ship、/{orderId}/deliver、/{orderId}/refund 与现有 pay/cancel 端点风格一致，动词 + 资源路径，无请求体，符合既有 API 惯例。
- [x] 对象转换链正确（DO <-> Domain <-> DTO <-> Request/Response）
  - DO -> Domain：OrderConverter.toDomain() 使用 OrderStatus.valueOf()，自动支持 REFUNDED。
  - Domain -> DO：OrderConverter.toDO() 使用 order.getStatus().name()，自动输出 "REFUNDED"。
  - Domain -> DTO：OrderAssembler 已映射 status 字段（String 类型）。
  - DTO -> Response：OrderController.convertToResponse() 已映射 status 字段。
  - 全链路无需额外变更。

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义
  - REFUNDED 终态、canRefund() 规则（PAID/SHIPPED/DELIVERED）、优惠券回滚步骤均明确。三个假设显式声明。
- [x] 验收条件可验证：每条 AC 可转化为 should_xxx_when_yyy 测试用例
  - 状态转换（3 条正向 + 3 条逆向）、优惠券回滚（有/无优惠券 2 条）、shipOrder/deliverOrder（各 2 条）均可转化为测试用例。task-plan 已提供骨架。
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明
  - 完整状态机图已给出，终态定义明确，退款不额外入参的边界已声明。

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10-15 分钟/步），每步含文件路径 + 验证命令 + 预期输出
  - 4 个原子任务（1.1 OrderStatus + 1.2 Order + 3.1 ApplicationService + 4.1 Controller），每步含文件路径、测试骨架代码、mvn test 验证命令、预期输出、commit 消息。符合模板要求。
- [x] 依赖顺序正确：domain -> application -> infrastructure -> adapter -> start 自下而上，层间依赖无倒置
  - 执行顺序为 domain -> application -> adapter -> 全量测试，正确。infrastructure 无变更。

**可测性保障（3 项 -- 010 复盘后新增）**
- [x] AC 自动化全覆盖：每条 AC 都有对应自动化测试方法
  - task-plan 中 Domain 层 8+ 测试用例、Application 层 8+ 测试用例、Adapter 层 7+ 测试用例，覆盖全部 AC。
- [x] 可测的注入方式：若引入新 Spring Bean，使用构造函数注入而非字段注入
  - OrderApplicationService 和 OrderController 已使用构造函数注入，新增方法无需改变注入方式。
- [x] 配置校验方式合规：若涉及敏感/跨环境配置校验，使用 @ConfigurationProperties + @Validated
  - 本任务不涉及配置校验，N/A。

**心智原则（Karpathy -- 动手前自检）**
- [x] 简洁性：需求未要求的抽象/配置/工厂已移除；任何单一实现的 XxxStrategy/XxxFactory 需说明存在理由
  - 无新增抽象/工厂/策略类。refundOrder 逻辑直接在 ApplicationService 中编排，与 cancelOrder 一致。shipOrder/deliverOrder 仅 3 行（find -> ship/deliver -> save），极简。
- [x] 外科性：设计仅改动任务直接相关的文件；若涉及跨聚合大改，在评审意见说明理由
  - 影响范围限定在 Order 聚合内（OrderStatus/Order/OrderApplicationService/OrderController + 对应测试）。Coupon 聚合仅被 Application 层读取调用，无修改。
- [x] 假设显性：需求里含糊的字段/边界/异常，requirement-design 已在「假设与待确认」列出
  - 三个假设已显式声明：REFUNDED 终态、退款无额外入参、API 风格一致性。

### 评审意见

**通过**。设计质量高，状态机完整，优惠券回滚逻辑与 cancelOrder 模式一致，影响范围精准，无 Infrastructure 层变更。

**观察项（非阻塞，不要求修改）**：

1. **isPaid() 语义**：当前 `Order.isPaid()` 返回 `PAID || SHIPPED || DELIVERED`，不包含 REFUNDED。从业务语义看，REFUNDED 订单"曾经支付过"，但当前 `isPaid()` 的实际用途仅限于 cancelOrder 判断优惠券是否需要 unuse。由于 canCancel() 不允许 REFUNDED -> CANCELLED，`isPaid()` 是否包含 REFUNDED 对现有逻辑无影响。建议保持现状，待有明确需求时再调整。

2. **OrderResponse 缺少优惠券字段**：OrderResponse 和 OrderController.convertToResponse() 缺少 discountAmount/finalAmount/couponId 字段（OrderDTO 有这些字段）。这是 015-order-coupon-integration 遗留的预存问题，不影响本任务（REFUNDED 状态通过 status 字段正确返回），但后续若需在 API 响应中展示优惠券回滚结果，需补齐。

3. **cancelOrder 优惠券回滚的防御性**：cancelOrder 先判断 `isPaid() && hasCoupon()` 做 unuse，再判断 `hasCoupon()` 做 removeCoupon。对于 refundOrder，由于只有 PAID/SHIPPED/DELIVERED 可退款（都是 isPaid()==true），逻辑可简化为 `if (hasCoupon()) { unuse + removeCoupon }`。建议 @dev 实现时采用简化写法，避免照搬 cancelOrder 的冗余双 if 结构。

### 架构基线检查

```
./scripts/entropy-check.sh
退出码：0（PASS）
结果：0 FAIL, 12 WARN
- FAIL：0
- WARN：6 个 auth 聚合测试缺失（预存）、5 个 ADR 缺少状态节（预存）、1 个归档目录 post-archive commit（预存）
```

所有 WARN 均为预存问题，非本任务引入。

### 需要新增的 ADR

无。本任务是既有状态机的扩展（新增 REFUNDED 枚举值 + 3 条转换边），属于 Order 聚合内部变更，无架构决策需要记录。
