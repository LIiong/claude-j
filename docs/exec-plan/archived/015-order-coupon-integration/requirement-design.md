# 需求拆分设计 — 015-order-coupon-integration

## 需求描述

补齐 Order 与 Coupon 聚合的集成能力，实现订单生命周期中的优惠券使用、核销与回滚机制。

核心目标：
1. 订单支持关联优惠券，记录折扣金额和最终应付金额
2. 下单时验证优惠券归属/有效期/最低消费，计算折扣金额
3. 支付订单时核销优惠券（AVAILABLE → USED）
4. 取消订单时回滚优惠券状态（USED → AVAILABLE）
5. 保持订单金额一致性：totalAmount = 原始金额，finalAmount = 折扣后金额

## 领域分析

### 聚合根: Order（修改）

**新增字段：**
- `couponId: CouponId`（值对象）— 关联的优惠券ID，可为null表示未使用优惠券
- `discountAmount: Money`（值对象）— 折扣金额，无券时为0
- `finalAmount: Money`（值对象）— 最终应付金额 = totalAmount - discountAmount

**不变量约束：**
- finalAmount = totalAmount - discountAmount（必须保持计算一致性）
- discountAmount >= 0 且 discountAmount <= totalAmount
- couponId 不为空时，discountAmount 必须 > 0
- finalAmount >= 0

**新增方法：**
- `applyCoupon(CouponId couponId, Money discountAmount)` — 应用优惠券（创建订单时调用）
- `removeCoupon()` — 移除优惠券（取消订单时调用）
- `getFinalAmount()` — 获取最终金额（支付时使用）

### 聚合根: Coupon（扩展方法）

**新增方法：**
- `unuse()` — 回滚优惠券状态（USED → AVAILABLE），用于订单取消场景
- 清除 usedTime 和 usedOrderId

### 值对象

**CouponId**: 优惠券标识符，不可变，包装 String 值

**Money**: 金额值对象，已存在，包含 amount 和 currency，支持加减运算

### 领域服务（无）

折扣计算逻辑由 Coupon.calculateDiscount() 提供，无需额外领域服务。

### 端口接口

**CouponRepository**（已存在，需新增查询方法）：
- `Optional<Coupon> findByCouponId(CouponId couponId)` — 根据优惠券ID查询（已存在）
- `Coupon save(Coupon coupon)` — 保存优惠券（已存在）

**OrderRepository**（已存在）：
- 无需新增方法，现有 save/find 已满足需求

## 关键算法/技术方案

### 1. 优惠券验证策略（Application 层）

在 OrderApplicationService 中创建订单时执行以下验证：

```
if (couponId != null) {
    // 1. 查询优惠券
    Coupon coupon = couponRepository.findByCouponId(couponId)
        .orElseThrow(COUPON_NOT_FOUND);

    // 2. 验证归属（优惠券属于当前用户）
    if (!coupon.getUserId().equals(customerId)) {
        throw BusinessException(COUPON_NOT_BELONG_TO_USER);
    }

    // 3. 检查有效期（懒过期策略）
    coupon.checkAndExpire(now);
    if (coupon.getStatus() != AVAILABLE) {
        throw BusinessException(COUPON_NOT_AVAILABLE);
    }

    // 4. 验证最低消费
    if (totalAmount < coupon.getMinOrderAmount()) {
        throw BusinessException(COUPON_MIN_ORDER_AMOUNT_NOT_MET);
    }

    // 5. 计算折扣金额
    discountAmount = coupon.calculateDiscount(totalAmount);
}
```

**设计决策：**
- 验证放在 Application 层而非 Domain 层：因为验证需要访问 CouponRepository，涉及跨聚合查询
- 遵循规则：聚合间不直接依赖，通过 Repository 协调

### 2. 优惠券核销策略

```
// 在 payOrder 方法中
if (order.getCouponId() != null) {
    Coupon coupon = couponRepository.findByCouponId(order.getCouponId())
        .orElseThrow(COUPON_NOT_FOUND);
    coupon.use(order.getOrderIdValue(), now);
    couponRepository.save(coupon);
}
order.pay();
```

### 3. 优惠券回滚策略

