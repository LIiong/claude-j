# 测试用例设计 — 003-link-management

## 测试范围
链接管理功能全链路测试，包括Domain层值对象与聚合根、Application层服务编排、Infrastructure层持久化、Adapter层API接口。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 一、Domain 层测试场景

### LinkName 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new LinkName("Test Link") | 创建成功，value="Test Link" |
| D2 | 首尾空格自动去除 | - | new LinkName("  Test  ") | value="Test" |
| D3 | null值 | - | new LinkName(null) | 抛BusinessException(LINK_NAME_EMPTY) |
| D4 | 空字符串 | - | new LinkName("") | 抛BusinessException(LINK_NAME_EMPTY) |
| D5 | 空白字符 | - | new LinkName("   ") | 抛BusinessException(LINK_NAME_EMPTY) |
| D6 | 长度超过100 | - | new LinkName("a".repeat(101)) | 抛BusinessException(LINK_NAME_TOO_LONG) |
| D7 | 边界值100字符 | - | new LinkName("a".repeat(100)) | 创建成功 |
| D8 | 相等性 | - | 两个相同值对象 | equals返回true，hashCode相同 |

### LinkUrl 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D9 | 合法HTTP URL | - | new LinkUrl("http://example.com") | 创建成功 |
| D10 | 合法HTTPS URL | - | new LinkUrl("https://example.com") | 创建成功 |
| D11 | 合法FTP URL | - | new LinkUrl("ftp://files.example.com") | 创建成功 |
| D12 | 首尾空格自动去除 | - | new LinkUrl("  https://example.com  ") | value="https://example.com" |
| D13 | null值 | - | new LinkUrl(null) | 抛BusinessException(LINK_URL_EMPTY) |
| D14 | 空字符串 | - | new LinkUrl("") | 抛BusinessException(LINK_URL_EMPTY) |
| D15 | 无效URL格式 | - | new LinkUrl("not-a-url") | 抛BusinessException(LINK_URL_INVALID) |
| D16 | 长度超过500 | - | new LinkUrl("https://"+"a".repeat(500)) | 抛BusinessException(LINK_URL_TOO_LONG) |
| D17 | 相等性 | - | 两个相同URL | equals返回true |

### LinkCategory 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D18 | 合法值 | - | new LinkCategory("tech") | value="tech" |
| D19 | null值转为default | - | new LinkCategory(null) | value="default" |
| D20 | 空字符串转为default | - | new LinkCategory("") | value="default" |
| D21 | 自动转小写 | - | new LinkCategory("TECH") | value="tech" |
| D22 | 首尾空格去除 | - | new LinkCategory("  tech  ") | value="tech" |

### Link 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D23 | 创建链接 | 有效name/url | Link.create(name,url,desc,category) | 创建成功，createTime/updateTime不为空 |
| D24 | 创建时name为null | - | Link.create(null,url,...) | 抛BusinessException(LINK_NAME_EMPTY) |
| D25 | 创建时url为null | - | Link.create(name,null,...) | 抛BusinessException(LINK_URL_EMPTY) |
| D26 | 更新链接 | 已创建链接 | link.update(newName,newUrl,...) | 字段更新，updateTime刷新 |
| D27 | 部分更新 | 已创建链接 | link.update(newName,null,null,null) | 仅name更新，其他不变 |
| D28 | 从持久化重建 | - | Link.reconstruct(...) | 完整还原所有字段 |
| D29 | setId回填 | 已创建链接 | link.setId(100L) | id=100L |
| D30 | getXxxValue便捷方法 | 已创建链接 | link.getNameValue() | 返回字符串值 |

---

