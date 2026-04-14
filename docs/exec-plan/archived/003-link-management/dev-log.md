# 开发日志 — 003-link-management

## 2026-04-13

### 今日完成
- [x] Domain层实现：Link聚合根、3个值对象、Repository端口
- [x] Application层实现：3个Command、DTO、Assembler、Service
- [x] Infrastructure层实现：DO、Mapper、Converter、RepositoryImpl
- [x] Adapter层实现：Request/Response、Controller
- [x] 数据库：schema.sql添加t_link表
- [x] 全局：ErrorCode扩展、GlobalExceptionHandler扩展

### 问题与决策

#### 问题1: application模块命令对象的验证注解
**现象**: 最初在CreateLinkCommand等类中使用了javax.validation注解，编译报错找不到包。
**原因**: application模块依赖中未引入validation-api，且根据已有代码规范，命令对象校验应在adapter层处理。
**解决**: 移除application模块命令对象中的javax.validation注解，保留在adapter层的Request对象中。

#### 问题2: ApiResult.ok()无参调用失败
**现象**: LinkController中删除接口调用`ApiResult.ok()`无参版本编译失败。
**原因**: ApiResult.ok()方法需要传入T类型参数。
**解决**: 改为`ApiResult.ok(null)`。

#### 问题3: LinkRepositoryImplTest表不存在
**现象**: Infrastructure层测试报错，表"T_LINK"不存在。
**原因**: infrastructure模块测试资源中的schema.sql未包含t_link表定义。
**解决**: 在claude-j-infrastructure/src/test/resources/db/schema.sql中添加t_link表DDL。

#### 问题4: 软删除实现方式选择
**决策**: 对比手动实现（update deleted字段）vs MyBatis-Plus逻辑删除（@TableLogic）
- 选择：使用MyBatis-Plus @TableLogic注解
- 理由：简化代码，自动过滤已删除记录，与已有shortlink实现保持一致风格

### 技术债务
- 无

### 下次工作
- 任务已完成，准备归档