```
// 在 cancelOrder 方法中
if (order.getCouponId() != null && couponWasUsed(order)) {
    Coupon coupon = couponRepository.findByCouponId(order.getCouponId())
        .orElseThrow(COUPON_NOT_FOUND);
    coupon.unuse();
    couponRepository.save(coupon);
}
order.cancel();
```

**跨聚合一致性：**
- 采用本地事务（@Transactional）保证 Order 和 Coupon 的状态一致性
- 一个事务内修改两个聚合，这是允许的最终一致性场景

### 4. 金额计算策略

```
totalAmount = sum(item.subtotal)      // 原始金额，不变
discountAmount = coupon.calculateDiscount(totalAmount)  // 折扣金额
finalAmount = totalAmount - discountAmount              // 最终金额
```

**注意：**
- totalAmount 保持原始商品总金额，不因优惠券而改变
- discountAmount 单独记录，便于财务对账
- finalAmount 动态计算，不持久化（或在 DO 中冗余存储）

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/orders | 创建订单 | `{ "customerId": "...", "items": [...], "couponId": "..." (可选) }` | `{ "success": true, "data": OrderDTO }` |
| POST | /api/v1/orders/from-cart | 从购物车创建订单 | `{ "customerId": "...", "couponId": "..." (可选) }` | `{ "success": true, "data": OrderDTO }` |
| POST | /api/v1/orders/{orderId}/pay | 支付订单 | — | `{ "success": true, "data": OrderDTO }` |
| POST | /api/v1/orders/{orderId}/cancel | 取消订单 | — | `{ "success": true, "data": OrderDTO }` |

**OrderDTO 扩展字段：**
```json
{
  "orderId": "...",
  "customerId": "...",
  "status": "CREATED",
  "totalAmount": 100.00,
  "discountAmount": 10.00,
  "finalAmount": 90.00,
  "couponId": "COUPON_xxx",
  "currency": "CNY",
  "items": [...],
  "createTime": "...",
  "updateTime": "..."
}
```

## 数据库设计

**t_order 表扩展：**

```sql
-- 新增字段
ALTER TABLE t_order ADD COLUMN coupon_id VARCHAR(64) NULL COMMENT '优惠券ID';
ALTER TABLE t_order ADD COLUMN discount_amount DECIMAL(19, 2) NOT NULL DEFAULT 0 COMMENT '折扣金额';
ALTER TABLE t_order ADD COLUMN final_amount DECIMAL(19, 2) NOT NULL DEFAULT 0 COMMENT '最终金额';
```

