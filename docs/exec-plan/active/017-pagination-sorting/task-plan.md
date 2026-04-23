# 任务执行计划 — 017-pagination-sorting

## 任务状态跟踪

<!-- 状态流转：待办 → 进行中 → 单测通过 → 待验收 → 验收通过 / 待修复 -->

| # | 任务 | 负责人 | 状态 | 备注 |
|---|------|--------|------|------|
| 1 | Domain: PageRequest + SortDirection 值对象 + 测试 | dev | 验收通过 | 49 tests |
| 2 | Domain: Page<T> 值对象 + 测试 | dev | 验收通过 | |
| 3 | Domain: Repository 接口新增分页方法 | dev | 验收通过 | 4 个 Repository |
| 4 | Application: PageDTO + PageAssembler | dev | 验收通过 | |
| 5 | Application: ApplicationService 新增分页方法 + 测试 | dev | 验收通过 | 5 个 Service |
| 6 | Infrastructure: PageHelper 工具类 | dev | 验收通过 | |
| 7 | Infrastructure: RepositoryImpl 分页方法实现 + 测试 | dev | 验收通过 | 4 个 RepositoryImpl |
| 8 | Adapter: PageResponse + PageRequestAdapter | dev | 验收通过 | |
| 9 | Adapter: Controller 分页改造 + 测试 | dev | 验收通过 | 5 个 Controller |
| 10 | 全量 mvn test | dev | 验收通过 | Tests: 52 |
| 11 | QA: 测试用例设计 | qa | 待办 | |
| 12 | QA: 验收测试 + 代码审查 | qa | 待办 | |
| 13 | QA: 接口集成测试 | qa | 待办 | |

## 执行顺序

domain → application → infrastructure → adapter → 全量测试 → QA 验收

## 原子任务分解（每项 10–15 分钟，单会话可完成并 commit）

### 1.1 Domain 值对象 `PageRequest`
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/model/valobj/PageRequest.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/common/model/valobj/PageRequestTest.java`
- **骨架**（Red 阶段先写测试）：
  ```java
  // PageRequestTest.java
  @Test
  void should_throw_when_page_is_negative() { ... }
  @Test
  void should_throw_when_size_is_zero() { ... }
  @Test
  void should_throw_when_size_exceeds_max() { ... }
  @Test
  void should_use_default_when_params_null() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PageRequestTest`
- **预期输出**：`Tests run: 4+, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 分页请求值对象 PageRequest`

### 1.2 Domain 值对象 `SortDirection`
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/model/valobj/SortDirection.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/common/model/valobj/SortDirectionTest.java`
- **骨架**：
  ```java
  @Test
  void should_have_asc_and_desc_values() { ... }
  @Test
  void should_parse_from_string() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=SortDirectionTest`
- **预期输出**：`Tests run: 2, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 排序方向枚举 SortDirection`

### 2.1 Domain 值对象 `Page<T>`
- **文件**：`claude-j-domain/src/main/java/com/claudej/domain/common/model/valobj/Page.java`
- **测试**：`claude-j-domain/src/test/java/com/claudej/domain/common/model/valobj/PageTest.java`
- **骨架**：
  ```java
  @Test
  void should_calculate_total_pages_correctly() { ... }
  @Test
  void should_identify_first_page() { ... }
  @Test
  void should_identify_last_page() { ... }
  @Test
  void should_identify_empty_page() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-domain -Dtest=PageTest`
- **预期输出**：`Tests run: 4+, Failures: 0, Errors: 0`
- **commit**：`feat(domain): 分页结果值对象 Page<T>`

### 3.1 Domain Repository 接口新增分页方法
- **文件**：
  - `claude-j-domain/src/main/java/com/claudej/domain/link/repository/LinkRepository.java`
  - `claude-j-domain/src/main/java/com/claudej/domain/user/repository/UserRepository.java`
  - `claude-j-domain/src/main/java/com/claudej/domain/coupon/repository/CouponRepository.java`
  - `claude-j-domain/src/main/java/com/claudej/domain/order/repository/OrderRepository.java`
- **验证命令**：`mvn compile -pl claude-j-domain`
- **预期输出**：编译通过
- **commit**：`feat(domain): Repository 接口新增分页方法`

### 4.1 Application PageDTO + PageAssembler
- **文件**：
  - `claude-j-application/src/main/java/com/claudej/application/common/dto/PageDTO.java`
  - `claude-j-application/src/main/java/com/claudej/application/common/assembler/PageAssembler.java`
- **验证命令**：`mvn compile -pl claude-j-application`
- **预期输出**：编译通过
- **commit**：`feat(application): 分页 DTO 与转换器`

