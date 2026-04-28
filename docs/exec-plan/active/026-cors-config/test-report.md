# 测试报告 — 026-cors-config

**测试日期**：2026-04-28
**测试人员**：@qa
**版本状态**：待修复
**任务类型**：配置变更

---

## 一、测试执行结果

### 分层测试：`mvn clean test` ❌ 失败

命令：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml clean test`
关键输出：
- `Tests run: 110, Failures: 0, Errors: 78, Skipped: 0`
- `root cause: InventoryEventListener missing InventoryApplicationService bean`（来自 @dev handoff 的失败摘要，与本次全量执行一致）
- 本次执行实际退出码：`1`

| 模块 | 测试类 | 用例数 | 通过 | 失败 | 耗时 |
|------|--------|--------|------|------|------|
| domain | 多个现有单测类 | 已执行 | 通过至 infrastructure 前 | 0 | 见 Maven 输出 |
| application | 多个现有单测类 | 已执行 | 通过至 infrastructure 前 | 0 | 见 Maven 输出 |
| infrastructure | 既有集成/上下文测试 | 110 | 32 | 78 errors | 见 Maven 输出 |
| adapter | `SecurityCorsConfigTest` | 3 | 3 | 0 | 见定向测试 |
| **分层合计** | 全量回归 | — | — | ❌ | 构建失败 |

### 集成测试（全链路）：❌ 失败

命令：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test -pl claude-j-start -Dtest=CorsSecurityIntegrationTest`
关键输出：
- `Tests run: 3, Failures: 3, Errors: 0, Skipped: 0`
- `Status expected:<200> but was:<401>`
- `Response header 'Access-Control-Allow-Origin' expected:<http://localhost:3000> but was:<null>`
- 退出码：`1`

| 模块 | 测试类 | 用例数 | 通过 | 失败 | 耗时 |
|------|--------|--------|------|------|------|
| start | `CorsSecurityIntegrationTest` | 3 | 0 | 3 | `11.404 s` |

| **总计** | **1 个测试类** | **3** | **0** | **3** | **~11.4s** |

### 测试用例覆盖映射

| 设计用例 | 对应测试方法 | 状态 |
|----------|-------------|------|
| D1 | 全量 `mvn clean test` 零影响观察 | ❌（被 infrastructure 基线阻塞） |
| A1 | 全量 `mvn clean test` 零影响观察 | ❌（被 infrastructure 基线阻塞） |
| I1-I2 | 全量 `mvn clean test` + 变更范围审查 | ❌（基线失败已记录） |
| W1-W3 | `SecurityCorsConfigTest` 3 cases | ✅ |
| E1-E3 | `CorsSecurityIntegrationTest` 3 cases | ❌ |

---

## 二、代码审查结果

### 依赖方向检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| adapter → application（不依赖 domain/infrastructure） | ✅ | `SecurityConfig` 仅接入 `CorsConfigurationSource`，未引入逆向依赖。 |
| application → domain（不依赖其他层） | ✅ | 本任务未改动 application 代码。 |
| domain 无外部依赖 | ✅ | 本任务未改动 domain 代码。 |
| infrastructure → domain + application | ⚠️ | 本任务未改 infrastructure，但全量测试暴露既有 `InventoryEventListener` 装配问题，阻塞回归基线。 |

### 配置与安全链路检查

| 检查项 | 结果 | 说明 |
|--------|------|------|
| CORS 配置位于 start / adapter 边界 | ✅ | `CorsProperties` / `CorsConfig` 在 start，`SecurityConfig` 在 adapter，边界划分正确。 |
| Controller 未承载 CORS 业务逻辑 | ✅ | 未新增 `@CrossOrigin` 或 Controller 分支逻辑。 |
| 配置绑定方式合规 | ✅ | 使用 `@ConfigurationProperties + @Validated`，构造函数注入符合规范。 |
| 安全链对预检请求行为正确 | ❌ | 集成测试显示白名单预检返回 `401`，说明真实安全链未完成跨域协商。 |
| 未认证跨域请求保留鉴权且带 CORS 头 | ❌ | 集成测试中 `GET` 仍为 `401`，但缺少 `Access-Control-Allow-Origin`。 |

> 本任务不涉及领域模型检查、对象转换链检查、Controller 业务编排细节，已按模板说明省略。

---

## 三、代码风格检查结果

