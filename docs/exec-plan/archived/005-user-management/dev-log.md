# 开发日志 — 005-user-management

## 问题记录

### 1. 邀请码格式设计
- **问题**：需要设计一个既能保证唯一性又便于用户输入的邀请码格式
- **决策**：采用6位Base32编码（字符集：23456789ABCDEFGHJKLMNPQRSTUVWXYZ）
- **原因**：
  - 排除易混淆字符（0/O, 1/I/l）减少输入错误
  - 6位长度提供约10亿种组合，足够唯一性
  - 全大写便于用户输入

### 2. 跨聚合查询方式
- **问题**：UserOrderQueryService 如何查询订单数据
- **决策**：通过 OrderApplicationService 查询，不直接访问 OrderRepository
- **原因**：
  - 遵循 DDD 跨聚合通信原则
  - 保持聚合边界清晰
  - 避免 domain 层依赖复杂化

### 3. UserId 格式设计
- **问题**：需要设计用户唯一标识格式
- **决策**：UR + 16位随机字母数字（Base32字符集）
- **原因**：
  - UR 前缀标识 User Resource
  - 18位总长度与 OrderId 等保持一致
  - 使用 Base32 排除易混淆字符

### 4. 值对象校验时机
- **问题**：Email 和 Phone 是可选字段，如何校验
- **决策**：在值对象构造时严格校验，但允许 null
- **原因**：
  - 值对象一旦创建就保证有效性
  - 可选字段使用 null 表示未设置
  - Application 层根据业务需求决定是否设置

### 5. 邀请码生成冲突处理
- **问题**：随机生成邀请码可能出现冲突
- **决策**：循环生成 + 数据库唯一索引兜底
- **原因**：
  - 冲突概率低，循环生成足够
  - 设置最大重试次数（100次）防止无限循环
  - 数据库唯一索引作为最终保障

### 6. Java 8 兼容性问题
- **问题**：测试代码中使用了 Java 11+ 特性（如 `String.repeat()`）
- **决策**：替换为 Java 8 兼容写法
- **原因**：
  - 项目要求 Java 8 兼容性
  - 使用 `StringBuilder` 循环构建替代 `repeat()`

### 7. GlobalExceptionHandler 扩展
- **问题**：新增 USER_NOT_FOUND 错误码需要返回 404
- **决策**：在 GlobalExceptionHandler 中添加 USER_NOT_FOUND 到 404 的映射
- **原因**：
  - 保持异常处理一致性
  - 符合 RESTful API 设计规范

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->

- **无与原设计不一致的变更**。所有实现均按照需求设计文档执行。

## 迭代记录

### 迭代 1 (2026-04-13)
- **完成内容**：
  - Domain 层：6个值对象 + User 聚合根 + 端口 + 单元测试 (108 tests)
  - Application 层：Command/DTO/Assembler + Service + 单元测试 (16 tests)
  - Infrastructure 层：DO/Mapper/Converter + RepositoryImpl + 集成测试 (12 tests)
  - Adapter 层：Controller + Request/Response + API测试 (16 tests)
  - 数据库：schema.sql 更新
- **遇到问题**：
  - 测试代码 Java 8 兼容性问题（已解决）
  - Adapter 层测试缺少 GlobalExceptionHandler 支持（已解决）
- **状态**：✅ 完成

### 迭代 2 (2026-04-13)
- **完成内容**：
  - QA 测试用例设计
  - QA 验收测试
  - 补充缺失的测试类
  - 代码审查
  - 更新 CLAUDE.md 聚合列表
- **遇到问题**：
  - 熵检查发现 Adapter 层测试缺失（已补充）
  - 发现 GlobalExceptionHandler 缺少 User 错误码映射（已修复）
- **状态**：✅ 验收通过
