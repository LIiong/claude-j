# 需求拆分设计 — {task-id}-{task-name}

## 需求描述
<!-- 用 1-3 句话概括本次需求的核心目标 -->


## 领域分析

### 聚合根: {AggregateName}
<!-- 列出聚合根的核心属性和值对象 -->
- field1 (ValueObject1) — 说明
- field2 (ValueObject2) — 说明

### 值对象
<!-- 每个值对象单独列出，说明约束条件 -->
- **{ValueObject1}**: 约束说明，不可变
- **{ValueObject2}**: 约束说明，不可变

### 领域服务（如有）
<!-- 无法归属到单一聚合的业务逻辑 -->

### 端口接口
<!-- Repository 和其他需要 infrastructure 实现的接口 -->
- **{Aggregate}Repository**: 方法列表
- **{DomainService}**: 方法列表

## 关键算法/技术方案
<!-- 核心算法设计、技术选型理由、性能考量 -->


## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/xxx | 创建 | `{ "field": "value" }` | `{ "success": true, "data": {...} }` |
| GET | /api/v1/xxx/{id} | 查询 | — | `{ "success": true, "data": {...} }` |

## 数据库设计（如有）
<!-- DDL、索引策略、数据量预估 -->
```sql
CREATE TABLE IF NOT EXISTS t_xxx (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- 字段定义
);
```

## 影响范围
<!-- 按 DDD 分层列出新增/修改的类 -->
- **domain**:
- **application**:
- **infrastructure**:
- **adapter**:
- **start**:
