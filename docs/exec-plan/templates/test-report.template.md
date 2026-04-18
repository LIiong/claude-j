# 测试报告 — {task-id}-{task-name}

**测试日期**：{YYYY-MM-DD}
**测试人员**：@qa
**版本状态**：{待验收 / 验收通过 / 待修复}
**任务类型**：{业务聚合 / 配置变更 / 基础设施 / 运维 / 文档}

> **模板使用提示（010 复盘后新增）**：
> - **业务聚合任务**（新增聚合、新增 API、新增业务规则）：填写所有章节
> - **非业务任务**（配置变更如 010 密钥外置、CI/部署、运维文档）：可**整节删除**不相关的 N/A 章节，在「任务类型」已声明后无需逐格打 N/A
> - 允许删除的章节：二节下的"领域模型检查/对象转换链检查/Controller 检查"、四节测试金字塔未涉及的层
> - 不允许删除的章节：一（测试执行）、三（代码风格）、五（问题清单）、六（验收结论）—— 任何任务都要留下证据
> - 删除的章节末尾用一行声明替代：`> 本任务不涉及 {章节名}，已按模板说明省略`

---

## 一、测试执行结果

### 分层测试：`mvn clean test` {✅ 通过 / ❌ 失败}

| 模块 | 测试类 | 用例数 | 通过 | 失败 | 耗时 |
|------|--------|--------|------|------|------|
| domain | {TestClass} | | | | |
| application | {TestClass} | | | | |
| infrastructure | {TestClass} | | | | |
| adapter | {TestClass} | | | | |
| **分层合计** | | | | | |

### 集成测试（全链路）：{✅ 通过 / ❌ 失败}

| 模块 | 测试类 | 用例数 | 通过 | 失败 | 耗时 |
|------|--------|--------|------|------|------|
| start | {IntegrationTest} | | | | |

| **总计** | **x 个测试类** | **x** | **x** | **0** | **~xs** |

### 测试用例覆盖映射

| 设计用例 | 对应测试方法 | 状态 |
|----------|-------------|------|
| D1-Dx | {TestClass} (x cases) | ✅ / ❌ |
| A1-Ax | {TestClass} (x cases) | ✅ / ❌ |
| I1-Ix | {TestClass} (x cases) | ✅ / ❌ |
| W1-Wx | {TestClass} (x cases) | ✅ / ❌ |
| E1-Ex | {IntegrationTest} (x cases) | ✅ / ❌ |

---

## 二、代码审查结果

### 依赖方向检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| adapter → application（不依赖 domain/infrastructure） | ✅ / ⚠️ | |
| application → domain（不依赖其他层） | ✅ / ⚠️ | |
| domain 无外部依赖 | ✅ / ⚠️ | |
| infrastructure → domain + application | ✅ / ⚠️ | |

### 领域模型检查

| 检查项 | 结果 |
|--------|------|
| domain 模块零 Spring/框架 import | |
| 聚合根封装业务不变量（非贫血模型） | |
| 值对象不可变，字段 final | |
| 值对象 equals/hashCode 正确 | |
| Repository 接口在 domain，实现在 infrastructure | |

### 对象转换链检查

| 转换 | 方式 | 结果 |
|------|------|------|
| Request → Command | 手动赋值 / MapStruct | |
| Domain → DTO | MapStruct | |
| Domain ↔ DO | 静态方法 / MapStruct | |
| DTO → Response | 手动赋值 / MapStruct | |
| DO 未泄漏到 infrastructure 之上 | — | |

### Controller 检查

| 检查项 | 结果 |
|--------|------|
| Controller 无业务逻辑，仅委托 application service | |
| 异常通过 GlobalExceptionHandler 统一处理 | |
| HTTP 状态码正确 | |

---

## 三、代码风格检查结果

| 检查项 | 结果 |
|--------|------|
| Java 8 兼容（无 var、records、text blocks、List.of） | |
| 聚合根仅 @Getter | |
| 值对象 @Getter + @EqualsAndHashCode + @ToString | |
| DO 用 @Data + @TableName | |
| DTO 用 @Data | |
| 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl | |
| 包结构 com.claudej.{layer}.{aggregate}.{sublayer} | |
| 测试命名 should_xxx_when_xxx | |

---

## 四、测试金字塔合规

| 层 | 测试类型 | 框架 | Spring 上下文 | 结果 |
|---|---------|------|-------------|------|
| Domain | 纯单元测试 | JUnit 5 + AssertJ | 无 | |
| Application | Mock 单元测试 | JUnit 5 + Mockito | 无 | |
| Infrastructure | 集成测试 | @SpringBootTest + H2 | 有 | |
| Adapter | API 测试 | @WebMvcTest + MockMvc | 部分（Web 层） | |
| **全链路** | **接口集成测试** | **@SpringBootTest + AutoConfigureMockMvc + H2** | **完整** | |

---

## 五、问题清单

<!-- 严重度：高（阻塞验收）/ 中（需修复后回归）/ 低（建议改进，不阻塞） -->

| # | 严重度 | 描述 | 处理 |
|---|--------|------|------|
| 1 | {高/中/低} | {问题描述} | {处理方式} |

**{阻塞性问题数量}个阻塞性问题，{改进建议数量}个改进建议。**

---

## 六、验收结论

| 维度 | 结论 |
|------|------|
| 功能完整性 | {✅ / ❌} {说明} |
| 测试覆盖 | {✅ / ❌} x 个测试用例，覆盖 x 层 |
| 架构合规 | {✅ / ❌} {说明} |
| 代码风格 | {✅ / ❌} {说明} |
| 数据库设计 | {✅ / ❌ / N/A} {说明} |

### 最终状态：{✅ 验收通过 / ❌ 待修复 — 见问题清单}

<!-- 验收通过后填写 -->
可归档至 `docs/exec-plan/archived/{task-id}-{task-name}/`。
