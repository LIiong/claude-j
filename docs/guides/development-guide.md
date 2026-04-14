# 开发流程文档

## 1. 环境搭建

### 前置要求
- JDK 8+（项目使用 Java 8 源码兼容）
- Maven 3.6+
- IDE：IntelliJ IDEA（推荐）+ Lombok 插件

### 构建与运行
```bash
mvn clean install                                                      # 全量构建
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev  # H2 开发环境运行
mvn test                                                               # 运行所有测试
mvn test -pl claude-j-domain                                           # 仅领域层测试
```

## 2. Dev-QA-Architect 协作工作流

```
┌─────────┐    ┌─────────────────────────────────────────────────┐
│   用户   │───►│ 提供需求任务                                     │
└─────────┘    └──────────────────┬──────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│ @dev：需求分析 & 任务拆解                                         │
│ → 创建 docs/exec-plan/active/{task-id}/                         │
│ → 编写 requirement-design.md + task-plan.md                      │
│ → 创建 handoff.md（to: architect）                               │
└──────────────────┬──────────────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────────┐
│ @architect：设计评审                                              │
│ → 交叉验证设计 vs 架构文档 + ADR + 已有聚合                        │
│ → 在 requirement-design.md 追加「架构评审」章节                     │
│ → 更新 handoff.md（approved / changes-requested）                │
└──────────────────┬──────────────────────────────────────────────┘
                   │ approved
                   ▼
┌─────────────────────────────────────────────────────────────────┐
│ @dev：TDD — 编写单测 → 编码开发 → 验证测试                         │
│ → 在 dev-log.md 记录问题与决策                                    │
│ → 更新 task-plan.md 任务状态                                     │
│ → 三项预飞检查通过 → 更新 handoff.md（to: qa）                    │
└──────────────────┬──────────────────────────────────────────────┘
                   │ 预飞通过 → 标记"待验收"
                   ▼
┌─────────────────────────────────────────────────────────────────┐
│ @qa：独立重跑三项检查 → 测试用例设计 → 执行测试 → 代码 Review       │
│ → 编写 test-case-design.md                                       │
│ → 记录结果到 test-report.md                                      │
└──────────────────┬──────────────────────────────────────────────┘
                   │
              ┌────┴────┐
              │ 有问题？  │
              └────┬────┘
          是       │     否
           │       │      │
           ▼       │      ▼
┌───────────────┐  │  ┌───────────────────┐
│ @dev：修复     │  │  │ @qa：标记           │
│ → 重新测试     │──┘  │ "验收通过"          │
│ → 通知 @qa    │     │ → 归档任务目录       │
└───────────────┘     └───────────────────┘
```

### Ralph Loop（Agent 持续运行）

对于大型任务，可使用 Ralph Loop 让 Agent 在循环中持续运行：

```bash
# 1. @dev 完成 Spec 阶段后初始化
./scripts/ralph-init.sh docs/exec-plan/active/{task-id}/

# 2. 启动 @dev 循环（自动迭代编码）
./scripts/ralph-loop.sh dev docs/exec-plan/active/{task-id}/

# 3. @dev 完成后启动 @qa 循环
./scripts/ralph-loop.sh qa docs/exec-plan/active/{task-id}/
```

**机制**：每次迭代启动全新 Claude 会话，通过 `progress.md` + git history 恢复上下文。

## 3. 新功能开发流程

### 逐步指南
1. **领域层**（`claude-j-domain`）：
   - 定义聚合根及其业务方法
   - 定义实体和值对象
   - 定义 Repository 端口接口
   - 定义领域事件（如需跨聚合）
   - 编写领域单元测试

2. **应用层**（`claude-j-application`）：
   - 创建命令/查询对象
   - 创建 DTO
   - 创建组装器（MapStruct：Domain ↔ DTO）
   - 创建应用服务
   - 编写应用层单元测试

3. **基础设施层**（`claude-j-infrastructure`）：
   - 创建数据对象（DO）+ MyBatis-Plus 注解
   - 创建 MyBatis-Plus Mapper 接口
   - 创建转换器（MapStruct：DO ↔ Domain）
   - 实现 Repository 适配器
   - 编写集成测试

4. **适配器层**（`claude-j-adapter`）：
   - 创建请求/响应对象
   - 创建 REST 控制器
   - 编写 API 测试

5. **启动模块**（`claude-j-start`）：
   - 在 `db/schema.sql` 添加 DDL
   - 必要时更新配置

## 4. 常见开发场景

### 新增聚合根
按上述完整新功能开发流程执行。聚合根是核心 — 先设计好所有业务不变量。

### 新增 API 端点
如果聚合已存在：
1. 在应用层添加命令/DTO
2. 在应用服务中添加方法
3. 在适配器层控制器中添加端点

### 修改领域逻辑
1. 更新领域模型（聚合方法、值对象）
2. 更新领域测试覆盖新行为
3. 检查应用服务是否需要变更
4. 检查基础设施层 DO 映射是否需要更新

## 5. Git 规范

### 分支策略
- `main` — 稳定发布分支
- `develop` — 集成分支
- `feature/{task-id}-{description}` — 功能分支
- `fix/{task-id}-{description}` — 修复分支

### 提交信息格式
```
{type}({scope}): {description}

{body}
```
类型：`feat`、`fix`、`refactor`、`test`、`docs`、`chore`
范围：`domain`、`application`、`infrastructure`、`adapter`、`start`

示例：
```
feat(domain): 添加 Order 聚合根及创建、取消操作

- Order 聚合根封装业务不变量
- OrderItem 实体、Money 值对象
- OrderRepository 端口接口
```
