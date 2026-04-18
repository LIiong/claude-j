# ADR-005: 敏感配置校验策略

**日期**：2026-04-18
**状态**：已接受
**决策者**：@architect / 用户

## 背景

010-secret-externalize 任务引入 `JwtSecretValidator`（实现 `ApplicationRunner`）做启动时密钥校验，复盘发现两个问题：

1. **时机错**：`ApplicationRunner` 在所有 Bean 初始化 **之后** 运行。但 `JwtTokenServiceImpl` 构造时就会用 secret 计算 HMAC key —— 如果 secret 非法，`JwtTokenServiceImpl` 先抛异常，校验器根本跑不到，错误消息定位成 Bean 创建失败而非"密钥无效"。
2. **可测性差**：字段注入 `@Value("${jwt.secret}")` 导致单测必须用反射注入私有字段，违反「不测私有实现细节」原则。

后续任务（011-flyway 的数据库密码、012-multi-profile 的生产环境变量、C1 Spring Security 的签名密钥等）都会涉及敏感配置校验，需要统一范式。

## 决策

**敏感配置校验一律使用 `@ConfigurationProperties + @Validated` + JSR-303 注解**，禁止：
- `ApplicationRunner` / `CommandLineRunner` 做启动校验
- `@PostConstruct` 做跨字段配置校验（仅允许做单对象内部一致性校验）

### 推荐范式

```java
// 1. 配置绑定类（属性集合 + 校验注解）
@ConfigurationProperties(prefix = "jwt")
@Validated
@Data
public class JwtProperties {

    @NotBlank(message = "JWT_SECRET environment variable is required")
    @Size(min = 32, message = "JWT_SECRET must be at least 32 characters")
    private String secret;

    @Positive
    private int accessTokenExpiration;

    @Positive
    private int refreshTokenExpiration;
}

// 2. 注册
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig { }

// 3. 使用方（构造注入）
@Service
public class JwtTokenServiceImpl {
    private final JwtProperties props;
    public JwtTokenServiceImpl(JwtProperties props) {
        this.props = props;
    }
}
```

### 失败行为

- 属性绑定阶段（应用启动最早期之一）就抛 `BindValidationException`
- 错误消息明确指向违规字段 + 注解 message
- 远早于业务 Bean 初始化，不会出现"密钥校验器被密钥消费者抢先挂掉"的怪象

### 单元测试

直接 `new JwtProperties()` + setter，或构造`JwtProperties` POJO 传入依赖方，无需反射、无需 Spring Context：

```java
@Test
void should_rejectSecret_when_tooShort() {
    JwtProperties props = new JwtProperties();
    props.setSecret("short");

    Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    Set<ConstraintViolation<JwtProperties>> violations = validator.validate(props);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage())
        .contains("at least 32 characters");
}
```

启动失败场景用 `ApplicationContextRunner`：
```java
new ApplicationContextRunner()
    .withUserConfiguration(JwtConfig.class)
    .withPropertyValues("jwt.secret=")
    .run(ctx -> assertThat(ctx)
        .hasFailed()
        .getFailure()
        .rootCause()
        .hasMessageContaining("JWT_SECRET"));
```

## 备选方案

| 方案 | 优点 | 缺点 | 排除原因 |
|------|------|------|----------|
| `ApplicationRunner` 手写校验 | 可写任意自定义逻辑 | 时机太晚（消费者可能先挂）；字段注入导致测试难；规则分散 | 010 已证明不可靠 |
| `@PostConstruct` 在消费类内校验 | 时机稍早 | 职责污染；多消费者重复校验；单测仍需构造 Spring 组件 | 职责不清晰 |
| `SpringApplication#addListeners` 自定义事件 | 最早介入 | 样板代码多；可测性差 | 过于复杂 |
| **`@ConfigurationProperties + @Validated`** | Spring 原生、绑定阶段即校验、可用标准 JSR-303 注解、单测不依赖 Spring | 仅能做声明式校验，跨字段规则需 `@AssertTrue` 辅助 | ✅ 选此 |

## 影响

### 需要改造的既有代码
- `JwtSecretValidator`（010 引入）→ 下一个 auth 相关任务改造为 `JwtProperties`，当前保留观察
- 未来 011-flyway 的 `spring.flyway.*` 走 Spring Boot 内建 `FlywayProperties`，无需自建

### 规则同步
- `docs/standards/java-dev.md` 增加 MUST：敏感配置校验用 `@ConfigurationProperties + @Validated`，禁 `ApplicationRunner`/`@PostConstruct` 做配置校验
- `@architect` checklist 增加一项：若改动涉及启动期配置校验，确认遵循本 ADR

### 不受影响
- 一次性业务逻辑的 `ApplicationRunner`（如启动时导入初始化数据）仍允许使用 —— 本 ADR 仅限制 **配置校验** 用途
- Bean 内部不变量校验的 `@PostConstruct` 仍允许 —— 本 ADR 仅限制 **跨字段/跨环境配置校验**