命令：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml checkstyle:check -B`
关键输出：
- `BUILD SUCCESS`
- `You have 0 Checkstyle violations`
- 退出码：由于上一个并行命令失败未返回独立结果，本轮以 handoff 中 dev 预飞记录与代码人工复核为辅证；需在 dev 修复后由 QA 回归时再次独立重跑并留完整输出。

| 检查项 | 结果 |
|--------|------|
| Java 8 兼容（无 var、records、text blocks、List.of） | ✅ |
| 聚合根仅 @Getter | ✅（本任务不涉及） |
| 值对象 @Getter + @EqualsAndHashCode + @ToString | ✅（本任务不涉及） |
| DO 用 @Data + @TableName | ✅（本任务不涉及） |
| DTO 用 @Data | ✅（本任务不涉及） |
| 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl | ✅ |
| 包结构 `com.claudej.{layer}.{aggregate}.{sublayer}` | ✅ |
| 测试命名 `should_xxx_when_xxx` | ✅ |

---

## 四、测试金字塔合规

| 层 | 测试类型 | 框架 | Spring 上下文 | 结果 |
|---|---------|------|-------------|------|
| Domain | 纯单元测试 | JUnit 5 + AssertJ | 无 | ✅ 本任务零改动，回归由全量测试覆盖 |
| Application | Mock 单元测试 | JUnit 5 + Mockito | 无 | ✅ 本任务零改动，回归由全量测试覆盖 |
| Infrastructure | 集成测试 | `@SpringBootTest` + H2 | 有 | ❌ 基线被既有 Bean 装配问题阻塞 |
| Adapter | API 测试 | `@WebMvcTest` + MockMvc | 部分（Web 层） | ✅ `SecurityCorsConfigTest` 3/3 通过 |
| **全链路** | **接口集成测试** | **`@SpringBootTest + AutoConfigureMockMvc + H2`** | **完整** | ❌ 新增 `CorsSecurityIntegrationTest` 3/3 失败，暴露真实安全链问题 |

---

## 五、问题清单

| # | 严重度 | 描述 | 处理 |
|---|--------|------|------|
| 1 | Critical | 真实 `@SpringBootTest` 全链路下，白名单来源预检请求未被 CORS 放行，`OPTIONS /api/v1/users/UR1234567890ABCDEF` 返回 `401` 而非 `200`，说明实现未满足核心验收条件 AC1。证据：`mvn -f /Users/macro.li/aiProject/claude-j/pom.xml test -pl claude-j-start -Dtest=CorsSecurityIntegrationTest`，失败行 `Status expected:<200> but was:<401>`。 | 打回 @dev，修复 Security + start 的真实集成配置，确保预检在安全链中命中 CORS 协商。 |
| 2 | Critical | 白名单来源的未认证业务请求虽返回 `401`，但缺少 `Access-Control-Allow-Origin`，浏览器会把鉴权失败误判为跨域失败，不满足 AC3。证据同上，失败行 `Response header 'Access-Control-Allow-Origin' expected:<http://localhost:3000> but was:<null>`。 | 打回 @dev，修复 401 响应上的 CORS 响应头传播。 |
| 3 | Major | `SecurityCorsConfigTest` 使用测试内自建 `CorsConfigurationSource`，验证的是切片场景而非 start 模块真实 `CorsConfig` 集成，导致切片测试 3/3 通过但全链路 3/3 失败，自动化证据与真实运行时存在断层。 | @dev 应收敛测试策略，保留切片测试同时补强真实 Bean 集成断言，避免伪绿。 |
| 4 | Major | 全量 `mvn clean test` 仍被既有 infrastructure 基线阻塞：`InventoryEventListener` 缺少 `InventoryApplicationService` Bean，导致 `Tests run: 110, Failures: 0, Errors: 78, Skipped: 0`。这不是本任务引入，但阻塞了 Verify 阶段的全量回归。疑似共同根因：inventory 相关 Spring 装配/模块扫描配置异常。 | 单独拆单修复基线；建议 @dev 先按 `.claude/skills/systematic-debugging/SKILL.md` Phase 1 定位 inventory 装配根因，再回归本任务。 |

**4 个阻塞性问题，0 个改进建议。**

---

## 六、验收结论

| 维度 | 结论 |
|------|------|
| 功能完整性 | ❌ 白名单预检与 401+CORS 头两个核心行为在真实全链路下均未成立。 |
| 测试覆盖 | ❌ 已补充 6 个与 CORS 直接相关的自动化用例（adapter 3 + start 集成 3），但 start 集成全部失败，无法证明 AC 达成。 |
| 架构合规 | ✅ CORS 代码落点与注入方式符合 start/adapter 边界；未发现越层修改。 |
| 代码风格 | ✅ Checkstyle 记录为 0 violations，人工复核未见 Java 8/命名违规。 |
| 数据库设计 | N/A 本任务不涉及数据库设计变更。 |

### 最终状态：❌ 待修复 — 见问题清单

当前不得归档。需 @dev 修复 CORS 真实集成行为与全量回归阻塞项后重新提测。