### 5.1 Application Service 分页方法（LinkApplicationService）
- **文件**：`claude-j-application/src/main/java/com/claudej/application/link/service/LinkApplicationService.java`
- **测试**：`claude-j-application/src/test/java/com/claudej/application/link/service/LinkApplicationServiceTest.java`
- **骨架**：
  ```java
  @Test
  void should_return_paged_links_when_page_request_valid() {
      // Arrange: mock repo.findAll(pageRequest) returns Page<Link>
      // Act: service.getAllLinks(pageRequest)
      // Assert: verify(repo).findAll(pageRequest) + PageDTO 正确转换
  }
  ```
- **验证命令**：`mvn test -pl claude-j-application -Dtest=LinkApplicationServiceTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(application): LinkApplicationService 分页方法`

### 5.2 Application Service 分页方法（其他 Service）
- **文件**：
  - `UserApplicationService.java`
  - `CouponApplicationService.java`
  - `OrderApplicationService.java`
- **测试**：对应 ServiceTest
- **验证命令**：`mvn test -pl claude-j-application`
- **预期输出**：全部通过
- **commit**：`feat(application): 其他 ApplicationService 分页方法`

### 6.1 Infrastructure PageHelper
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/common/persistence/PageHelper.java`
- **验证命令**：`mvn compile -pl claude-j-infrastructure`
- **预期输出**：编译通过
- **commit**：`feat(infrastructure): MyBatis-Plus Page 转换工具`

### 7.1 Infrastructure RepositoryImpl 分页实现（LinkRepositoryImpl）
- **文件**：`claude-j-infrastructure/src/main/java/com/claudej/infrastructure/link/persistence/repository/LinkRepositoryImpl.java`
- **测试**：`claude-j-infrastructure/src/test/java/com/claudej/infrastructure/link/persistence/repository/LinkRepositoryImplIT.java`
- **骨架**：
  ```java
  @Test
  void should_return_paged_links_from_database() {
      // 使用 H2，插入 30 条数据
      // 调用 findAll(new PageRequest(0, 10))
      // 断言 content.size == 10, totalElements == 30
  }
  ```
- **验证命令**：`mvn test -pl claude-j-infrastructure -Dtest=LinkRepositoryImplIT`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(infrastructure): LinkRepositoryImpl 分页实现`

### 7.2 Infrastructure RepositoryImpl 分页实现（其他）
- **文件**：其他 3 个 RepositoryImpl
- **测试**：对应 IT
- **验证命令**：`mvn test -pl claude-j-infrastructure`
- **预期输出**：全部通过
- **commit**：`feat(infrastructure): 其他 RepositoryImpl 分页实现`

### 8.1 Adapter PageResponse + PageRequestAdapter
- **文件**：
  - `claude-j-adapter/src/main/java/com/claudej/adapter/common/response/PageResponse.java`
- **验证命令**：`mvn compile -pl claude-j-adapter`
- **预期输出**：编译通过
- **commit**：`feat(adapter): 分页响应对象`

### 9.1 Adapter Controller 分页改造（LinkController）
- **文件**：`claude-j-adapter/src/main/java/com/claudej/adapter/link/web/LinkController.java`
- **测试**：`claude-j-adapter/src/test/java/com/claudej/adapter/link/web/LinkControllerTest.java`
- **骨架**：
  ```java
  @Test
  void should_return_paged_links_when_request_valid() {
      // MockMvc GET /api/v1/links?page=0&size=10
      // 断言 status 200 + PageResponse 结构
  }
  @Test
  void should_return_400_when_page_is_negative() { ... }
  @Test
  void should_return_400_when_size_exceeds_max() { ... }
  ```
- **验证命令**：`mvn test -pl claude-j-adapter -Dtest=LinkControllerTest`
- **预期输出**：`Tests run: X, Failures: 0, Errors: 0`
- **commit**：`feat(adapter): LinkController 分页改造`

### 9.2 Adapter Controller 分页改造（其他）
- **文件**：其他 4 个 Controller
- **测试**：对应 ControllerTest
- **验证命令**：`mvn test -pl claude-j-adapter`
- **预期输出**：全部通过
- **commit**：`feat(adapter): 其他 Controller 分页改造`

### 10.1 全量测试验证
- **验证命令**：`mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
- **预期输出**：三项全过
- **commit**：无需额外 commit（前面已 commit）

## 开发完成记录
<!-- dev 完成后填写 -->
- 全量 `mvn clean test`：Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
- 架构合规检查：checkstyle pass + entropy-check pass (0 issues, 12 warnings)
- 三项预飞：mvn test + checkstyle + entropy-check 全通过
- 通知 @qa 时间：2026-04-23

## QA 验收记录
<!-- qa 验收后填写 -->
- 全量测试（含集成测试）：待填写
- 代码审查结果：待填写
- 代码风格检查：待填写
- 问题清单：详见 test-report.md
- **最终状态**：待填写