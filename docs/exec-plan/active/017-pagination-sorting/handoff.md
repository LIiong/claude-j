---
task-id: "017-pagination-sorting"
from: dev
to: architect
status: pending-review
timestamp: "2026-04-23T15:13:56Z"
pre-flight:
  mvn-test: pending
  checkstyle: pending
  entropy-check: pending
artifacts:
  - requirement-design.md
  - task-plan.md
summary: "为所有列表接口增加分页与排序支持。新增 PageRequest/Page/SortDirection 值对象（domain/common），改造 4 个 Repository 接口、5 个 ApplicationService、4 个 RepositoryImpl、5 个 Controller。无 DDL 变更。"
---