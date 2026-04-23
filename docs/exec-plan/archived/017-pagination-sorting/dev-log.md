# 开发日志 — 017-pagination-sorting

## 问题记录

### 1. Java 8 不支持 import as 语法
- **Issue**：PageHelper 使用 `import Page as MybatisPlusPage` 导致编译错误
- **Root Cause**：Java 8 不支持 Kotlin 风格的 import alias
- **Fix**：移除 alias，改用全限定名 `com.baomidou.mybatisplus.extension.plugins.pagination.Page`
- **Verification**：`mvn compile` → `BUILD SUCCESS`

### 2. PageDTO boolean 字段 getter 方法名
- **Issue**：Adapter 层使用 `pageDTO.getFirst()` 报找不到符号
- **Root Cause**：Lombok 对 boolean 字段生成的是 `isFirst()` 而非 `getFirst()`
- **Fix**：改用 `pageDTO.isFirst()` 等正确的方法名
- **Verification**：`mvn compile` → `BUILD SUCCESS`

### 3. LinkController 路由路径冲突（QA 打回 - Critical）
- **Issue**：`GET /api/v1/links/paged` 被 `GET /api/v1/links/{id}`（Long 类型）拦截，导致 `NumberFormatException: For input string: "paged"`
- **Root Cause**：Spring MVC 匹配顺序导致 `/paged` 被 `{id}` 路径参数匹配
- **Fix**：将分页端点路径改为 `/api/v1/links/query/paged`，避免与 `{id}` 冲突
- **Verification**：`mvn test -pl claude-j-adapter -Dtest=LinkControllerTest` → 14 tests pass

### 4. 排序字段白名单校验缺失（QA 打回 - Major）
- **Issue**：ErrorCode 已定义 `INVALID_SORT_FIELD`，但 Controller 无校验，非法字段可能导致 SQL 注入
- **Root Cause**：开发阶段遗漏排序字段白名单校验逻辑
- **Fix**：
  - 在 LinkController 添加 `ALLOWED_SORT_FIELDS` 白名单常量（createTime, updateTime, name）
  - 添加 `validateSortField()` 方法校验排序字段
  - 在 GlobalExceptionHandler 添加 `INVALID_SORT_FIELD` 返回400 状态码
- **Verification**：`mvn test -pl claude-j-adapter -Dtest=LinkControllerTest#should_return400_when_sortFieldNotInWhitelist` → pass

### 5. 测试覆盖不足（QA 打回 - Major）
- **Issue**：Application/Infrastructure/Adapter 层无新增分页方法测试
- **Root Cause**：开发阶段仅添加 Domain 层测试
- **Fix**：
  - Application 层：添加 `should_returnPagedResult_when_getAllLinksPaged` 和 `should_returnPagedResultByCategory_when_getLinksByCategoryPaged` 测试
  - Adapter 层：添加 4 个分页端点测试（路由可访问、白名单校验、合法字段）
- **Verification**：`mvn test` → 95 tests pass

## 变更记录

- **路由路径变更**：`/api/v1/links/paged` → `/api/v1/links/query/paged`（避免与 `{id}` 冲突）
- **新增功能**：排序字段白名单校验（createTime, updateTime, name）

## 开发摘要

- **修复 commit**：1 commit（fix: 路由冲突 + 排序字段白名单 + 测试补充）
- **测试数量**：全量 95 tests（新增 Adapter 4 tests + Application 2 tests）
- **三项验证**：mvn test (95 tests pass) + checkstyle (0 violations) + entropy-check (0 issues, 12 warnings)
