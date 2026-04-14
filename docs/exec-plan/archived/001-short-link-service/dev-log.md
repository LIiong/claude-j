# 开发日志 — 001-short-link-service

## 问题记录

### 1. ShortLink.createTime final 字段赋值冲突
- **问题**：`createTime` 声明为 `final`，但 `reconstruct()` 工厂方法中需要传入自定义值，编译报错。
- **决策**：将私有构造函数改为接受 `(OriginalUrl, LocalDateTime createTime)` 两个参数，`create()` 方法传 `LocalDateTime.now()`，`reconstruct()` 传持久化时间。保持 `final` 不可变语义。
- **原因**：值对象和聚合根字段应尽量 final 以保证不变性。

### 2. GlobalExceptionHandler 导入 domain 层异常类
- **问题**：adapter 层规则说"禁止直接 import domain 类"，但 `GlobalExceptionHandler` 需要捕获 `BusinessException`。
- **决策**：保留当前实现。`BusinessException` 是领域层的公共契约，通过 application 层传播到 adapter 层，属于合理的跨层异常流。
- **原因**：异常处理是横切关注点，强制包装为 application 层异常会增加不必要的复杂度。QA 审查时需评估此决策。

### 3. 基础设施层测试需要 -am 参数
- **问题**：单独运行 `mvn test -pl claude-j-infrastructure` 编译失败，因找不到 domain 类。
- **决策**：使用 `mvn test -pl claude-j-infrastructure -am` 或全量 `mvn test`。
- **原因**：Maven 多模块项目中，需要先构建依赖模块。

## 变更记录
- 无与原设计不一致的变更。
