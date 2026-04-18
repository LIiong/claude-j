# 项目变量定义

本文档集中定义移植时需要替换的所有占位变量。模板文件（本目录下 `*.template.md`）中使用 `${VARIABLE_NAME}` 语法引用这些变量。

---

## 使用方法

### 方法 A：手动替换
直接用编辑器的"查找替换"功能将模板中的 `${VARIABLE_NAME}` 替换为实际值。

### 方法 B：脚本替换
```bash
./scripts/bootstrap-project.sh \
  --project-name "my-app" \
  --package-root "com.mycompany" \
  --description "My new DDD project"
```

### 方法 C：envsubst
```bash
export PROJECT_NAME=my-app
export PACKAGE_ROOT=com.mycompany
# ... 其他变量
envsubst < CLAUDE.template.md > CLAUDE.md
```

---

## 变量定义

### 项目身份
| 变量名 | 默认值 | 说明 |
|-------|--------|------|
| `PROJECT_NAME` | `example-app` | 项目名（kebab-case），用于目录、模块名 |
| `PROJECT_DESCRIPTION` | `示例 DDD 项目` | 一句话项目描述 |
| `PROJECT_OWNER` | `team-name` | 团队/所有者名称 |

### 技术栈
| 变量名 | 默认值 | 说明 |
|-------|--------|------|
| `LANGUAGE` | `Java` | 主要编程语言 |
| `LANGUAGE_VERSION` | `Java 8` | 语言版本 |
| `FRAMEWORK` | `Spring Boot 2.7.18` | 主框架 + 版本 |
| `BUILD_TOOL` | `Maven` | 构建工具 |
| `ORM` | `MyBatis-Plus 3.5.5` | 持久化框架 |
| `MAPPER_TOOL` | `MapStruct 1.5.5` | 对象映射工具 |
| `TEST_FRAMEWORK` | `JUnit 5 + AssertJ + Mockito` | 测试栈 |
| `DB_DEV` | `H2` | 开发数据库 |
| `DB_PROD` | `MySQL 8` | 生产数据库 |

### 命名空间
| 变量名 | 默认值 | 说明 |
|-------|--------|------|
| `PACKAGE_ROOT` | `com.example` | 根包名 / 根命名空间 |
| `MODULE_DOMAIN` | `${PROJECT_NAME}-domain` | 领域层模块 |
| `MODULE_APPLICATION` | `${PROJECT_NAME}-application` | 应用层模块 |
| `MODULE_INFRASTRUCTURE` | `${PROJECT_NAME}-infrastructure` | 基础设施模块 |
| `MODULE_ADAPTER` | `${PROJECT_NAME}-adapter` | 适配器模块 |
| `MODULE_START` | `${PROJECT_NAME}-start` | 启动模块 |

### 构建命令
| 变量名 | 默认值（Maven） | 其他栈示例 |
|-------|----------------|-----------|
| `CMD_BUILD_ALL` | `mvn clean install` | `pnpm build` / `gradle build` |
| `CMD_RUN_TESTS` | `mvn test` | `pnpm test` / `pytest` |
| `CMD_RUN_DEV` | `mvn spring-boot:run -pl ${MODULE_START} -Dspring-boot.run.profiles=dev` | `pnpm dev` / `uvicorn main:app --reload` |
| `CMD_STYLE_CHECK` | `mvn checkstyle:check` | `pnpm lint` / `ruff check` |
| `CMD_ARCH_TEST` | `mvn test`（ArchUnit 类） | 见具体栈 |
| `CMD_QUICK_CHECK` | `./scripts/quick-check.sh` | 同名脚本适配 |
| `CMD_ENTROPY_CHECK` | `./scripts/entropy-check.sh` | 同名脚本适配 |

### 架构量化
| 变量名 | 默认值 | 说明 |
|-------|--------|------|
| `ARCHUNIT_RULE_COUNT` | `14` | 架构测试规则数 |
| `ENTROPY_CHECK_COUNT` | `12` | 熵扫描检查项数 |

### 命名约定
| 变量名 | 默认值 | 说明 |
|-------|--------|------|
| `TABLE_PREFIX` | `t_` | 数据库表名前缀 |
| `COLUMN_CASE` | `snake_case` | 列命名风格 |
| `CLASS_CASE` | `PascalCase` | 类命名 |
| `METHOD_CASE` | `camelCase` | 方法/函数命名 |
| `TEST_NAMING_PATTERN` | `should_{行为}_when_{条件}` | 测试命名规范 |

---

## 变量校验

在脚手架运行后，可运行以下命令验证是否还有未替换的占位符：
```bash
grep -rn '\${[A-Z_]*}' CLAUDE.md docs/ .claude/ scripts/ 2>/dev/null | grep -v '.template.md'
```
若输出非空，说明存在未替换变量。
