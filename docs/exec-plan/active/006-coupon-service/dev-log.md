# 开发日志 — 006-coupon-service

## 问题记录

<!-- 开发过程中遇到的问题、做出的决策及原因。按时间顺序记录。 -->

### 1. 优惠券折扣值建模
- **问题**：折扣值在固定金额和百分比两种模式下有不同的约束条件
- **决策**：创建 DiscountValue 值对象，内部持有 BigDecimal 值和 DiscountType，在构造时根据类型执行不同的校验逻辑
- **原因**：将校验逻辑封装在值对象中，符合 DDD 值对象自校验原则，避免在应用层重复校验

### 2. 过期策略选择
- **问题**：优惠券过期状态是定时批量更新还是查询时懒更新
- **决策**：采用懒过期策略，查询时检测并更新状态
- **原因**：避免引入定时任务的复杂性，对于电商场景查询频率足够高，懒过期即时性可接受

### 3. Money 值对象复用
- **问题**：优惠券的最低订单金额和折扣金额是否复用 order 聚合的 Money 值对象
- **决策**：复用 domain/common 或 order 的 Money（当前 Money 位于 order 包下）。考虑到 Money 是通用值对象，建议后续重构到 common 包。本期先直接在 coupon 聚合内部引用 order 的 Money，或在 coupon 内独立定义 minOrderAmount 为 BigDecimal + currency
- **原因**：为避免跨聚合引用，本期 coupon 聚合内的金额字段直接使用 BigDecimal + String(currency) 组合，不引入跨聚合依赖

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
- 无与原设计不一致的变更。

## 开发完成记录

### 2026-04-14 - Build 阶段完成

**今日完成**：
1. Domain 层开发完成
   - CouponId 值对象 + 测试
   - DiscountType 枚举
   - DiscountValue 值对象 + 测试
   - CouponStatus 枚举 + 测试
   - Coupon 聚合根 + 测试
   - CouponRepository 端口接口

2. Application 层开发完成
   - CreateCouponCommand、UseCouponCommand
   - CouponDTO
   - CouponAssembler（MapStruct）
   - CouponApplicationService + Mockito 测试

3. Infrastructure 层开发完成
   - CouponDO（@TableName("t_coupon")）
   - CouponMapper（继承 BaseMapper）
   - CouponConverter（DO ↔ Domain 转换）
   - CouponRepositoryImpl + SpringBootTest 集成测试

4. Adapter 层开发完成
   - CreateCouponRequest、UseCouponRequest（@Valid 校验）
   - CouponResponse
   - CouponController + WebMvcTest API 测试

5. Start 模块
   - schema.sql 添加 t_coupon DDL（claude-j-start 和 infrastructure test 资源）

**遇到的问题**：
1. CouponControllerTest 中两个测试用例初始返回 500 而非预期的 404/400
   - 原因：GlobalExceptionHandler 缺少 COUPON_NOT_FOUND 和 INVALID_COUPON_STATUS_TRANSITION 的 HTTP 状态映射
   - 解决：在 resolveHttpStatus 方法中添加对应 case

2. Infrastructure 测试报 BadSqlGrammar 错误
   - 原因：claude-j-infrastructure/src/test/resources/db/schema.sql 缺少 t_coupon 表定义
   - 解决：添加 t_coupon DDL 到测试资源 schema.sql

**测试统计**：
- Domain 层测试：4 个文件
- Application 层测试：1 个文件（CouponApplicationServiceTest）
- Infrastructure 层测试：1 个文件（CouponRepositoryImplTest）
- Adapter 层测试：1 个文件（CouponControllerTest）
- 全量测试：310 tests passed

**三项验证结果**：
- `mvn clean test`: 310/310 通过
- `mvn checkstyle:check`: 通过
- `./scripts/entropy-check.sh`: 通过（0 FAIL, 3 WARN）
