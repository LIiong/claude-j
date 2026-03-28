# Java 单元测试规则

## 测试金字塔

| 层 | 测试类型 | 框架 | Spring 上下文 |
|---|---------|------|-------------|
| Domain | 纯单元测试 | JUnit 5 + AssertJ | 禁止 |
| Application | Mock 单元测试 | JUnit 5 + Mockito + AssertJ | 禁止 |
| Infrastructure | 集成测试 | @SpringBootTest + H2 | 必须 |
| Adapter | API 测试 | @WebMvcTest + MockMvc | 部分（仅 Web 层） |

## Domain 层测试
- 位置：`claude-j-domain/src/test/java/`
- 禁止 `@SpringBootTest`、禁止 `@ExtendWith(SpringExtension.class)`
- 禁止 Mockito — 领域对象无外部依赖需要 mock
- 测试聚合行为：
  - 状态转换（如 CREATED → PAID → SHIPPED）
  - 不变量强制（如不能添加数量 <= 0 的商品）
  - 计算正确性（如总金额计算）
  - 异常场景（如取消已发货订单）
- 测试值对象：
  - 相等语义（两个 Money(100, "CNY") 应相等）
  - 不可变性（操作返回新实例）
  - 边界情况（null、零、负值）

## Application 层测试
- 位置：`claude-j-application/src/test/java/`
- 使用 `@ExtendWith(MockitoExtension.class)`
- 使用 `@Mock` mock Repository 端口
- 使用 `@InjectMocks` 注入应用服务
- 验证编排：领域方法按正确顺序调用
- 验证 `save()` 在领域操作后被调用
- 测试命令校验边界情况

## Infrastructure 层测试
- 位置：`claude-j-infrastructure/src/test/java/`
- 使用 `@SpringBootTest` + H2 数据库
- 测试往返：保存领域对象 → 查询 → 验证字段匹配
- 测试 MyBatis-Plus Mapper 操作：insert、selectById、update、delete
- 测试 DO ↔ Domain 转换准确性
- 加载 `schema.sql` 建表

## Adapter 层测试
- 位置：`claude-j-adapter/src/test/java/`
- 使用 `@WebMvcTest(XxxController.class)`
- 使用 `@MockBean` mock 应用服务
- 使用 MockMvc 测试：
  - HTTP 方法和路径正确
  - 请求体校验生效（@Valid）
  - 成功响应格式：`{"success": true, "data": ...}`
  - 错误响应格式：`{"success": false, "errorCode": ..., "message": ...}`
  - HTTP 状态码：200、400、404、500

## 测试命名规范
```java
@Test
void should_预期行为_when_条件() { }
```
示例：
- `should_throwBusinessException_when_cancellingDeliveredOrder()`
- `should_calculateCorrectTotal_when_multipleItemsAdded()`
- `should_return400_when_customerIdIsBlank()`

## 测试结构（AAA 模式）
```java
@Test
void should_doSomething_when_someCondition() {
    // Arrange — 准备测试数据和前置条件

    // Act — 执行被测行为

    // Assert — 验证预期结果
}
```

## 测试禁止事项
- 禁止跨层测试依赖（domain 测试不得依赖 infrastructure）
- 禁止在 domain/application 单元测试中启动 Spring 上下文
- 禁止在单元测试中使用真实数据库（仅 infrastructure 集成测试可用）
- 禁止 Thread.sleep()
- 禁止测试顺序依赖（@Order）
- 禁止测试方法间共享可变状态
- 禁止直接测试私有方法 — 通过公开 API 测试

## 测试文件位置
与源码结构镜像：
```
src/main/java/com/claudej/domain/order/model/aggregate/Order.java
src/test/java/com/claudej/domain/order/model/aggregate/OrderTest.java
```
