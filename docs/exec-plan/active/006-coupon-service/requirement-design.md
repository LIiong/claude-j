# 需求拆分设计 — 006-coupon-service

## 需求描述
实现优惠券服务，支持创建优惠券（固定金额/百分比折扣两种类型）、用户使用优惠券（校验有效性并核销）、查询优惠券（按优惠券ID查询、按用户查询可用列表）。优惠券作为电商系统的核心营销工具，需要严格的业务不变量保护。

## 领域分析

### 聚合根: Coupon
优惠券聚合根，封装优惠券全生命周期的业务不变量。

- couponId (CouponId) — 优惠券唯一业务标识，UUID 生成
- name (String) — 优惠券名称，1-50 字符
- discountType (DiscountType) — 折扣类型：FIXED_AMOUNT（固定金额）/ PERCENTAGE（百分比）
- discountValue (DiscountValue) — 折扣值（固定金额时为具体金额，百分比时为 1-100 的整数）
- minOrderAmount (Money) — 最低订单金额门槛（满减条件），可为 0
- status (CouponStatus) — 状态：AVAILABLE -> USED / EXPIRED
- userId (String) — 所属用户ID
- validFrom (LocalDateTime) — 有效期开始时间
- validUntil (LocalDateTime) — 有效期截止时间
- usedTime (LocalDateTime) — 使用时间，使用后填充
- usedOrderId (String) — 关联订单号，使用后填充
- createTime (LocalDateTime)
- updateTime (LocalDateTime)

**业务不变量**：
1. 只有 AVAILABLE 状态的优惠券才能被使用
2. 使用时必须校验有效期（validFrom <= now <= validUntil）
3. 使用时必须关联订单号
4. 百分比折扣值范围 1-100
5. 固定金额折扣值必须大于 0
6. validFrom 必须早于 validUntil
7. 已过期/已使用的优惠券不可回退到 AVAILABLE

### 值对象

- **CouponId**: 优惠券唯一标识，包装 String，不可变，重写 equals/hashCode
- **DiscountType**: 枚举，FIXED_AMOUNT / PERCENTAGE
- **DiscountValue**: 折扣值，包装 BigDecimal + DiscountType，根据类型进行不同校验
  - FIXED_AMOUNT: > 0
  - PERCENTAGE: 1-100 整数
- **CouponStatus**: 枚举，AVAILABLE / USED / EXPIRED。封装状态转换规则：
  - AVAILABLE -> USED（使用）
  - AVAILABLE -> EXPIRED（过期）
  - 其他转换抛 BusinessException

### 领域服务（如有）
无需领域服务，所有业务逻辑由聚合根自身封装。

### 端口接口
- **CouponRepository**:
  - `Coupon save(Coupon coupon)` — 保存优惠券
  - `Optional<Coupon> findByCouponId(CouponId couponId)` — 按业务 ID 查询
  - `List<Coupon> findByUserId(String userId)` — 按用户查询所有优惠券
  - `List<Coupon> findAvailableByUserId(String userId)` — 按用户查询可用优惠券
  - `boolean existsByCouponId(CouponId couponId)` — 判断是否存在

## 关键算法/技术方案

### 折扣计算
优惠券本身不计算最终金额，仅封装折扣值和类型。实际金额计算由订单聚合在使用优惠券时完成（跨聚合集成属于未来 story）。本期优惠券聚合仅提供 `calculateDiscount(Money orderAmount)` 方法，返回折扣金额：
- FIXED_AMOUNT: 返回固定折扣值（不超过订单金额）
- PERCENTAGE: 返回 orderAmount * percentage / 100（向下取整到分）

