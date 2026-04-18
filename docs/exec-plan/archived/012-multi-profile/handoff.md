# Handoff: 012-multi-profile

## 当前状态

```yaml
task-id: 012-multi-profile
from: qa
to: dev
status: approved
pre-flight:
  mvn-test: pass       # 142 tests run, 0 failures, 0 errors
  checkstyle: pass     # Exit 0
  entropy-check: pass  # 0 FAIL, 12 WARN (已有问题)
qa-verify:
  mvn-test: pass       # 142 tests run, 0 failures, 0 errors
  checkstyle: pass     # Exit 0
  entropy-check: pass  # 0 FAIL
  test-report: pass    # 验收通过
```

## 变更摘要
- 重构 application.yml：移除 profile 激活，仅保留通用配置
- 新增 application-staging.yml：MySQL + 占位变量 + INFO 日志 + 受限 actuator
- 新增 application-prod.yml：MySQL + 连接池优化 + WARN 日志 + 最小 actuator
- 更新 Dockerfile：ENV SPRING_PROFILES_ACTIVE=prod
- 新增 docs/ops/profiles.md：5 维配置矩阵 + 环境变量清单 + 故障排查
- 添加 actuator 依赖 + MySQL 驱动

## 验证记录
- dev 启动测试：✅ 通过（actuator/health UP）
- staging 缺 env 报错：✅ 通过（明确指向 ${MYSQL_HOST}）
- 三项预飞：✅ 全部通过
- QA 独立验证：✅ 全部通过

## 问题清单
- Critical: 0
- Major: 0
- Minor: 0

## 下一步
执行 Ship 阶段，任务归档
