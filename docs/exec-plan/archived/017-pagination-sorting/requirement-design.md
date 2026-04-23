# 需求拆分设计 — 017-pagination-sorting

## 需求描述

为所有返回列表数据的 REST API 接口增加分页与排序支持，解决全表返回导致的 OOM 风险和性能问题。采用统一的分页参数（page/size/sortField/sortDirection）和分页响应结构（PageResponse），遵循 DDD 六边形架构的分层约束。

## 领域分析

### 值对象（新增，位于 domain/common）

本次需求不新增聚合根，而是为现有所有聚合提供通用的分页能力。

#### PageRequest（分页请求值对象）
- **page** (int) — 页码，从 0 开始，默认 0，约束 >= 0
- **size** (int) — 每页条数，默认 20，约束 1-100
- **sortField** (String) — 排序字段，可选，默认 null（不排序）
- **sortDirection** (SortDirection) — 排序方向，可选，默认 ASC

#### SortDirection（排序方向枚举）
- **ASC** — 升序
- **DESC** — 降序

#### Page<T>（分页结果值对象）
- **content** (List<T>) — 数据列表
- **totalElements** (long) — 总条数
- **totalPages** (int) — 总页数
- **page** (int) — 当前页码
- **size** (int) — 每页条数
- **first** (boolean) — 是否首页
- **last** (boolean) — 是否末页
- **empty** (boolean) — 是否为空

### 端口接口改造

为现有 Repository 接口新增分页查询方法：

| Repository | 新增方法签名 |
|------------|-------------|
| LinkRepository | `Page<Link> findAll(PageRequest pageRequest)` |
| LinkRepository | `Page<Link> findByCategory(LinkCategory category, PageRequest pageRequest)` |
| UserRepository | `Page<User> findByInviterId(UserId inviterId, PageRequest pageRequest)` |
| CouponRepository | `Page<Coupon> findByUserId(String userId, PageRequest pageRequest)` |
| CouponRepository | `Page<Coupon> findAvailableByUserId(String userId, PageRequest pageRequest)` |
| OrderRepository | `Page<Order> findByCustomerId(CustomerId customerId, PageRequest pageRequest)` |

## 关键算法/技术方案

### 分页参数校验规则

| 参数 | 校验规则 | 默认值 |
|------|---------|--------|
| page | >= 0 | 0 |
| size | 1-100 | 20 |
| sortField | 允许的排序字段白名单（各聚合独立定义） | null |
| sortDirection | ASC/DESC | ASC |

### 排序字段白名单（防止 SQL 注入）

各聚合定义允许排序的字段：
- **link**: createTime, updateTime, name
- **user**: createTime, updateTime, username
- **coupon**: createTime, validUntil, discountValue
- **order**: createTime, updateTime, totalAmount, status

### MyBatis-Plus 分页实现

使用 MyBatis-Plus 的 `Page<T>` 对象进行分页：
- `selectPage(Page<T> page, Wrapper<T> queryWrapper)` 返回 `IPage<T>`
- 从 `IPage<T>` 提取数据转换为 Domain层的 `Page<T>`

### 假设与待确认

- **假设**：不改动原有的无参 `findAll()` 等方法签名，保留向后兼容（新增分页方法而非替换）
- **假设**：默认每页 20 条，上限 100 条（行业惯例，可调整）
- **待确认**：是否需要为列表接口增加 `hasNext` / `hasPrevious` 等导航信息？（当前 Page 设计已包含 first/last）

## API 设计

### 分页请求参数（Query String）

所有列表接口新增以下可选参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 0 | 页码，从 0 开始 |
| size | int | 否 | 20 | 每页条数，范围 1-100 |
| sortField | string | 否 | null | 排序字段 |
| sortDirection | string | 否 | ASC | 排序方向，ASC/DESC |

### 分页响应结构

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 5,
    "page": 0,
    "size": 20,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

### 接口改造清单

