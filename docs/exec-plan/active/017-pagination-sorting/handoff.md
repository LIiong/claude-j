# Handoff — 017-pagination-sorting

## 基本信息
- **task-id**: 017-pagination-sorting
- **from**: qa
- **to**: dev
- **status**: changes-requested
- **date**: 2026-04-23

## QA 验收结果

### 三项预飞（独立重跑）
```yaml
pre-flight:
  mvn-test: pass       # Tests: 52 run, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS
  checkstyle: pass     # 0 Checkstyle violations, BUILD SUCCESS
  entropy-check: pass  # issues: 0, warnings: 12, status: PASS
```

### 验收结论：❌ 待修复

**问题清单**：

| # | 严重度 | 描述 | 建议 |
|---|--------|------|------|
| 1 | **Critical** | **LinkController 路由路径冲突**：`GET /api/v1/links/paged` 被 Spring MVC 匹配到 `GET /api/v1/links/{id}`（`{id}` 为 Long 类型），导致 `NumberFormatException: For input string: "paged"`。分页接口完全不可用。 | (1) 将 `/paged` 改为 `/api/v1/links-all/paged` 或 `/api/v1/links/query/paged`；(2) 或在原 `GET /api/v1/links` 上增加 Query 参数支持分页；(3) 或将 `{id}` 改为 String 类型并在 Service 层校验。 |
| 2 | **Major** | **排序字段白名单校验缺失**：requirement-design.md 定义了排序字段白名单（link: createTime, updateTime, name），ErrorCode 定义了 `INVALID_SORT_FIELD`，但 Controller 层无校验逻辑。 | 在 Controller 或 Application 层增加白名单校验。 |
| 3 | **Major** | **测试覆盖不足**：Application/Infrastructure/Adapter 层均无新增测试类，仅 Domain 层有测试。集成测试因路由冲突无法执行。 | 新增各层分页方法测试。 |

### 测试覆盖情况
- ✅ Domain 层：49 tests（PageRequest 19 + SortDirection 9 + Page 21）
- ⚠️ Application 层：无新增测试
- ⚠️ Infrastructure 层：无新增测试
- ⚠️ Adapter 层：无新增测试
- ❌ 集成测试：因路由冲突无法执行

### 架构合规
- ✅ 依赖方向正确（adapter → application → domain ← infrastructure）
- ✅ Domain 层无 Spring/框架 import
- ✅ 值对象不可变（final class + final fields）
- ✅ Checkstyle 通过
- ✅ ArchUnit 14 条规则通过

---

## 修复建议（优先级排序）

### P0 — Critical 路由冲突（必须修复）

**问题**：LinkController 的 `/{id}`（Long 类型）与 `/paged` 路径冲突。

**推荐方案**（任选其一）：
1. **路径重构**：将 `/paged` 改为 `/api/v1/links/query/paged` 或类似路径，与 `/{id}` 区分
2. **Query 参数方案**：在原有 `GET /api/v1/links` 上增加可选 Query 参数 `page/size/sortField/sortDirection`，不新增 `/paged` 路径
3. **类型修改**：将 `{id}` 改为 String 类型，在 Service 层校验是否为数字

### P1 — Major 排序白名单（必须修复）

**问题**：sortField 无白名单校验，可能导致 SQL 注入。

**建议**：
- 在 Controller 层增加校验逻辑，或
- 在 Application 层增加统一的 `SortFieldValidator`

### P2 — Major 测试覆盖（必须修复）

**建议**：
- 新增 `LinkApplicationServiceTest` 分页方法测试
- 新增 `LinkRepositoryImplIT` 分页方法测试
- 新增 `LinkControllerTest` 分页端点测试（MockMvc）
- 修复路由后新增 `PaginationIntegrationTest`

---

## 开发摘要（原 from:dev）

### 变更范围
- **Domain 层**：新增 `domain/common/model/valobj` 下三个值对象
  - `PageRequest` — 分页请求参数（page/size/sortField/sortDirection）
  - `SortDirection` — 排序方向枚举（ASC/DESC）
  - `Page<T>` — 分页结果值对象（content/totalElements/totalPages/first/last/empty）
- **Application 层**：新增 `application/common` 下 DTO 与转换器
  - `PageDTO<T>` — 分页 DTO
  - `PageAssembler` — Page<T> 转 PageDTO<T> 转换器
  - ApplicationService 新增分页方法（5 个 Service）
- **Infrastructure 层**：新增 `PageHelper` 工具类 + RepositoryImpl 分页方法实现（4 个）
- **Adapter 层**：新增 `PageResponse` + Controller 分页端点（5 个 Controller）

### Commits
- `af1329f` feat(domain): 分页请求与结果值对象 PageRequest/SortDirection/Page<T>
- `a55bb2c` feat(domain): Repository 接口新增分页方法
- `d8db222` feat(application): 分页 DTO 与 Assembler
- `e466dcc` feat(application): ApplicationService 新增分页方法
- `bc1bab2` feat(adapter): Controller 新增分页端点

---

## 交付物

- test-case-design.md：测试用例设计（七节完整）
- test-report.md：测试报告（含问题清单）

修复完成后请通知 @qa 重新验收。