# 测试用例设计 — 006-coupon-service

## 测试范围
优惠券服务全功能测试，覆盖 Domain/Application/Infrastructure/Adapter 四层及全链路集成测试。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 一、Domain 层测试场景

### CouponId 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new CouponId("abc123") | 创建成功，value="abc123" |
| D2 | 自动trim | - | new CouponId("  abc123  ") | value="abc123" |
| D3 | null值 | - | new CouponId(null) | 抛 BusinessException |
| D4 | 空字符串 | - | new CouponId("") | 抛 BusinessException |
| D5 | 空白字符串 | - | new CouponId("   ") | 抛 BusinessException |
| D6 | 相等性 | - | 两个相同值 | equals 返回 true，hashCode 相等 |
| D7 | 不相等 | - | 两个不同值 | equals 返回 false |

### DiscountValue 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D8 | 固定金额合法值 | - | new DiscountValue(20, FIXED_AMOUNT) | 创建成功 |
| D9 | 固定金额为零 | - | new DiscountValue(0, FIXED_AMOUNT) | 抛 BusinessException |
| D10 | 固定金额为负 | - | new DiscountValue(-10, FIXED_AMOUNT) | 抛 BusinessException |
| D11 | 百分比合法值 | - | new DiscountValue(50, PERCENTAGE) | 创建成功 |
| D12 | 百分比边界1 | - | new DiscountValue(1, PERCENTAGE) | 创建成功 |
| D13 | 百分比边界100 | - | new DiscountValue(100, PERCENTAGE) | 创建成功 |
| D14 | 百分比小于1 | - | new DiscountValue(0, PERCENTAGE) | 抛 BusinessException |
| D15 | 百分比大于100 | - | new DiscountValue(101, PERCENTAGE) | 抛 BusinessException |
| D16 | 百分比非整数 | - | new DiscountValue(50.5, PERCENTAGE) | 抛 BusinessException |
| D17 | value为null | - | new DiscountValue(null, FIXED_AMOUNT) | 抛 BusinessException |
| D18 | type为null | - | new DiscountValue(20, null) | 抛 BusinessException |
| D19 | 相等性 | - | 两个相同值和类型 | equals 返回 true |
| D20 | 不相等（不同类型） | - | 相同值不同类型 | equals 返回 false |

### CouponStatus 枚举
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D21 | 可使用检查 | - | AVAILABLE.canUse() | 返回 true |
| D22 | 不可使用检查 | - | USED.canUse() / EXPIRED.canUse() | 返回 false |
| D23 | 可过期检查 | - | AVAILABLE.canExpire() | 返回 true |
| D24 | 不可过期检查 | - | USED.canExpire() / EXPIRED.canExpire() | 返回 false |
| D25 | 转为已使用 | - | AVAILABLE.toUsed() | 返回 USED |
| D26 | 已使用不能再用 | - | USED.toUsed() | 抛 BusinessException |
| D27 | 已过期不能再用 | - | EXPIRED.toUsed() | 抛 BusinessException |
| D28 | 转为已过期 | - | AVAILABLE.toExpired() | 返回 EXPIRED |
| D29 | 已使用不能过期 | - | USED.toExpired() | 抛 BusinessException |
| D30 | 已过期不能再过期 | - | EXPIRED.toExpired() | 抛 BusinessException |

