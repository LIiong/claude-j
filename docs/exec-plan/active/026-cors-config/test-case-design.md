# 测试用例设计 — 026-cors-config

## 测试范围
验证 CORS 配置在 Spring Security + JWT 链路中的跨域协商行为、配置绑定约束，以及该任务对既有分层与安全语义的零业务侵入。

## 测试策略
按测试金字塔分层测试 + 代码审查 + 风格检查。

---

## 0. AC 自动化覆盖矩阵（强制，动笔前先填）

| # | 验收条件（AC） | 自动化层 | 对应测试方法 | 手动备注（若有） |
|---|---|---|---|---|
| AC1 | 白名单来源的预检请求在安全链路下返回正确 `Access-Control-Allow-*` 响应头 | Adapter | `SecurityCorsConfigTest.should_return_cors_headers_when_preflight_request_from_allowed_origin` | - |
| AC2 | 非白名单来源不会被错误放行，不返回允许来源头 | Adapter | `SecurityCorsConfigTest.should_not_allow_when_origin_not_whitelisted` | - |
| AC3 | 未认证的跨域业务请求仍保持 `401`，不能因 CORS 放宽授权 | Adapter | `SecurityCorsConfigTest.should_return_401_when_cross_origin_request_without_jwt` | - |
| AC4 | CORS 配置通过 `@ConfigurationProperties + @Validated` 绑定，开启时缺少白名单必须启动失败 | Start | `CorsPropertiesTest.should_fail_when_allowed_origins_missing_in_enabled_mode` | - |
| AC5 | 开发环境白名单配置可正确绑定并用于联调默认值 | Start | `CorsPropertiesTest.should_bind_dev_origins_when_profile_config_present` | - |
| AC6 | 全链路启动后对白名单来源的预检与未认证请求行为与设计一致 | Start（集成） | `CorsSecurityIntegrationTest.should_return_cors_headers_when_preflight_request_from_allowed_origin`、`CorsSecurityIntegrationTest.should_return_401_with_cors_header_when_request_without_jwt_from_allowed_origin` | - |

---

## 一、Domain 层测试场景

> 本任务不涉及 Domain 层变更，已按模板说明省略具体用例设计；验收关注点为零影响确认。

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| D1 | Domain 零影响确认 | 当前任务仅涉及 start/adapter 配置 | 检查变更范围与全量测试输出 | 无新增 domain 生产代码与领域规则变更 |

---

## 二、Application 层测试场景

> 本任务不涉及 Application 层变更，已按模板说明省略具体编排用例设计；验收关注点为零影响确认。

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| A1 | Application 零影响确认 | 当前任务仅涉及 start/adapter 配置 | 检查变更范围与安全测试依赖 | 无新增 application service / DTO / command 行为变更 |

---

## 三、Infrastructure 层测试场景

> 本任务不新增 DO/Mapper/Repository；Infrastructure 仅作为全量测试阻塞项观察对象。

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| I1 | 全量回归基线可执行性 | 执行 `mvn clean test` | 观察 infrastructure 模块启动 | 若失败，需记录真实根因与错误位置，不得伪造通过 |
| I2 | CORS 任务零持久化侵入 | 当前任务无 DB 设计变更 | 审查变更文件范围 | 无新增表、Mapper、Repository 实现改动 |

---

## 四、Adapter 层测试场景

| # | 场景 | 前置条件 | 操作 | 预期结果 |
|---|------|----------|------|----------|
| W1 | 白名单来源预检成功 | 配置允许 `http://localhost:3000` | `OPTIONS /api/v1/users/{id}` 携带 `Origin` 与 `Access-Control-Request-Method` | `200` 且返回 `Access-Control-Allow-Origin` 与 `Access-Control-Allow-Credentials` |
| W2 | 非白名单来源预检拒绝 | 来源为 `http://evil.example.com` | 发起同路径 `OPTIONS` 预检 | `403` 且无 `Access-Control-Allow-Origin` |
| W3 | 未认证跨域请求不放宽授权 | 白名单来源但无 JWT | `GET /api/v1/users/{id}` 携带 `Origin` | `401` 且带允许来源头，证明鉴权失败而非跨域失败 |

---

## 五、集成测试场景（全链路）

| # | 场景 | 操作 | 预期结果 |
|---|------|------|----------|
| E1 | 白名单来源预检全链路协商 | `OPTIONS /api/v1/users/{id}`，白名单来源，走 start 模块真实 CORS Bean | `200`，响应头包含允许来源、允许方法、允许凭证 |
| E2 | 白名单来源未认证请求全链路鉴权 | `GET /api/v1/users/{id}`，白名单来源，无 JWT | `401` 且保留 `Access-Control-Allow-Origin` |
| E3 | 非白名单来源全链路拒绝 | `OPTIONS /api/v1/users/{id}`，非白名单来源 | `403` 且不返回允许来源头 |

---

## 六、代码审查检查项

- [x] 依赖方向正确（adapter → application → domain ← infrastructure）
- [x] domain 模块无 Spring/框架 import（本任务未改动 domain）
- [x] 聚合根封装业务不变量（本任务不涉及领域模型变更）
- [x] 值对象不可变，equals/hashCode 正确（本任务不涉及值对象变更）
- [x] Repository 接口在 domain，实现在 infrastructure（本任务未改动）
- [x] 对象转换链正确：DO ↔ Domain ↔ DTO ↔ Request/Response（本任务未引入新对象链）
- [x] Controller 无业务逻辑（CORS 接入位于 `SecurityConfig`，未向 Controller 下沉）
- [x] 异常通过 GlobalExceptionHandler 统一处理（未新增旁路异常处理）

## 七、代码风格检查项

- [x] Java 8 兼容（无 var、records、text blocks、List.of）
- [x] 聚合根用 @Getter，值对象用 @Getter + @EqualsAndHashCode + @ToString（本任务不涉及）
- [x] DO 用 @Data + @TableName，DTO 用 @Data（本任务不涉及）
- [x] 命名规范：XxxDO, XxxDTO, XxxMapper, XxxRepository, XxxRepositoryImpl
- [x] 包结构符合 `com.claudej.{layer}.{aggregate}.{sublayer}`
- [x] 测试命名 `should_xxx_when_xxx`