| 原接口 | 原返回 | 改造后返回 |
|--------|--------|-----------|
| GET /api/v1/links | List<LinkResponse> | PageResponse<LinkResponse> |
| GET /api/v1/links/category?category=xxx | List<LinkResponse> | PageResponse<LinkResponse> |
| GET /api/v1/users/{userId}/invited-users | List<UserResponse> | PageResponse<UserResponse> |
| GET /api/v1/coupons?userId=xxx | List<CouponResponse> | PageResponse<CouponResponse> |
| GET /api/v1/coupons/available?userId=xxx | List<CouponResponse> | PageResponse<CouponResponse> |
| GET /api/v1/orders?customerId=xxx | List<OrderResponse> | PageResponse<OrderResponse> |
| GET /api/v1/users/{userId}/orders | List<OrderResponse> | PageResponse<OrderResponse> |

## 数据库设计（无新增表）

本次需求不新增表，仅涉及查询方式改造。无需 DDL 变更。

为提高分页查询性能，建议确认现有索引覆盖：
- `t_link.idx_category` — 已存在
- `t_user.idx_inviter_id` — 已存在
- `t_coupon.idx_user_id, idx_user_status` — 已存在
- `t_order.idx_customer_id` — 已存在

## 影响范围

### domain（新增）
- `com.claudej.domain.common.model.valobj.PageRequest` — 分页请求值对象
- `com.claudej.domain.common.model.valobj.SortDirection` — 排序方向枚举
- `com.claudej.domain.common.model.valobj.Page` — 分页结果值对象

### domain（修改）
- `LinkRepository` — 新增 `findAll(PageRequest)`, `findByCategory(LinkCategory, PageRequest)`
- `UserRepository` — 新增 `findByInviterId(UserId, PageRequest)`
- `CouponRepository` — 新增 `findByUserId(String, PageRequest)`, `findAvailableByUserId(String, PageRequest)`
- `OrderRepository` — 新增 `findByCustomerId(CustomerId, PageRequest)`

### application（新增）
- `com.claudej.application.common.dto.PageDTO` — 分页 DTO（泛型）
- `com.claudej.application.common.assembler.PageAssembler` — Page<T> ↔ PageDTO<T> 转换器

### application（修改）
- `LinkApplicationService` — 新增 `getAllLinks(PageRequest)`, `getLinksByCategory(String, PageRequest)`
- `UserApplicationService` — 新增 `getInvitedUsers(String, PageRequest)`
- `CouponApplicationService` — 新增 `getCouponsByUserId(String, PageRequest)`, `getAvailableCouponsByUserId(String, PageRequest)`
- `OrderApplicationService` — 新增 `getOrdersByCustomerId(String, PageRequest)`
- `UserOrderQueryService` — 新增 `getUserOrders(String, PageRequest)` 分页方法

### infrastructure（新增）
- `com.claudej.infrastructure.common.persistence.PageHelper` — MyBatis-Plus Page ↔ Domain Page 转换工具

### infrastructure（修改）
- `LinkRepositoryImpl` — 实现新增的分页方法
- `UserRepositoryImpl` — 实现新增的分页方法
- `CouponRepositoryImpl` — 实现新增的分页方法
- `OrderRepositoryImpl` — 实现新增的分页方法

### adapter（新增）
- `com.claudej.adapter.common.response.PageResponse` — 分页响应对象
- `com.claudej.adapter.common.request.PageRequest` — 分页请求参数对象（可复用 domain 的值对象概念，但 Adapter 层需要独立定义以保持分层边界）

### adapter（修改）
- `LinkController` — 改造 `getAllLinks()`, `getLinksByCategory()`
- `UserController` — 改造 `getInvitedUsers()`
- `CouponController` — 改造 `getCouponsByUserId()`, `getAvailableCouponsByUserId()`
- `OrderController` — 改造 `getOrdersByCustomerId()`
- `UserOrderController` — 改造 `getUserOrders()`

### start
- 无变更（无新表、无新配置）

## 验收条件

1. 所有列表接口支持分页参数（page/size/sortField/sortDirection）
2. 分页参数校验生效（page>=0, size 1-100）
3. 排序字段白名单校验生效（非法字段返回 400）
4. 分页响应结构符合规范（content + totalElements + totalPages + page + size + first + last + empty）
5. 原有无参列表接口保持向后兼容（不删除，返回默认分页结果）
6. Domain 层 PageRequest/Page 值对象无 Spring 依赖
7. ArchUnit 架构规则全部通过
8. 所有测试用例通过

