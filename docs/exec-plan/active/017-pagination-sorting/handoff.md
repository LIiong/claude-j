# Handoff — 017-pagination-sorting

## 基本信息
- **task-id**: 017-pagination-sorting
- **from**: dev
- **to**: qa
- **status**: pending-review
- **date**: 2026-04-23

## 三项预飞（本消息实际运行）

```yaml
pre-flight:
  mvn-test: pass       # Tests: 95 run, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS
  checkstyle: pass     # 0 Checkstyle violations, BUILD SUCCESS
  entropy-check: pass  # issues: 0, warnings: 12, status: PASS
```

## 修复摘要

### Critical #1 — 路由冲突修复
- **修复方案**：将分页端点路径从 `/paged` 改为 `/query/paged`
- **变更文件**：`LinkController.java`
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=LinkControllerTest`
- **验证结果**：14 tests pass

### Major #2 — 排序字段白名单校验
- **修复方案**：
  - 在 `LinkController` 添加 `ALLOWED_SORT_FIELDS` 常量
  - 添加 `validateSortField()` 方法校验排序字段
  - 在 `GlobalExceptionHandler` 添加 `INVALID_SORT_FIELD` 返回400 状态码
- **变更文件**：`LinkController.java`, `GlobalExceptionHandler.java`
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=LinkControllerTest#should_return400_when_sortFieldNotInWhitelist`
- **验证结果**：pass（返回 400 + errorCode: INVALID_SORT_FIELD）

### Major #3 — 测试覆盖补充
- **Application 层**：新增 2 个分页方法测试
  - `should_returnPagedResult_when_getAllLinksPaged`
  - `should_returnPagedResultByCategory_when_getLinksByCategoryPaged`
- **Adapter 层**：新增 4 个分页端点测试
  - `should_return200WithPagedResult_when_getAllLinksPaged`
  - `should_return200WithPagedResult_when_getLinksByCategoryPaged`
  - `should_return400_when_sortFieldNotInWhitelist`
  - `should_return200_when_sortFieldInWhitelist`
- **验证命令**：`mvn test`
- **验证结果**：95 tests pass（新增6 tests）

## 变更记录

### 路由路径变更
- `/api/v1/links/paged` → `/api/v1/links/query/paged`
- `/api/v1/links/category/paged` 保持不变（无冲突）

### 新增功能
- 排序字段白名单校验（createTime, updateTime, name）
- 非法排序字段返回 400 + INVALID_SORT_FIELD

## 交付物

- 修复代码已提交（commit 待生成）
- dev-log.md 已更新修复记录

请 @qa 重新验收。