## 二、Application 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 创建链接 | 有效command | createLink(cmd) | 调用repository.save，返回DTO |
| A2 | 创建时category为null | - | command.setCategory(null) | category设为null，不抛异常 |
| A3 | 更新链接 | 链接存在 | updateLink(cmd) | 先findById，再save，返回DTO |
| A4 | 更新不存在链接 | 链接不存在 | updateLink(cmd) | 抛BusinessException(LINK_NOT_FOUND) |
| A5 | 删除链接 | 链接存在 | deleteLink(cmd) | 调用existsById确认，再deleteById |
| A6 | 删除不存在链接 | 链接不存在 | deleteLink(cmd) | 抛BusinessException(LINK_NOT_FOUND)，不调用deleteById |
| A7 | 根据ID查询 | 链接存在 | getLinkById(id) | 返回DTO |
| A8 | 根据ID查询不存在 | 链接不存在 | getLinkById(id) | 抛BusinessException(LINK_NOT_FOUND) |
| A9 | 查询所有链接 | 多条记录存在 | getAllLinks() | 返回所有DTO列表 |
| A10 | 按分类查询 | 该分类有记录 | getLinksByCategory("tech") | 返回该分类DTO列表 |

---

## 三、Infrastructure 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存新链接 | link无id | save(link) | 插入记录，id自动回填 |
| I2 | 更新链接 | link有id | save(link) | 更新记录，updateTime刷新 |
| I3 | 根据ID查询 | 记录存在 | findById(id) | 返回Optional有值 |
| I4 | 根据ID查询不存在 | - | findById(9999L) | 返回Optional.empty |
| I5 | 软删除 | 记录存在 | deleteById(id) | deleted=1，逻辑删除 |
| I6 | 查询不到已删除 | 记录已软删除 | findById(id) | 返回Optional.empty |
| I7 | 查询所有 | 多条记录 | findAll() | 返回未删除记录列表 |
| I8 | 按分类查询 | 该分类有记录 | findByCategory(category) | 返回该分类未删除记录 |
| I9 | 检查存在 | 记录存在 | existsById(id) | 返回true |
| I10 | 检查不存在 | - | existsById(9999L) | 返回false |
| I11 | DO与Domain转换 | - | converter.toDO/toDomain | 字段完整映射 |

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 创建链接成功 | - | POST /api/v1/links | 200 + success=true + data有id |
| W2 | 创建链接参数校验失败 | name为空 | POST /api/v1/links | 400 Bad Request |
| W3 | 创建链接URL过长 | url>500字符 | POST /api/v1/links | 400 Bad Request |
| W4 | 更新链接成功 | - | PUT /api/v1/links/1 | 200 + success=true |
| W5 | 更新不存在链接 | Mock抛异常 | PUT /api/v1/links/999 | 404 Not Found |
| W6 | 删除链接成功 | - | DELETE /api/v1/links/1 | 200 + success=true |
| W7 | 删除不存在链接 | Mock抛异常 | DELETE /api/v1/links/999 | 404 Not Found |
| W8 | 根据ID查询成功 | - | GET /api/v1/links/1 | 200 + data完整 |
| W9 | 根据ID查询不存在 | Mock抛异常 | GET /api/v1/links/999 | 404 Not Found |
| W10 | 查询所有链接 | - | GET /api/v1/links | 200 + data数组 |
| W11 | 按分类查询 | - | GET /api/v1/links/category?category=tech | 200 + data数组 |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 完整CRUD流程 | 创建 → 查询 → 更新 → 删除 → 查询 | 各步骤数据一致，删除后查不到 |
| E2 | 软删除验证 | 创建 → 删除 → 查询所有 | 查询结果不包含已删除记录 |
| E3 | 分类查询 | 创建2条不同分类 → 按分类查询 | 返回对应分类的记录 |
| E4 | URL格式校验 | 创建时传无效URL | 400错误，不创建记录 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import
- [x] 聚合根封装业务不变量（非贫血模型）
- [x] 值对象不可变，equals/hashCode 正确
- [x] Repository 接口在 domain，实现在 infrastructure
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response
- [x] Controller 无业务逻辑
- [x] 异常通过 GlobalExceptionHandler 统一处理
- [x] 使用@Transactional在应用服务层
- [x] MyBatis-Plus逻辑删除配置正确（@TableLogic）

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [x] DO 用 @Data + @TableName，DTO 用 @Data
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [x] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [x] 测试命名 should_xxx_when_xxx
