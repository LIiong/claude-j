# 测试用例设计 — {task-id}-{task-name}

## 测试范围
<!-- 一句话概括本次测试范围 -->


## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 一、Domain 层测试场景

<!-- 纯单元测试，JUnit 5 + AssertJ，禁止 Spring 上下文 -->

### {ValueObject} 值对象
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | 合法值创建 | - | new {VO}(valid) | 创建成功 |
| D2 | null 值 | - | new {VO}(null) | 抛 BusinessException |
| D3 | 边界值 | - | {描述} | {预期} |
| D4 | 相等性 | - | 两个相同值 | equals 返回 true |

### {Aggregate} 聚合根
| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D5 | 创建聚合 | - | {Aggregate}.create(...) | 初始状态正确 |
| D6 | 状态变更 | {前置状态} | {方法名}() | 状态变更成功 |
| D7 | 不变量约束 | {违反条件} | {方法名}() | 抛 BusinessException |

---

## 二、Application 层测试场景

<!-- Mock 单元测试，JUnit 5 + Mockito，禁止 Spring 上下文 -->

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | 正常用例 | {前置} | {service方法}(cmd) | 返回 DTO |
| A2 | 异常用例 | {前置} | {service方法}(cmd) | 抛 BusinessException |
| A3 | 编排验证 | {前置} | {service方法}(cmd) | verify save() 被调用 |

---

## 三、Infrastructure 层测试场景

<!-- 集成测试，@SpringBootTest + H2 -->

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 保存并查询 | - | save → findByXxx | 返回匹配记录 |
| I2 | 查询不存在 | - | findByXxx | 返回 empty |
| I3 | DO ↔ Domain 转换 | - | 保存 → 查询 | 字段完整还原 |

---

## 四、Adapter 层测试场景

<!-- API 测试，@WebMvcTest + MockMvc -->

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 成功响应 | Mock 正常返回 | {HTTP方法} {路径} | 200 + success=true |
| W2 | 参数校验失败 | 无效参数 | {HTTP方法} {路径} | 400 + success=false |
| W3 | 业务异常 | Mock 抛异常 | {HTTP方法} {路径} | 对应 HTTP 状态码 |

---

## 五、集成测试场景（全链路）

<!-- @SpringBootTest + AutoConfigureMockMvc + H2，在 start 模块 -->

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 核心流程正向 | {完整操作描述} | {预期结果} |
| E2 | 核心流程异常 | {完整操作描述} | {预期结果} |
| E3 | 全链路往返 | 创建 → 查询/操作 → 验证一致性 | 数据一致 |

---

## 六、代码审查检查项

- [ ] 依赖方向正确（adapter → application → domain ← infrastructure）
- [ ] domain 模块无 Spring/框架 import
- [ ] 聚合根封装业务不变量（非贫血模型）
- [ ] 值对象不可变，equals/hashCode 正确
- [ ] Repository 接口在 domain，实现在 infrastructure
- [ ] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response
- [ ] Controller 无业务逻辑
- [ ] 异常通过 GlobalExceptionHandler 统一处理

## 七、代码风格检查项

- [ ] Java 8 兼容（无 var、records、text blocks、List.of）
- [ ] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString
- [ ] DO 用 @Data + @TableName，DTO 用 @Data
- [ ] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [ ] 包结构符合 com.claudej.{layer}.{aggregate}.{sublayer}
- [ ] 测试命名 should_xxx_when_xxx
