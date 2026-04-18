# 012-multi-profile 任务计划

## 原子任务

### T1: 重构 application.yml
**验证**: `cat claude-j-start/src/main/resources/application.yml | grep -q "active:" && echo "FAIL: still has active" || echo "PASS"`

### T2: 创建 application-staging.yml
**验证**: `[ -f claude-j-start/src/main/resources/application-staging.yml ] && echo "PASS" || echo "FAIL"`

### T3: 创建 application-prod.yml
**验证**: `[ -f claude-j-start/src/main/resources/application-prod.yml ] && echo "PASS" || echo "FAIL"`

### T4: 更新 Dockerfile
**验证**: `grep -q "SPRING_PROFILES_ACTIVE=prod" docs/devops/Dockerfile && echo "PASS" || echo "FAIL"`

### T5: 创建 profiles.md 文档
**验证**: `[ -f docs/ops/profiles.md ] && echo "PASS" || echo "FAIL"`

### T6: dev 启动测试
**验证**: `mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev &
sleep 10
curl -s http://localhost:8080/actuator/health | grep -q "UP" && echo "PASS" || echo "FAIL"`

### T7: staging 缺 env 报错测试
**验证**: `java -jar ... --spring.profiles.active=staging` 启动失败并提示具体缺失变量

### T8: 三项预飞
**验证**: `mvn test && mvn checkstyle:check && ./scripts/entropy-check.sh`
