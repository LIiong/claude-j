# 开发日志 — 010-secret-externalize

## 问题记录

<!-- 开发过程中遇到的问题、做出的决策及原因。按时间顺序记录。 -->

### 1. 任务类型识别
- **问题**：这是一个配置外置化任务，不是业务聚合开发
- **决策**：按照配置变更任务模式设计，不按 DDD 聚合标准流程
- **原因**：任务本质是安全配置管理，无业务领域模型，主要涉及：
  - 配置文件变更 (application.yml / application-dev.yml)
  - 启动校验组件 (JwtSecretValidator)
  - CI 环境变量配置
  - 运维文档编写

### 2. JwtSecretValidator 位置选择
- **问题**：Validator 应放在哪个模块？
- **决策**：放在 infrastructure 层的 auth/config 包下
- **原因**：
  - 属于基础设施层的配置管理组件
  - 依赖 Spring Boot 的 ApplicationRunner
  - 与 JwtTokenServiceImpl 同属 auth 模块，便于维护

### 3. 默认值处理策略
- **问题**：JwtTokenServiceImpl 当前有 fallback 默认值
- **决策**：移除 fallback，完全依赖外部配置 + 启动校验
- **原因**：
  - fallback 会掩盖配置缺失问题
  - 启动校验确保运行时一定有有效配置
  - 开发环境通过 application-dev.yml 提供默认值

## 变更记录

<!-- 与原设计（requirement-design.md）不一致的变更 -->
<!-- 格式：变更内容 + 变更原因 -->
- 无与原设计不一致的变更。
