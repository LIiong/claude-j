# 任务计划 — 003-link-management

## 任务清单

### Domain 层
- [x] Link 聚合根
- [x] LinkName 值对象
- [x] LinkUrl 值对象
- [x] LinkCategory 值对象
- [x] LinkRepository 端口接口
- [x] ErrorCode 扩展（Link相关错误码）
- [x] Domain 层单元测试

### Application 层
- [x] CreateLinkCommand
- [x] UpdateLinkCommand
- [x] DeleteLinkCommand
- [x] LinkDTO
- [x] LinkAssembler (MapStruct)
- [x] LinkApplicationService
- [x] Application 层单元测试

### Infrastructure 层
- [x] LinkDO (MyBatis-Plus)
- [x] LinkMapper
- [x] LinkConverter
- [x] LinkRepositoryImpl
- [x] schema.sql 测试资源
- [x] Infrastructure 层集成测试

### Adapter 层
- [x] CreateLinkRequest
- [x] UpdateLinkRequest
- [x] LinkResponse
- [x] LinkController
- [x] GlobalExceptionHandler 扩展
- [x] Adapter 层单元测试

### Start 模块
- [x] schema.sql 添加 t_link 表

### 文档
- [x] requirement-design.md
- [x] task-plan.md
- [x] dev-log.md
- [x] handoff.md

## 状态
- **当前阶段**: Ship
- **完成度**: 100%
- **负责人**: @dev
