---
task-id: "008-order-from-cart"
from: architect
to: dev
status: approved
timestamp: "2026-04-15T11:30:00"
summary: "架构评审通过，可以进入 Build 阶段"
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
- [x] approved - 设计通过，可以进入 Build 阶段
- [ ] changes-requested - 需要修改设计

### 具体意见

详见 `requirement-design.md` 中「架构评审」章节。核心结论：

1. **应用服务层依赖**：合理，应用服务职责就是编排多个聚合
2. **事务边界**：单服务场景下 `@Transactional` 足够，无需分布式事务
3. **用户状态验证**：设计已预留扩展点，M1 阶段可跳过
4. **优惠券扩展点**：设计合理，couponId 作为可选字段传入
5. **DDD 架构合规**：完全符合六边形架构约束
6. **Java 8 规范**：合规

### 补充 ADR
- [ ] 需要创建 ADR 记录重要决策
- 无需新增 ADR，本功能遵循已有架构决策

## 评审人
- 评审者：@architect
- 评审时间：2026-04-15