### Coupon 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D31 | 创建固定金额券 | - | Coupon.create("满100减20", FIXED_AMOUNT, 20, 100, "user", from, until) | 创建成功，状态 AVAILABLE |
| D32 | 创建百分比券 | - | Coupon.create("8折", PERCENTAGE, 20, 0, "user", from, until) | 创建成功，类型 PERCENTAGE |
| D33 | minOrderAmount默认0 | minOrderAmount=null | Coupon.create(...) | minOrderAmount=0 |
| D34 | 名称为空 | - | Coupon.create("", ...) | 抛 BusinessException |
| D35 | 名称为null | - | Coupon.create(null, ...) | 抛 BusinessException |
| D36 | 名称超长 | 名称>50字符 | Coupon.create(...) | 抛 BusinessException |
| D37 | userId为空 | - | Coupon.create(..., "", ...) | 抛 BusinessException |
| D38 | 有效期结束早于开始 | from > until | Coupon.create(...) | 抛 BusinessException |
| D39 | 有效期相等 | from = until | Coupon.create(...) | 抛 BusinessException |
| D40 | minOrderAmount为负 | - | Coupon.create(..., -1, ...) | 抛 BusinessException |
| D41 | 使用优惠券 | AVAILABLE状态，有效期内 | coupon.use("order", now) | 状态变 USED，usedOrderId 设置 |
| D42 | 使用过期券 | 已过有效期 | coupon.use("order", afterExpiry) | 抛 BusinessException |
| D43 | 使用未生效券 | 未到有效期 | coupon.use("order", beforeValid) | 抛 BusinessException |
| D44 | 使用已使用券 | USED状态 | coupon.use("order", now) | 抛 BusinessException |
| D45 | orderId为空 | - | coupon.use("", now) | 抛 BusinessException |
| D46 | orderId为null | - | coupon.use(null, now) | 抛 BusinessException |
| D47 | 懒过期检查通过 | 已过有效期 | checkAndExpire(afterExpiry) | 返回 true，状态变 EXPIRED |
| D48 | 懒过期未到期 | 在有效期内 | checkAndExpire(withinValidity) | 返回 false，状态不变 |
| D49 | 已使用不检查过期 | USED状态 | checkAndExpire(afterExpiry) | 返回 false，状态不变 |
| D50 | 固定金额折扣计算 | 订单金额 > 折扣值 | calculateDiscount(200) | 返回 20 |
| D51 | 固定金额不超过订单 | 订单金额 < 折扣值 | calculateDiscount(30) with 折扣50 | 返回 30 |
| D52 | 百分比折扣计算 | - | calculateDiscount(100) with 20% | 返回 20 |
| D53 | 百分比向下取整 | - | calculateDiscount(33.33) with 15% | 返回 4.99 |
| D54 | 订单金额为null | - | calculateDiscount(null) | 返回 0 |
| D55 | 订单金额为0 | - | calculateDiscount(0) | 返回 0 |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 创建优惠券 | 有效命令 | createCoupon(cmd) | 返回 CouponDTO，调用 repository.save |
| A2 | 创建命令为null | - | createCoupon(null) | 抛 BusinessException |
| A3 | 按ID查询存在 | coupon存在 | getCouponById(id) | 返回 CouponDTO |
| A4 | 按ID查询不存在 | - | getCouponById("不存在") | 抛 BusinessException |
| A5 | 按用户查询 | 用户有优惠券 | getCouponsByUserId(userId) | 返回列表 |
| A6 | 查询可用优惠券 | 有可用券 | getAvailableCouponsByUserId(userId) | 返回可用列表 |
| A7 | 使用优惠券 | 券存在且可用 | useCoupon(id, cmd) | 返回已使用 CouponDTO，调用 save |
| A8 | 使用命令orderId为null | - | useCoupon(id, nullOrderCmd) | 抛 BusinessException |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存新优惠券 | coupon无id | save(coupon) | 返回有id的coupon |
| I2 | 按ID查询存在 | 已保存 | findByCouponId(id) | 返回 Optional.of(coupon) |
| I3 | 按ID查询不存在 | - | findByCouponId("不存在") | 返回 Optional.empty |
| I4 | 按用户查询 | 用户有多张券 | findByUserId(userId) | 返回列表 |
| I5 | 查询可用优惠券 | 有可用券 | findAvailableByUserId(userId) | 返回可用列表 |
| I6 | 懒过期标记 | 有过期券 | findAvailableByUserId | 过期券被标记，不出现在结果中 |
| I7 | 判断存在 | 已保存 | existsByCouponId(id) | 返回 true |
| I8 | 判断不存在 | - | existsByCouponId("不存在") | 返回 false |
| I9 | 更新优惠券 | 已保存，状态变更 | save(coupon) | 更新成功，状态持久化 |
| I10 | DO转换完整性 | - | save → findByCouponId | 所有字段完整还原 |
| I11 | 固定金额折扣 | - | calculateDiscount(150) | 返回 20 |
| I12 | 百分比折扣 | - | calculateDiscount(100) with 20% | 返回 20 |

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建优惠券成功 | Mock正常返回 | POST /api/v1/coupons | 200 + success=true |
| W2 | 创建参数校验失败 | name为空 | POST /api/v1/coupons | 400 |
| W3 | 按ID查询成功 | Mock正常返回 | GET /api/v1/coupons/{id} | 200 + success=true |
| W4 | 查询不存在 | Mock抛异常 | GET /api/v1/coupons/不存在 | 404 + errorCode=COUPON_NOT_FOUND |
| W5 | 按用户查询成功 | Mock正常返回 | GET /api/v1/coupons?userId=xxx | 200 + success=true |
| W6 | 查询可用券成功 | Mock正常返回 | GET /api/v1/coupons/available?userId=xxx | 200 + success=true |
| W7 | 使用优惠券成功 | Mock正常返回 | POST /api/v1/coupons/{id}/use | 200 + status=USED |
| W8 | 使用无效券 | Mock抛异常 | POST /api/v1/coupons/{id}/use | 400 + errorCode=INVALID_COUPON_STATUS_TRANSITION |
| W9 | 使用参数校验失败 | orderId为空 | POST /api/v1/coupons/{id}/use | 400 |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 创建并查询 | POST /api/v1/coupons → GET /api/v1/coupons/{id} | 创建成功，查询返回一致数据 |
| E2 | 按用户查询列表 | 创建多张券 → GET /api/v1/coupons?userId=xxx | 返回用户所有券 |
| E3 | 使用优惠券 | 创建 → 使用 → 查询可用 | 使用后状态变 USED，可用列表为空 |
| E4 | 查询404 | GET /api/v1/coupons/NONEXISTENT | 404 + errorCode=COUPON_NOT_FOUND |
| E5 | 参数校验400 | POST /api/v1/coupons (name="") | 400 Bad Request |
| E6 | 懒过期 | 创建过期券 → 查询可用 → 查询详情 | 可用为空，详情状态为 EXPIRED |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（非贫血模型）
- [x] 值对象不可变，字段 final
- [x] 值对象 equals/hashCode 正确
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response
- [x] Controller 无业务逻辑
- [x] 异常通过 GlobalExceptionHandler 统一处理

---

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [x] DO 用 @Data + @TableName，DTO 用 @Data
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx
