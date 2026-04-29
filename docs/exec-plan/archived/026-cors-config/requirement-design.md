# 需求拆分设计 — 026-cors-config

## 需求描述
在现有 Spring Security + JWT 无状态认证链路中补充 CORS 配置能力，为前后端分离联调提供受控的跨域协商支持。
方案需提供按环境收敛的白名单来源配置，保证预检请求与实际跨域请求在安全链路下行为正确，同时不放宽现有受保护接口的认证要求。

## 领域分析

### 聚合根: 无新增聚合根
本任务属于启动模块与安全适配层的横切配置，不引入新的业务聚合、实体或值对象，也不改变现有订单/用户/认证等领域模型不变量。

### 值对象
- 无新增领域值对象。

### 领域服务（如有）
- 无新增领域服务。CORS 属于 HTTP 安全协商规则，不应下沉到 domain/application。

### 端口接口
- 无新增 Repository 端口。
- 无新增领域端口接口。
- 需要在 start/adapter 边界内新增配置对象与 Spring Bean：
  - **CorsProperties**：承载允许来源、允许方法、允许头、是否允许携带凭证等配置。
  - **CorsConfigurationSource Bean**：为 Spring Security 提供统一 CORS 配置入口。

### 边界与职责划分
- **domain**：不感知 CORS。
- **application**：不感知 CORS。
- **infrastructure**：不承担 HTTP 跨域协商。
- **adapter**：`SecurityConfig` 接入 `.cors()`，确保安全过滤链对 OPTIONS 与跨域请求使用统一配置。
- **start**：通过 `@ConfigurationProperties + @Validated` 提供环境化白名单配置，并在 `application.yml` / `application-dev.yml` 中声明默认值与最小暴露策略。

## 假设与待确认
- **假设 1**：本任务只需要支持浏览器跨域协商，不新增基于路径的细粒度 CORS 策略；所有 API 统一使用一套来源白名单。
- **假设 2**：开发环境默认允许的前端来源至少包含常见本地地址，如 `http://localhost:3000`、`http://127.0.0.1:3000`、`http://localhost:5173`、`http://127.0.0.1:5173`，以满足 React/Vite 联调。
- **假设 3**：非开发环境默认不开放额外来源；若需要开放，由部署环境显式配置白名单，而不是在代码中放宽默认值。
- **假设 4**：前端携带 JWT 的方式以 `Authorization` 请求头为主，因此允许头需覆盖 `Authorization`、`Content-Type`、`Accept`、`Origin`，并支持预检常见请求头。
- **待确认 1**：生产环境是否需要支持通配子域（如 `https://*.example.com`）。若需要，应优先使用 `allowedOriginPatterns` 还是显式枚举域名，需要 @architect 确认。
- **待确认 2**：是否需要暴露自定义响应头（如 `Authorization`、`Location`、分页头）。当前先按最小暴露处理，不额外开放 exposed headers。
- **待确认 3**：运维文档落点是否接受更新现有 roadmap/配置说明文档，而不是新增独立 README；本设计默认在 Build 阶段补充最小必要文档说明。

## 关键算法/技术方案
### 方案目标
- 让浏览器预检请求在安全链路下得到正确的 CORS 响应头。
- 不改变业务接口的认证授权语义：跨域请求若命中受保护端点，仍需 JWT 才能通过。
- 使用配置绑定替代硬编码，满足 `docs/standards/java-dev.md` 的配置校验规则。

### 方案设计
1. 在 `claude-j-start` 新增 `CorsProperties`：
   - 使用 `@ConfigurationProperties(prefix = "app.security.cors")`。
   - 使用 `@Validated` + JSR-303 约束校验必填集合与布尔配置。
   - 属性建议：`enabled`、`allowedOrigins`、`allowedMethods`、`allowedHeaders`、`allowCredentials`、`maxAge`。
2. 在 start 模块新增 `CorsConfig`（或同类配置类）暴露 `CorsConfigurationSource` Bean：
   - 从 `CorsProperties` 组装 Spring `CorsConfiguration`。
   - 仅当 `enabled=true` 时注册受控配置；非开发环境默认来源为空或显式最小集合。
3. 在 `claude-j-adapter` 的 `SecurityConfig` 中启用 `http.cors().configurationSource(...)`：
   - 保持现有 JWT filter、异常处理、公开端点规则不变。
   - 明确允许 OPTIONS 预检通过 CORS 协商，但不新增对受保护业务接口的 `permitAll` 放宽。
4. 配置分层：
   - `application.yml`：定义安全默认值（最小暴露）。
   - `application-dev.yml`：追加本地前端白名单，满足开发联调。
5. 文档：
   - 在运维/路线图文档中补充配置键说明、默认行为、生产环境如何显式配置。

### 备选方案与取舍
- **方案 A：在 SecurityFilterChain 中集成 `CorsConfigurationSource` Bean**
  - 优点：与现有安全链统一，预检与鉴权交互可控，符合验收条件 2/3。
  - 缺点：需要补充安全切片测试。
  - 结论：采用。
- **方案 B：单独注册 `WebMvcConfigurer#addCorsMappings`**
  - 优点：实现简单。
  - 缺点：可能绕开或弱化 Security 过滤链语义，难以证明“安全链路下行为正确”。
  - 结论：不采用，除非 @architect 明确要求双层配置。
- **方案 C：直接在控制器层逐个 `@CrossOrigin`**
  - 优点：粒度细。
  - 缺点：侵入大量 adapter 类，容易遗漏，且不符合统一最小暴露原则。
  - 结论：不采用。

### 测试策略（TDD 设计）
遵循先红后绿，优先补充 start/adapter 层测试：
1. **Start 配置绑定测试**
   - 使用 `ApplicationContextRunner` 或等价轻量上下文测试 `CorsProperties` 绑定与校验。
   - 覆盖开发环境默认来源可加载、非法配置被拒绝。
2. **Adapter 安全切片测试**
   - 新增基于现有 `SecurityConfig` 的 MockMvc 测试。
   - 覆盖：
     - `OPTIONS` 预检命中受保护接口时返回正确 CORS 头。
     - 跨域 `GET/POST` 未携带 JWT 时仍返回 401，而不是因 CORS 配置误放行为 200。
     - 允许来源返回 `Access-Control-Allow-Origin`，非白名单来源不返回允许头或被拒绝。
3. **集成测试预算控制**
   - 本任务不计划新增超过 1 个 `@SpringBootTest` 场景；优先使用 `@WebMvcTest` / 轻量上下文测试，避免重复覆盖既有认证功能。

## API 设计
本任务不新增业务 API，仅影响现有 REST API 的跨域协商行为。

| 方法 | 路径 | 描述 | 请求体 | 响应体 |
|------|------|------|--------|--------|
| OPTIONS | `/api/v1/**` | 浏览器预检请求，返回 CORS 协商头 | — | 空体 + `Access-Control-Allow-*` 响应头 |
| GET/POST/... | 现有 `/api/v1/**` | 维持现有业务语义；若跨域且来源在白名单内，附加 CORS 响应头 | 维持现状 | 维持现状 |

### 关键响应行为
- 预检请求成功时：
  - `Access-Control-Allow-Origin: <origin>`
  - `Access-Control-Allow-Methods` 包含请求方法
  - `Access-Control-Allow-Headers` 包含 `Authorization` 等允许头
  - 根据配置决定是否返回 `Access-Control-Allow-Credentials: true`
- 受保护接口未认证时：
  - 保持现有 `401 Unauthorized` / 统一错误体
  - 对白名单来源附带必要 CORS 响应头，避免浏览器把鉴权失败误判为跨域失败

## 数据库设计（如有）
本任务无需新增或修改 DDL。
原因：CORS 白名单属于应用配置，不属于持久化业务数据；配置应由环境文件或部署参数提供，而不是落库。

## 运维/配置设计
建议新增以下配置键：
```yaml
app:
  security:
    cors:
      enabled: true
      allow-credentials: true
      max-age: 1800
      allowed-origins:
        - http://localhost:3000
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers:
        - Authorization
        - Content-Type
        - Accept
        - Origin
```

环境策略：
- **dev**：内置本地开发前端白名单。
- **test**：测试按用例显式注入白名单，避免依赖环境噪音。
- **prod / 非 dev**：默认最小暴露；部署时按环境变量或外部配置显式传入允许来源。

## 影响范围
- **domain**:
  - 无变更。
- **application**:
  - 无变更。
- **infrastructure**:
  - 无变更。