## 架构评审

**评审人**：@architect
**日期**：2026-04-23
**结论**：通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则） — 本任务不新增聚合根，为现有聚合增加分页能力，不影响聚合边界
- [x] 值对象识别充分（金额、标识符等应为 VO） — PageRequest、SortDirection、Page<T> 作为通用值对象放在 domain/common/model/valobj 下，符合跨聚合复用需求
- [x] Repository 端口粒度合适（方法不多不少） — 新增分页方法而不替换原有方法，保持向后兼容，设计合理
- [x] 与已有聚合无循环依赖 — 分页值对象位于 domain/common，不依赖任何聚合，无循环依赖风险
- [x] DDL 设计与领域模型一致（字段映射、索引合理） — 无 DDL 变更，现有索引覆盖分页查询场景
- [x] API 设计符合 RESTful 规范 — 接口路径保持 `/api/v1/{resource}` 格式，响应统一使用 `ApiResult<PageResponse<T>>`
- [x] 对象转换链正确（DO <-> Domain <-> DTO <-> Request/Response） — 设计了完整的转换链：PageHelper(infra) -> PageAssembler(application) -> PageResponse(adapter)

**需求质量（3 项）**
- [x] 需求无歧义：核心名词（page/size/sortField/sortDirection）、流程、异常分支均有明确定义
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例
- [x] 业务规则完备：分页参数校验规则（page>=0, size 1-100）、排序字段白名单、默认值均已列明

**计划可执行性（2 项）**
- [x] task-plan 粇度合格：按层任务已分解到原子级（10-15 分钟/步），每步含文件路径 + 验证命令 + 预期输出
- [x] 依赖顺序正确：domain -> application -> infrastructure -> adapter 自下而上，层间依赖无倒置

**可测性保障（3 项）**
- [x] AC 自动化全覆盖：task-plan 每个原子任务都有对应测试骨架，无标「手动」项
- [x] 可测的注入方式：分页值对象为纯 Java（无 Spring Bean），无需依赖注入，测试无需反射
- [x] 配置校验方式合规：无涉及敏感/跨环境配置校验

**心智原则（Karpathy — 动手前自检）**
- [x] 简洁性：设计仅引入必要的分页值对象，无过度抽象（无 XxxFactory/XxxStrategy）
- [x] 外科性：改动仅涉及分页相关的 Repository/Service/Controller，不涉及其他聚合核心逻辑
- [x] 假设显性：「假设与待确认」章节已列出向后兼容、默认每页条数等假设

### 评审意见

**设计亮点**：
1. **值对象位置合理**：将 PageRequest/Page/SortDirection 放在 `domain/common/model/valobj` 下，而非特定聚合包，符合"分页是跨聚合通用能力"的设计意图
2. **向后兼容设计**：新增分页方法而非替换原有方法，保留无参 `findAll()` 等接口，避免破坏现有调用方
3. **排序字段白名单**：各聚合独立定义允许排序的字段，防止 SQL 注入，安全考量到位
4. **转换链完整**：从 MyBatis-Plus IPage -> Domain Page -> Application PageDTO -> Adapter PageResponse，分层边界清晰

**建议优化（Build 阶段可采纳）**：
1. 设计文档中提到 "Adapter 层需要独立定义 PageRequest 以保持分层边界"，表述略有歧义。实际上 Adapter 层应直接接收 HTTP Query 参数（`@RequestParam`），在 Controller 内构建 domain 层的 `PageRequest` 值对象。无需在 Adapter 层再定义同名类。建议 task-plan 中明确这一点。
2. 排序字段白名单校验可考虑统一封装为 `SortFieldValidator`（放在 application 或 infrastructure 层），避免每个 Controller 重复校验逻辑。

### entropy-check 基线确认

```bash
./scripts/entropy-check.sh
# 退出码: 0 (PASS)
# 错误 (FAIL): 0
# 警告 (WARN): 12（auth 聚合测试缺失等，与本任务无关）
```

### 需要新增的 ADR

本任务不涉及重大架构决策变更，无需新增 ADR。分页能力属于基础设施增强，现有架构约束已覆盖。