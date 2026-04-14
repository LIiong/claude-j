# 需求拆分设计 — 003-link-management

## 需求描述
实现链接管理功能，支持链接的新增、修改、删除、查询。用于管理系统中的外部链接资源，支持按分类组织。

## 领域分析

### 聚合根: Link
- id (Long) — 数据库自增ID
- name (LinkName) — 链接名称，必填，最大100字符
- url (LinkUrl) — 链接地址，必填，最大500字符，需符合URL格式
- description (String) — 链接描述，可选，最大500字符
- category (LinkCategory) — 分类，可选，默认"default"
- createTime (LocalDateTime) — 创建时间
- updateTime (LocalDateTime) — 更新时间

### 值对象
- **LinkName**: 链接名称，必填，1-100字符，自动去除首尾空格
- **LinkUrl**: 链接地址，必填，支持http/https/ftp协议，最大500字符，自动去除首尾空格
- **LinkCategory**: 分类，可选，默认"default"，最大50字符，自动转小写

### 领域服务（如有）
无

### 端口接口
- **LinkRepository**:
  - `Link save(Link link)` — 保存或更新链接
  - `Optional<Link> findById(Long id)` — 根据ID查询
  - `void deleteById(Long id)` — 删除链接（软删除）
  - `List<Link> findAll()` — 查询所有链接
  - `List<Link> findByCategory(LinkCategory category)` — 根据分类查询
  - `boolean existsById(Long id)` — 检查链接是否存在

## 关键算法/技术方案
- 使用 MyBatis-Plus 逻辑删除（@TableLogic）实现软删除
- URL 格式校验使用正则表达式: `^(https?|ftp)://[^\s/$.?#].[^\s]*$`
- 分层测试策略：Domain纯单元测试、Application Mock测试、Infrastructure集成测试、Adapter @WebMvcTest

## API 设计

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| POST | /api/v1/links | 创建链接 | `{ "name": "", "url": "", "description": "", "category": "" }` | `{ "success": true, "data": {...} }` |
| PUT | /api/v1/links/{id} | 更新链接 | `{ "name": "", "url": "", "description": "", "category": "" }` | `{ "success": true, "data": {...} }` |
| DELETE | /api/v1/links/{id} | 删除链接 | — | `{ "success": true }` |
| GET | /api/v1/links | 查询所有链接 | — | `{ "success": true, "data": [...] }` |
| GET | /api/v1/links/{id} | 根据ID查询 | — | `{ "success": true, "data": {...} }` |
| GET | /api/v1/links/category?category=xxx | 根据分类查询 | — | `{ "success": true, "data": [...] }` |

## 数据库设计

```sql
CREATE TABLE IF NOT EXISTS t_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '链接名称',
    url VARCHAR(500) NOT NULL COMMENT '链接地址',
    description VARCHAR(500) COMMENT '链接描述',
    category VARCHAR(50) COMMENT '分类',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_category (category)
);
```

## 影响范围
- **domain**: Link、LinkName、LinkUrl、LinkCategory、LinkRepository、ErrorCode
- **application**: CreateLinkCommand、UpdateLinkCommand、DeleteLinkCommand、LinkDTO、LinkAssembler、LinkApplicationService
- **infrastructure**: LinkDO、LinkMapper、LinkConverter、LinkRepositoryImpl
- **adapter**: CreateLinkRequest、UpdateLinkRequest、LinkResponse、LinkController、GlobalExceptionHandler
- **start**: schema.sql (添加t_link表)

## 架构评审

### 评审记录
- **评审人**: @dev
- **评审时间**: 2026-04-13
- **评审结果**: ✅ 通过
- **评审意见**:
  - 设计符合DDD分层架构，依赖方向正确
  - 值对象设计合理，封装了业务约束
  - 使用MyBatis-Plus逻辑删除简化软删除实现
  - API设计符合RESTful规范
  - 测试覆盖Domain、Application、Infrastructure、Adapter四层