- **adapter**:
  - `claude-j-adapter/src/main/java/com/claudej/adapter/auth/security/SecurityConfig.java`
  - 新增/调整安全测试，参考现有 `TestSecurityConfig` 与 MockMvc 模式。
- **start**:
  - 新增 CORS 配置属性类与配置类。
  - 更新 `claude-j-start/src/main/resources/application.yml`
  - 更新 `claude-j-start/src/main/resources/application-dev.yml`（若已存在则增量修改）
  - 视 Build 实施结果补充运维说明文档。
  - 路线图 `docs/roadmap/industry-gap-analysis.md` 在验收通过后可将 C3 标记完成。

## 架构评审

**评审人**：@architect
**日期**：2026-04-28
**结论**：✅ 通过

### 评审检查项（15 维四类）

**架构合规（7 项）**
- [x] 聚合根边界合理（遵循事务一致性原则）
- [x] 值对象识别充分（金额、标识符等应为 VO）
- [x] Repository 端口粒度合适（方法不多不少）
- [x] 与已有聚合无循环依赖
- [x] DDL 设计与领域模型一致（字段映射、索引合理）
- [x] API 设计符合 RESTful 规范
- [x] 对象转换链正确（DO ↔ Domain ↔ DTO ↔ Request/Response）

**需求质量（3 项）**
- [x] 需求无歧义：核心名词、流程、异常分支均有明确定义
- [x] 验收条件可验证：每条 AC 可转化为 `should_xxx_when_yyy` 测试用例
- [x] 业务规则完备：状态机/不变量/边界值在需求中已列明

**计划可执行性（2 项）**
- [x] task-plan 粒度合格：按层任务已分解到原子级（10–15 分钟/步），每步含文件路径 + 验证命令 + 预期输出（详见 `docs/exec-plan/templates/task-plan.template.md` 原子任务章节）
- [x] 依赖顺序正确：domain → application → infrastructure → adapter → start 自下而上，层间依赖无倒置

**可测性保障（3 项 — 010 复盘后新增）**
- [x] **AC 自动化全覆盖**：`test-case-design.md` 的「AC 自动化覆盖矩阵」每条 AC 都有对应自动化测试方法；任一标「手动」但无替代自动化测试 → **打回**
- [x] **可测的注入方式**：若引入新 Spring Bean，使用构造函数注入而非字段注入（避免测试反射）；详见 `java-dev.md` 依赖注入规则
- [x] **配置校验方式合规**：若涉及敏感/跨环境配置校验，使用 `@ConfigurationProperties + @Validated`，不得用 `ApplicationRunner`/`@PostConstruct`；详见 ADR-005

**心智原则（Karpathy — 动手前自检）**
- [x] **简洁性**：需求未要求的抽象/配置/工厂已移除；任何单一实现的 `XxxStrategy`/`XxxFactory` 需说明存在理由
- [x] **外科性**：设计仅改动任务直接相关的文件；若涉及跨聚合大改，在评审意见说明理由
- [x] **假设显性**：需求里含糊的字段/边界/异常，requirement-design 已在「假设与待确认」列出

### 评审意见
- 方案选择正确：CORS 放在 `start` 提供配置和 `CorsConfigurationSource`，由 `adapter` 的 `SecurityFilterChain` 显式接入；这符合六边形边界，避免把 HTTP 协商逻辑下沉到 `domain/application/infrastructure`。
- 预检 `OPTIONS` 的风险已被约束为“只做协商，不放宽鉴权”；设计明确要求保留受保护接口的 `401` 语义，且只对白名单来源返回 CORS 头，未出现把受保护接口整体 `permitAll` 的问题。
- 配置键命名 `app.security.cors.*` 与现有 `@ConfigurationProperties + @Validated` 约定一致，环境策略采用 `dev` 默认放开本地前端、非 dev 最小暴露，也符合项目“配置优先、默认收敛”的原则。
- `task-plan.md` 已拆到可执行原子步，且验证命令覆盖了绑定测试、安全切片测试与三项预飞，粒度可落地。
- 唯一建议是：后续 Build 阶段在实现 `CorsConfigurationSource` 时保持“仅配置层拥有白名单默认值，adapter 只消费 bean”，避免把环境策略复制进安全配置类。

### 需要新增的 ADR
- 无需新增 ADR。当前方案已被 ADR-005 的配置校验策略覆盖；本任务属于该决策的具体落地，不构成新的架构决策边界。
