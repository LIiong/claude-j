---
task-id: "008-order-from-cart"
from: dev
to: architect
status: pending-review
timestamp: "2026-04-15T11:00:00"
summary: "Spec 阶段完成：从购物车创建订单功能设计"
---

# 交接文档

## 任务概述

实现从购物车创建订单的桥梁功能，连接购物车与订单模块。

## 设计要点

### 业务需求
- 支持用户从购物车直接创建订单
- 下单成功后自动清空购物车
- 事务保证数据一致性

### 技术方案
- **Domain 层**: 无变更，复用现有 Order、Cart 聚合
- **Application 层**: 新增 `createOrderFromCart` 方法，编排 Cart + Order 两个聚合
- **Adapter 层**: 新增 `POST /api/v1/orders/from-cart` 端点

### 事务边界
应用服务层使用 `@Transactional` 保证：
1. 查询购物车
2. 验证购物车非空
3. 创建订单
4. 保存订单
5. 清空购物车
6. 保存购物车（空状态）

以上所有操作在同一个事务中完成。

### 异常场景
| 场景 | HTTP状态码 | 错误码 |
|------|-----------|--------|
| 购物车不存在 | 404 | CART_NOT_FOUND |
| 购物车为空 | 400 | CART_EMPTY |
| 客户ID为空 | 400 | PARAM_INVALID |

### 变更清单

#### 新增文件
1. `CreateOrderFromCartRequest.java` - 请求对象
2. `CreateOrderFromCartCommand.java` - 命令对象

#### 修改文件
3. `ErrorCode.java` - 添加 CART_EMPTY 错误码
4. `OrderApplicationService.java` - 添加 createOrderFromCart 方法
5. `OrderController.java` - 添加 POST /from-cart 端点

#### 测试文件
6. `OrderApplicationServiceTest.java` - 应用服务单元测试（新增测试方法）
7. `OrderControllerTest.java` - 控制器单元测试（新增测试方法）
8. `OrderFromCartIntegrationTest.java` - 集成测试

## 待评审内容

1. 应用服务层同时依赖 CartRepository 和 OrderRepository 是否合理？
2. 事务边界是否合适？是否需要考虑分布式事务？
3. 是否需要添加用户状态验证？
4. 优惠券预留扩展点的设计是否合适？

## 文档清单

- [x] requirement-design.md - 需求设计（已存在）
- [x] task-plan.md - 任务计划（已创建）
- [x] dev-log.md - 开发日志（已创建，空文件）

---

# 架构评审回复区

<!-- architect 评审后填写 -->

## 评审意见

### 评审结论
- [ ] approved - 设计通过，可以进入 Build 阶段
- [ ] changes-requested - 需要修改设计

### 具体意见

<!-- 如有修改意见，请列出具体条目 -->

### 补充 ADR
- [ ] 需要创建 ADR 记录重要决策
- ADR 文件路径：（如适用）

## 评审人
- 评审者：@architect
- 评审时间：