**完整 DDL：**
```sql
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    customer_id VARCHAR(64) NOT NULL COMMENT '客户ID',
    status VARCHAR(32) NOT NULL COMMENT '订单状态',
    total_amount DECIMAL(19, 2) NOT NULL COMMENT '订单总金额',
    discount_amount DECIMAL(19, 2) NOT NULL DEFAULT 0 COMMENT '折扣金额',
    final_amount DECIMAL(19, 2) NOT NULL DEFAULT 0 COMMENT '最终应付金额',
    coupon_id VARCHAR(64) NULL COMMENT '关联优惠券ID',
    currency VARCHAR(8) NOT NULL COMMENT '币种',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

**索引策略：**
- 已有：order_id（唯一）、customer_id（普通）
- 新增：coupon_id（普通，用于查询使用该优惠券的订单）

## 影响范围

按 DDD 分层列出新增/修改的类：

### domain
- **修改**：`com.claudej.domain.order.model.aggregate.Order`
  - 新增字段：couponId, discountAmount, finalAmount
  - 新增方法：applyCoupon(CouponId, Money), removeCoupon()
  - 修改 reconstruct() 工厂方法
- **修改**：`com.claudej.domain.coupon.model.aggregate.Coupon`
  - 新增方法：unuse() — 回滚优惠券状态
- **新增 ErrorCode**：
  - COUPON_NOT_BELONG_TO_USER("优惠券不属于该用户")
  - COUPON_NOT_AVAILABLE("优惠券不可用")
  - COUPON_MIN_ORDER_AMOUNT_NOT_MET("订单金额未达到优惠券最低消费")

### application
- **修改**：`com.claudej.application.order.command.CreateOrderCommand`
  - 新增可选字段：couponId
- **修改**：`com.claudej.application.order.command.CreateOrderFromCartCommand`
  - 已有 couponId 字段，需启用
- **修改**：`com.claudej.application.order.dto.OrderDTO`
  - 新增字段：couponId, discountAmount, finalAmount
- **修改**：`com.claudej.application.order.assembler.OrderAssembler`
  - 更新 toDTO 方法映射新字段
- **修改**：`com.claudej.application.order.service.OrderApplicationService`
  - 新增 CouponRepository 依赖
  - 修改 createOrder() — 添加优惠券验证和计算逻辑
  - 修改 createOrderFromCart() — 添加优惠券验证和计算逻辑
  - 修改 payOrder() — 添加优惠券核销逻辑
  - 修改 cancelOrder() — 添加优惠券回滚逻辑

### infrastructure
- **修改**：`com.claudej.infrastructure.order.persistence.dataobject.OrderDO`
  - 新增字段：couponId, discountAmount, finalAmount
- **修改**：`com.claudej.infrastructure.order.persistence.converter.OrderConverter`
  - 更新 toDomain() 和 toDO() 方法
- **修改**：`com.claudej.infrastructure.order.persistence.repository.OrderRepositoryImpl`
  - 修改 save() — 保存 couponId 和金额字段

### adapter
- 无新增类，现有 Controller 方法支持（已通过 Command 对象扩展）

### start
- **修改**：`claude-j-start/src/main/resources/db/schema.sql`
  - 更新 t_order 表结构

## 待确认事项

1. **订单金额精度**：discountAmount 和 finalAmount 使用 DECIMAL(19,2)，与 Money 值对象保持一致
2. **优惠券回滚时机**：订单取消时无条件回滚（即使订单未支付但已关联优惠券）
3. **finalAmount 持久化策略**：在 DO 中冗余存储 finalAmount，便于查询和报表
4. **跨用户优惠券使用**：严格禁止，必须通过归属验证

---

## 架构评审

**评审人**：@architect
**日期**：2026-04-19
**结论**：✅ 通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）
  - Order 聚合封装金额不变量（finalAmount = totalAmount - discountAmount）
  - Coupon 聚合封装状态转换（use/unuse）
  - 跨聚合操作通过 Application 层协调，符合六边形架构
- [x] 值对象识别充分（金额、标识符等应为 VO）
  - discountAmount, finalAmount 使用 Money 值对象（已存在）
  - couponId 使用 CouponId 值对象（已存在）
- [x] Repository 端口粒度合适（方法不多不少）
  - 复用现有 CouponRepository.findByCouponId() 和 save()
  - 无需新增 Repository 方法
- [x] 与已有聚合无循环依赖
  - Order 仅依赖 CouponId（值对象），不直接依赖 Coupon 聚合
  - Application 层通过 Repository 协调，符合依赖方向
- [x] DDL 设计与领域模型一致（字段映射、索引合理）
  - t_order 新增 coupon_id, discount_amount, final_amount
  - 字段类型 DECIMAL(19,2) 与 Money 值对象一致
  - 新增 coupon_id 索引便于反向查询
- [x] API 设计符合 RESTful 规范
  - 复用现有端点，通过 Command 扩展参数
  - OrderDTO 扩展响应字段，保持向后兼容
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）
  - 已规划 OrderConverter 更新 toDomain/toDO
  - 已规划 OrderAssembler 更新 toDTO

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义
  - 名词定义：discountAmount（折扣金额）、finalAmount（最终应付金额）
  - 流程：下单验证 → 支付核销 → 取消回滚
  - 异常分支：COUPON_NOT_FOUND, COUPON_NOT_BELONG_TO_USER, COUPON_NOT_AVAILABLE, COUPON_MIN_ORDER_AMOUNT_NOT_MET
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例
  - 设计文档中的不变量约束可直接映射为测试
  - 状态转换场景（AVAILABLE → USED → AVAILABLE）已明确
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明
  - 金额不变量：finalAmount = totalAmount - discountAmount >= 0
  - 状态机：CouponStatus 已有 AVAILABLE → USED 转换，需补充 USED → AVAILABLE 回滚
  - 边界值：discountAmount <= totalAmount, finalAmount >= 0

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出
  - 18 个任务步骤，每个步骤均含：文件路径、变更说明、验证命令、预期输出、commit 消息
  - 符合模板要求（`docs/exec-plan/templates/task-plan.template.md` 原子任务章节）
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置
  - 计划顺序：Domain（1-3）→ Application（4-7）→ Infrastructure（8-11）→ Start（12）→ 全量验证（13-15）

**可测性保障（3 项）**
- [x] **AC 自动化全覆盖**：`test-case-design.md` 的「AC 自动化覆盖矩阵」每条 AC 都有对应自动化测试方法
  - 设计阶段尚未生成 test-case-design.md，由 @qa 在 Build 完成后补充
  - 设计已预留测试点：金额计算、状态回滚、异常场景
- [x] **可测的注入方式**：若引入新 Spring Bean，使用构造函数注入而非字段注入
  - OrderApplicationService 已有构造函数注入模式
  - CouponRepository 将通过构造函数注入（符合 java-dev.md 规则）
- [x] **配置校验方式合规**：若涉及敏感/跨环境配置校验，使用 `@ConfigurationProperties + @Validated`
  - 本任务不涉及新配置，仅使用现有 Repository 和领域逻辑

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：需求未要求的抽象/配置/工厂已移除
  - 无新增 Factory/Strategy，复用现有模式
- [x] **外科性**：设计仅改动任务直接相关的文件
  - 文件清单明确，无顺手优化其他聚合的计划
- [x] **假设显性**：需求里含糊的字段/边界/异常，requirement-design 已在「假设与待确认」列出
  - 4 项待确认事项已明确列出

### 发现的问题与建议

#### 1. **Money 类缺少 subtract 方法**（需补充）
当前 `Money` 类只有 `add` 方法，没有 `subtract` 方法。在 Order.applyCoupon() 中需要计算 `finalAmount = totalAmount - discountAmount`。

**建议**：
- 在 Domain 层 Money 类添加 `subtract(Money other)` 方法
- 或添加 `minus`/`subtract` 语义方法

```java
public Money subtract(Money other) {
    if (!this.currency.equals(other.currency)) {
        throw new BusinessException(ErrorCode.CART_CURRENCY_MISMATCH, "币种不一致，无法相减");
    }
    return new Money(this.amount.subtract(other.amount), this.currency);
}
```

#### 2. **CouponStatus 需要扩展回滚转换**（需补充）
当前 CouponStatus 只有 `toUsed()` 和 `toExpired()` 方法，没有 `toAvailable()` 或类似回滚方法。

**建议**：
在 CouponStatus 枚举中添加：
```java
public boolean canUnuse() {
    return this == USED;
}

