# Handoff — 011-flyway-migration

## 任务信息

- **task-id**: 011-flyway-migration
- **task-name**: 引入 Flyway 数据库迁移框架
- **from**: architect
- **to**: dev
- **status**: approved

## 阶段说明

Spec 阶段完成，等待架构评审。

## 交付物

| 文件 | 路径 | 说明 |
|------|------|------|
| 需求设计 | docs/exec-plan/active/011-flyway-migration/requirement-design.md | 含 V1-V7 migration 拆分方案 |
| 任务计划 | docs/exec-plan/active/011-flyway-migration/task-plan.md | 19 项原子任务 |
| 开发日志 | docs/exec-plan/active/011-flyway-migration/dev-log.md | 初始化（空） |

## 关键设计决策（待评审确认）

1. **版本号策略**: 采用线性整数版本 V1-V7（非时间戳），按聚合初始化顺序排列
2. **聚合划分**: user→order→shortlink→link→coupon→cart→auth（无跨聚合依赖）
3. **Flyway 版本**: 选用 8.5.13（Spring Boot 2.7 内置兼容版本）
4. **baseline 策略**: 新库启用 `baseline-on-migrate: true`，首次启动自动基线化

## 影响范围

- 父 pom.xml（添加 flyway-core 依赖管理）
- claude-j-start pom.xml（添加 flyway-core 依赖）
- application-dev.yml（移除 spring.sql.init，启用 flyway）
- 删除 schema.sql，新增 db/migration/V1-V7__*.sql
- 新增 docs/ops/db-migrations.md
- CI workflow（新增 flyway:validate job）

## 验收标准（需 QA 后续验证）

1. 空库启动自动建表
2. flyway_schema_history 表可见 7 条记录
3. 修改历史 migration 后 validate 失败
4. 三项预飞检查通过

## Summary

**评审结论**: approved
**关键发现**:
- V1-V7 聚合划分合理，与 schema.sql 表结构完全对应
- Flyway 8.5.13 与 Spring Boot 2.7 兼容
- baseline 策略适用于新库场景
- 19 项原子任务粒度合格，可执行性高

**评审通过时间**: 2026-04-18
**可进入阶段**: Build（开发实现）
