# 测试用例设计 — 017-pagination-sorting

## 测试范围
验证分页功能的完整实现，包括分页参数校验、分页响应结构、排序方向解析、向后兼容性，以及架构规则合规性。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | 所有列表接口支持分页参数（page/size/sortField/sortDirection） | Adapter | `LinkControllerTest` (分页端点测试) | - |
| AC2 | 分页参数校验生效（page>=0, size 1-100） | Domain | `PageRequestTest.should_throwException_when_pageIsNegative` + `PageRequestTest.should_throwException_when_sizeExceedsMax` | - |
| AC3 | 排序字段白名单校验生效（非法字段返回 400） | — | **未实现** | ErrorCode 定义了 INVALID_SORT_FIELD 但 Controller 无校验逻辑 |
| AC4 | 分页响应结构符合规范（content + totalElements + totalPages + page + size + first + last + empty） | Start（集成） | `PaginationIntegrationTest.should_returnPagedResponse_when_requestValid` | - |
| AC5 | 原有无参列表接口保持向后兼容（不删除，返回默认分页结果） | Adapter | `LinkControllerTest` (原 getAllLinks 端点测试) | - |
| AC6 | Domain 层 PageRequest/Page 值对象无 Spring 依赖 | Domain + ArchUnit | `PageRequestTest` + ArchUnit 架构规则 14 条 | - |
| AC7 | ArchUnit 架构规则全部通过 | All | `mvn test` (ArchUnit) | - |
| AC8 | 所有测试用例通过 | All | `mvn clean test` | - |

**AC3 问题说明**：requirement-design.md 定义了排序字段白名单（link: createTime, updateTime, name），但 Controller 层直接透传 sortField 到 PageRequest，未做白名单校验。INVALID_SORT_FIELD ErrorCode 已定义但未使用。这是一个 **Major 级别缺陷**。

---

## 一、Domain 层测试场景

### PageRequest 值对象（已有 19 个测试）
| # | 场景 | 前置条件 | 操作 | 预期结果 | 状态 |
|---|------|----------|------|----------|------|
| D1 | 合法值创建 | - | new PageRequest(0, 20, "createTime", DESC) | 创建成功 | ✅ 已覆盖 |
| D2 | null 参数默认值 | - | PageRequest.of(null, null, null, null) | page=0, size=20, sortDirection=ASC | ✅ 已覆盖 |
| D3 | 页码负数校验 | - | new PageRequest(-1, 20, null, null) | 抛 BusinessException(PAGE_NUMBER_NEGATIVE) | ✅ 已覆盖 |
| D4 | size=0 校验 | - | new PageRequest(0, 0, null, null) | 抛 BusinessException(PAGE_SIZE_INVALID) | ✅ 已覆盖 |
| D5 | size=101 超限校验 | - | new PageRequest(0, 101, null, null) | 抛 BusinessException(PAGE_SIZE_INVALID) | ✅ 已覆盖 |
| D6 | size=100 边界值 | - | new PageRequest(0, 100, null, null) | 创建成功，size=100 | ✅ 已覆盖 |
| D7 | size=1 边界值 | - | new PageRequest(0, 1, null, null) | 创建成功，size=1 | ✅ 已覆盖 |
| D8 | sortField 空字符串处理 | - | new PageRequest(0, 20, "", null) | sortField=null | ✅ 已覆盖 |
| D9 | sortField trim 处理 | - | new PageRequest(0, 20, "  createTime  ", null) | sortField="createTime" | ✅ 已覆盖 |
| D10 | 偏移量计算 | - | page=2, size=10 | getOffset() = 20 | ✅ 已覆盖 |
| D11 | 相等性校验 | - | 两个相同参数的 PageRequest | equals=true, hashCode 相同 | ✅ 已覆盖 |

### SortDirection 枚举（已有 9 个测试）
| # | 场景 | 前置条件 | 操作 | 预期结果 | 状态 |
|---|------|----------|------|----------|------|
| D12 | ASC/DESC 值存在 | - | SortDirection.values() | 长度=2 | ✅ 已覆盖 |
| D13 | 大写字符串解析 | - | SortDirection.fromString("ASC") | 返回 ASC | ✅ 已覆盖 |
| D14 | 小写字符串解析 | - | SortDirection.fromString("asc") | 返回 ASC | ✅ 已覆盖 |
| D15 | 混合大小写解析 | - | SortDirection.fromString("DeSc") | 返回 DESC | ✅ 已覆盖 |
| D16 | null 字符串解析 | - | SortDirection.fromString(null) | 返回 ASC（默认） | ✅ 已覆盖 |
| D17 | 无效字符串解析 | - | SortDirection.fromString("invalid") | 返回 ASC（默认） | ✅ 已覆盖 |

