# Handoff — 017-pagination-sorting

## 基本信息
- **task-id**: 017-pagination-sorting
- **from**: dev
- **to**: qa
- **status**: pending-review
- **date**: 2026-04-23

## Pre-flight 检查结果

```yaml
pre-flight:
  mvn-test: pass       # Tests: 52 run, 0 failures, 0 errors, 0 skipped
  checkstyle: pass     # 0 Checkstyle violations
  entropy-check: pass  # issues: 0, warnings: 12, status: PASS
```

## 开发摘要

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

### 测试覆盖
- Domain 值对象测试：49 个测试用例（SortDirection 9 + PageRequest 19 + Page 21）
- 全量测试：52 个测试通过（含 ArchUnit 14 条架构规则）

### Commits
- `af1329f` feat(domain): 分页请求与结果值对象 PageRequest/SortDirection/Page<T>
- `a55bb2c` feat(domain): Repository 接口新增分页方法
- `d8db222` feat(application): 分页 DTO 与 Assembler
- `e466dcc` feat(application): ApplicationService 新增分页方法
- `bc1bab2` feat(adapter): Controller 新增分页端点

## 待验收清单

1. 分页参数校验（page>=0, size 1-100）
2. 分页响应结构（content/totalElements/totalPages/page/size/first/last/empty）
3. 排序方向解析（ASC/DESC）
4. 向后兼容（原有列表接口保留）
5. ArchUnit 架构规则全部通过

## 验收命令

```bash
# 三项预飞
mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh

# 验证分页接口（示例）
curl "http://localhost:8080/api/v1/links/paged?page=0&size=10"
curl "http://localhost:8080/api/v1/links/category/paged?category=tech&page=0&size=20"
```