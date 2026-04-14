# 需求拆分设计 — 002-order-service

## 需求描述
实现电商订单聚合的完整 CRUD 功能，包括创建订单（含订单项）、查询订单、取消订单、订单支付确认。订单聚合封装金额计算、状态流转等业务不变量。

## 领域分析

### 聚合根: Order
- orderId (OrderId VO) — 业务订单号，UUID 生成
- customerId (CustomerId VO) — 客户标识
- status (OrderStatus VO) — 订单状态枚举（CREATED → PAID → SHIPPED → DELIVERED / CANCELLED）
- items (List<OrderItem> Entity) — 订单项列表
- totalAmount (Money VO) — 订单总金额（自动计算）
- createTime — 创建时间
- updateTime — 更新时间

### 实体: OrderItem
- productId (String) — 商品 ID
- productName (String) — 商品名称
- quantity (int) — 数量（>0）
- unitPrice (Money VO) — 单价
- subtotal (Money VO) — 小计（自动计算：quantity * unitPrice）

### 值对象
- **OrderId**: 订单号，非空字符串，UUID 格式
- **CustomerId**: 客户 ID，非空字符串
- **OrderStatus**: 枚举 — CREATED, PAID, SHIPPED, DELIVERED, CANCELLED；封装状态转换规则
- **Money**: 金额+币种，amount >= 0，currency 非空；支持 add/multiply 运算；不可变

### 领域服务（如有）
无，所有业务逻辑封装在 Order 聚合根中。

### 端口接口
- **OrderRepository**:
  - `Order save(Order order)` — 保存订单
  - `Optional<Order> findByOrderId(OrderId orderId)` — 按订单号查询
  - `List<Order> findByCustomerId(CustomerId customerId)` — 按客户查询

## 关键算法/技术方案
- 订单号生成：UUID.randomUUID()，在 Order.create() 工厂方法中生成
- 金额计算：Order.addItem() 时自动重算 totalAmount = sum(item.subtotal)
- 状态机：Order 聚合根方法 pay()/ship()/deliver()/cancel() 封装状态转换规则，非法转换抛 BusinessException

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/orders | 创建订单 | `CreateOrderRequest` | `ApiResult<OrderResponse>` |
| GET | /api/v1/orders/{orderId} | 查询订单 | — | `ApiResult<OrderResponse>` |
| POST | /api/v1/orders/{orderId}/pay | 支付确认 | — | `ApiResult<OrderResponse>` |
| POST | /api/v1/orders/{orderId}/cancel | 取消订单 | — | `ApiResult<OrderResponse>` |

## 数据库设计
DDL 已存在于 `claude-j-start/src/main/resources/db/schema.sql`（t_order + t_order_item）。

## 影响范围
- **domain**: Order, OrderItem, OrderId, CustomerId, OrderStatus, Money, OrderRepository
- **application**: CreateOrderCommand, OrderDTO, OrderItemDTO, OrderAssembler, OrderApplicationService
- **infrastructure**: OrderDO, OrderItemDO, OrderMapper, OrderItemMapper, OrderConverter, OrderRepositoryImpl
- **adapter**: OrderController, CreateOrderRequest, OrderItemRequest, OrderResponse, OrderItemResponse
- **start**: 无变更（DDL 已存在）

---

## 架构评审

**评审人**: @architect
**评审日期**: 2026-04-13
**评审结果**: ✅ **APPROVED**

### 评审检查清单

| 检查项 | 状态 | 备注 |
|--------|------|------|
| 聚合边界清晰 | ✅ | Order 聚合根 + OrderItem 实体，符合 DDD 聚合定义 |
| 值对象不可变 | ✅ | OrderId, CustomerId, OrderStatus, Money 均为值对象 |
| 状态机封装 | ✅ | 状态转换规则封装在 Order 聚合根内 |
| 业务不变量 | ✅ | 金额自动计算，禁止外部直接修改 |
| Repository 端口 | ✅ | 接口定义在 domain 层，符合端口-适配器模式 |
| DDL 一致性 | ✅ | schema.sql 已存在 t_order + t_order_item |
| API RESTful | ✅ | 资源路径设计符合 REST 规范 |

### 设计亮点

1. **状态机设计**: CREATED→PAID→SHIPPED→DELIVERED/CANCELLED 状态流转清晰，符合电商订单生命周期
2. **金额计算**: 自动计算 totalAmount = sum(item.subtotal)，封装在聚合根内
3. **值对象复用**: Money 值对象可与未来其他聚合（如支付）共享

### 注意事项

1. **ErrorCode 扩展**: 需要在 `ErrorCode.java` 中添加 Order 相关错误码：
   - `ORDER_NOT_FOUND`
   - `ORDER_ITEM_QUANTITY_INVALID`
   - `ORDER_ALREADY_PAID`
   - `ORDER_CANNOT_CANCEL`
   - `INVALID_ORDER_STATUS_TRANSITION`

2. **状态转换原子性**: 确保状态转换方法（pay/ship/deliver/cancel）在 Application 层使用 `@Transactional` 保护

3. **Money 值对象**: 检查是否已存在 Money 实现，避免重复定义；如不存在需新建

### 建议（非阻塞）

- 可考虑添加 `OrderSnapshot` 用于订单历史版本记录（未来迭代）
- 可考虑使用领域事件发布订单状态变更（未来迭代）

**结论**: 设计符合 DDD + 六边形架构规范，可以进入 Build 阶段。
