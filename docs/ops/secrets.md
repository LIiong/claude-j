# Secrets 运维文档

本文档说明 claude-j 项目敏感配置的运维管理方式。

## 环境变量配置

### JWT_SECRET（必需）

**用途**: JWT Token 签名密钥

**要求**:
- 必须设置
- 长度 >= 32 字符
- 生产环境应使用随机生成的强密钥

**配置示例**:

```bash
# Linux/macOS
export JWT_SECRET="your-secure-random-key-at-least-32-bytes"

# Docker
-e JWT_SECRET="your-secure-random-key-at-least-32-bytes"

# Kubernetes (Secret)
kubectl create secret generic jwt-secret \
  --from-literal=JWT_SECRET="your-secure-random-key-at-least-32-bytes"
```

**生成强密钥**:
```bash
# 使用 openssl 生成 32 字节随机密钥
openssl rand -base64 32

# 或使用 /dev/urandom
head -c 32 /dev/urandom | base64
```

## 配置分层策略

| 环境 | 配置来源 | 说明 |
|------|---------|------|
| 生产 (prod) | 环境变量 `JWT_SECRET` | 强制依赖，无默认值 |
| 开发 (dev) | `application-dev.yml` | 提供本地默认值，便于调试 |
| CI | 环境变量 `JWT_SECRET` | 测试密钥已注入 CI Pipeline |

## 本地开发

开发环境使用 `application-dev.yml` 中的默认值，无需手动设置环境变量：

```yaml
jwt:
  secret: dev-jwt-secret-key-at-least-32-bytes-for-local
```

启动开发服务:
```bash
mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev
```

## 生产部署

### 1. 生成生产密钥
```bash
export JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET length: ${#JWT_SECRET}"
```

### 2. Docker 部署
```bash
docker run -e JWT_SECRET="$JWT_SECRET" -p 8080:8080 claude-j:latest
```

### 3. Kubernetes 部署
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - name: app
        env:
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: JWT_SECRET
```

### 4. 密钥轮换

JWT Secret 轮换需要谨慎处理，因为会影响已签发的 Token：

1. **渐进式轮换**:
   - 保持旧密钥验证现有 Token
   - 使用新密钥签发新 Token
   - 等待所有旧 Token 过期后移除旧密钥

2. **强制重新登录**:
   - 直接更换密钥
   - 所有现有 Token 立即失效
   - 用户需要重新登录

## 故障排查

### 启动失败: "JWT_SECRET environment variable is required"

**原因**: 未设置 JWT_SECRET 环境变量

**解决**:
```bash
export JWT_SECRET="your-secret-key-at-least-32-bytes"
```

### 启动失败: "JWT_SECRET must be at least 32 characters"

**原因**: 密钥长度不足 32 字符

**解决**: 使用更长的密钥
```bash
export JWT_SECRET=$(openssl rand -base64 32)
```

### Token 验证失败

**原因**: JWT_SECRET 与签发时不一致

**解决**: 确认生产/测试环境使用的密钥相同

## 安全建议

1. **永远不要**将生产密钥提交到代码仓库
2. **定期轮换**密钥（建议每 90 天）
3. **使用密钥管理系统**（如 HashiCorp Vault、AWS Secrets Manager）
4. **限制密钥访问权限**，遵循最小权限原则
5. **监控异常登录**，发现密钥泄露立即轮换

## Vault 集成（可选）

项目可扩展支持 Vault 动态密钥:

```yaml
# application-vault.yml
spring:
  cloud:
    vault:
      host: vault.example.com
      port: 8200
      scheme: https
      authentication: KUBERNETES

jwt:
  secret: ${vault://secret/jwt#secret}
```

需要添加 `spring-cloud-starter-vault-config` 依赖。