public CouponStatus toAvailable() {
    if (!canUnuse()) {
        throw new BusinessException(ErrorCode.INVALID_COUPON_STATUS_TRANSITION,
                "优惠券状态 " + this + " 不允许回滚");
    }
    return AVAILABLE;
}
```

#### 3. **CreateOrderCommand 缺少 couponId 字段**（已识别）
当前 CreateOrderCommand 没有 couponId 字段，与设计文档一致（需要添加）。

**确认**：task-plan 2.1 已规划此变更。

### 架构基线检查结果

```bash
$ ./scripts/entropy-check.sh
...
{"issues": 0, "warnings": 12, "status": "PASS"}
架构合规检查通过。
```

- 退出码：0（PASS）
- FAIL 级问题：0
- WARN 级问题：12（与本次任务无关的既有警告）

### 评审意见

1. **设计符合六边形架构**：跨聚合协调放在 Application 层，Domain 层保持纯净
2. **事务边界清晰**：@Transactional 包裹 Application Service 方法，保证 Order + Coupon 状态一致性
3. **金额处理正确**：使用 Money 值对象，避免 BigDecimal 精度问题
4. **待办补充**：
   - Domain 层：Money 添加 subtract 方法
   - Domain 层：CouponStatus 添加 toAvailable() 回滚方法
   - Domain 层：Coupon 添加 unuse() 方法（已在设计中）

### 需要新增的 ADR

无。本任务为既有聚合的功能扩展，不涉及新的架构决策。

---

**评审结论**：✅ 设计通过，可以进入 Build 阶段。

**建议**：在 Build 阶段第一步（Money 扩展）和第二步（CouponStatus 扩展）中补充上述缺失方法。