### Page<T> 值对象（已有 21 个测试）
| # | 场景 | 前置条件 | 操作 | 预期结果 | 状态 |
|---|------|----------|------|----------|------|
| D18 | 分页结果创建 | content.size=2, total=100 | new Page(...) | 创建成功 | ✅ 已覆盖 |
| D19 | 总页数计算（整除） | total=100, size=20 | totalPages=5 | ✅ 已覆盖 |
| D20 | 总页数计算（有余） | total=105, size=20 | totalPages=6 | ✅ 已覆盖 |
| D21 | 首页标识 | page=0 | isFirst=true | ✅ 已覆盖 |
| D22 | 末页标识 | page=totalPages-1 | isLast=true | ✅ 已覆盖 |
| D23 | 空页标识 | content.isEmpty() | isEmpty=true | ✅ 已覆盖 |
| D24 | hasNext/hasPrevious | page=1 (非首非末) | hasNext=true, hasPrevious=true | ✅ 已覆盖 |
| D25 | 空分页创建 | - | Page.empty(0, 20) | 空页，isFirst=true, isLast=true | ✅ 已覆盖 |

---

## 二、Application 层测试场景

### LinkApplicationService 分页方法（需新增测试）
| # | 场景 | 置条件 | 操作 | 预期结果 | 状态 |
|---|------|----------|------|----------|------|
| A1 | 分页查询正常返回 | Mock repo 返回 Page<Link> | getAllLinks(pageRequest) | 返回 PageDTO<LinkDTO> | ✅ 已覆盖 |
| A2 | 分页查询编排验证 | Mock repo | getAllLinks(pageRequest) | verify(repo).findAll(pageRequest) | ✅ 已覆盖 |
| A3 | 分类分页查询 | Mock repo 返回 Page<Link> | getLinksByCategory(category, pageRequest) | 返回 PageDTO | ✅ 已覆盖 |

---

## 三、Infrastructure 层测试场景

### LinkRepositoryImpl 分页实现（需新增测试）
| # | 场景 | 前置条件 | 操作 | 预期结果 | 状态 |
|---|------|----------|------|----------|------|
| I1 | 分页查询数据库 | H2 插入 30 条数据 | findAll(PageRequest(0, 10)) | content.size=10, total=30 | ✅ 已覆盖 |
| I2 | 分类分页查询 | H2 插入分类数据 | findByCategory(category, pageRequest) | 正确分类过滤 | ✅ 已覆盖 |
| I3 | MyBatis-Plus Page 转 Domain Page | - | PageHelper.toDomainPage(iPage) | 页码转换正确（MyBatis 1→Domain 0） | ✅ 已覆盖 |

---

## 四、Adapter 层测试场景

### LinkController 分页端点（需新增测试）
| # | 场景 | 置条件 | 操作 | 预期结果 | 状态 |
|---|------|----------|------|----------|------|
| W1 | 分页查询成功响应 | Mock service 返回 PageDTO | GET /api/v1/links/paged?page=0&size=10 | 200 + PageResponse 结构完整 | ✅ 已覆盖 |
| W2 | 无参数默认值 | Mock service | GET /api/v1/links/paged | PageRequest 使用默认值 | ✅ 已覆盖 |
| W3 | 排序参数传递 | Mock service | GET /api/v1/links/paged?sortField=createTime&sortDirection=DESC | 正确传递 sortField/Direction | ✅ 已覆盖 |
| W4 | 分类分页查询 | Mock service | GET /api/v1/links/category/paged?category=tech | 正确传递 category + 分页参数 | ✅ 已覆盖 |
| W5 | 向后兼容性验证 | Mock service 返回 List | GET /api/v1/links | 仍返回 List<LinkResponse>（非 PageResponse） | ✅ 已覆盖 |

---

## 五、集成测试场景（全链路）

### PaginationIntegrationTest（start 模块新增）
| # | 场景 | 操作 | 预期结果 | 状态 |
|---|------|------|----------|------|
| E1 | 分页查询全链路 | 创建 30 条 Link → GET /api/v1/links/paged?page=0&size=10 | content.size=10, totalElements=30, totalPages=3, first=true | ⏳ 待实现 |
| E2 | 分页边界验证 | 创建 30 条 Link → GET /api/v1/links/paged?page=2&size=10 | content.size=10, last=true | ⏳ 待实现 |
| E3 | 分类分页全链路 | 创建分类数据 → GET /api/v1/links/category/paged?category=tech&page=0&size=5 | 正确分类过滤 + 分页结构 | ⏳ 待实现 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import（PageRequest/Page/SortDirection 均为纯 Java）
- [x] 聚合根封装业务不变量（非贫血模型）— 本任务不新增聚合根
- [x] 值对象不可变，equals/hashCode 正确（PageRequest/Page 均为 final + @EqualsAndHashCode）
- [x] Repository 接口在 domain，实现在 infrastructure（LinkRepository 接口在 domain，实现在 infrastructure）
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response（PageHelper + PageAssembler + Controller 手动转换）
- [x] Controller 无业务逻辑（仅构建 PageRequest + 调用 service + 转换响应）
- [x] 异常通过 GlobalExceptionHandler 统一处理（BusinessException 已被捕获）
- [ ] **排序字段白名单校验** — 未实现（ErrorCode 定义了 INVALID_SORT_FIELD 但 Controller 未使用）

---

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [x] DO 用 @Data + @TableName，DTO 用 @Data
- [x] 命名规范：PageRequest, PageDTO, PageResponse, PageHelper, PageAssembler
- [x] 包结构符合 com.claudej.{layer}.common.model.valobj / dto / assembler / persistence
- [x] 测试命名 should_xxx_when_xxx（49 个 Domain 测试均符合）