### 过期处理
过期状态由查询时判断（懒过期）：查询时如果当前时间超过 validUntil 且状态为 AVAILABLE，则自动转为 EXPIRED 并持久化。

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/coupons | 创建优惠券 | `CreateCouponRequest` | `{ "success": true, "data": CouponResponse }` |
| POST | /api/v1/coupons/{couponId}/use | 使用优惠券 | `UseCouponRequest` | `{ "success": true, "data": CouponResponse }` |
| GET | /api/v1/coupons/{couponId} | 查询单个优惠券 | -- | `{ "success": true, "data": CouponResponse }` |
| GET | /api/v1/coupons?userId=xxx | 查询用户优惠券列表 | -- | `{ "success": true, "data": [CouponResponse] }` |
| GET | /api/v1/coupons/available?userId=xxx | 查询用户可用优惠券 | -- | `{ "success": true, "data": [CouponResponse] }` |

### CreateCouponRequest
```json
{
  "name": "满100减20",
  "discountType": "FIXED_AMOUNT",
  "discountValue": 20.00,
  "minOrderAmount": 100.00,
  "userId": "user123",
  "validFrom": "2026-04-14T00:00:00",
  "validUntil": "2026-05-14T23:59:59"
}
```

### UseCouponRequest
```json
{
  "orderId": "order456"
}
```

### CouponResponse
```json
{
  "couponId": "abc123",
  "name": "满100减20",
  "discountType": "FIXED_AMOUNT",
  "discountValue": 20.00,
  "minOrderAmount": 100.00,
  "status": "AVAILABLE",
  "userId": "user123",
  "validFrom": "2026-04-14T00:00:00",
  "validUntil": "2026-05-14T23:59:59",
  "usedTime": null,
  "usedOrderId": null,
  "createTime": "2026-04-14T10:00:00",
  "updateTime": "2026-04-14T10:00:00"
}
```

## 数据库设计

```sql
CREATE TABLE IF NOT EXISTS t_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id VARCHAR(32) NOT NULL COMMENT '优惠券唯一业务标识',
    name VARCHAR(50) NOT NULL COMMENT '优惠券名称',
    discount_type VARCHAR(20) NOT NULL COMMENT '折扣类型：FIXED_AMOUNT/PERCENTAGE',
    discount_value DECIMAL(12,2) NOT NULL COMMENT '折扣值',
    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '最低订单金额门槛',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/USED/EXPIRED',
    user_id VARCHAR(64) NOT NULL COMMENT '所属用户ID',
    valid_from TIMESTAMP NOT NULL COMMENT '有效期开始',
    valid_until TIMESTAMP NOT NULL COMMENT '有效期截止',
    used_time TIMESTAMP NULL COMMENT '使用时间',
    used_order_id VARCHAR(64) NULL COMMENT '关联订单号',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    UNIQUE KEY uk_coupon_id (coupon_id),
    KEY idx_user_id (user_id),
    KEY idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';
```

**索引策略**：
- `uk_coupon_id`: 唯一索引，按业务 ID 快速查找
- `idx_user_id`: 普通索引，按用户查询所有优惠券
- `idx_user_status`: 联合索引，按用户查询指定状态的优惠券（最常见查询）

## 影响范围

- **domain**:
  - 新增聚合 `com.claudej.domain.coupon`
  - model/aggregate: `Coupon`
  - model/valobj: `CouponId`, `DiscountType`, `DiscountValue`, `CouponStatus`
  - repository: `CouponRepository`
  - 修改 `ErrorCode` 新增优惠券相关错误码
- **application**:
  - 新增 `com.claudej.application.coupon`
  - command: `CreateCouponCommand`, `UseCouponCommand`
  - dto: `CouponDTO`
  - assembler: `CouponAssembler`
  - service: `CouponApplicationService`
- **infrastructure**:
  - 新增 `com.claudej.infrastructure.coupon`
  - persistence/dataobject: `CouponDO`
  - persistence/mapper: `CouponMapper`
  - persistence/converter: `CouponConverter`
  - persistence/repository: `CouponRepositoryImpl`
- **adapter**:
  - 新增 `com.claudej.adapter.coupon`
  - web: `CouponController`
  - web/request: `CreateCouponRequest`, `UseCouponRequest`
  - web/response: `CouponResponse`
- **start**:
  - `schema.sql` 新增 `t_coupon` 表 DDL
