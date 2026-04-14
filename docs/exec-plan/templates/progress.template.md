# 任务进度

> Ralph Loop 核心状态文件。每次迭代后 Agent 必须更新此文件。
> 下次迭代的全新 Agent 会话通过读取此文件恢复上下文。

## 当前状态
- 阶段：{dev-spec | architect-review | dev-build | qa-verify | shipped}
- 当前迭代：{N}
- 最后更新：{YYYY-MM-DDTHH:MM:SS}

## 任务清单

> 使用 checkbox 格式。完成后标记 [x] 并附 commit hash。
> ralph-loop.sh 通过读取第一个 [ ] 项确定下一个任务。

- [x] 1. Domain：定义聚合根（示例，commit abc1234）
- [ ] 2. Domain：定义值对象 ← 当前
- [ ] 3. Domain：定义 Repository 端口
- [ ] 4. Application：创建命令 + DTO
- [ ] 5. Application：创建应用服务
- [ ] 6. Infrastructure：创建 DO + Mapper
- [ ] 7. Infrastructure：实现 Repository
- [ ] 8. Adapter：创建 Controller + Request/Response
- [ ] 9. Start：DDL + 配置
- [ ] 10. 各层编写测试
- [ ] 11. 验证：mvn test + checkstyle + entropy-check

## 迭代日志

> 每次迭代结束时追加一条记录。
> 下次迭代的 Agent 通过此日志了解之前发生了什么。

### 迭代 1 ({timestamp})
- 完成：{完成的任务编号}
- 遇到问题：{问题描述，无则写"无"}
- 下次应做：{建议下次迭代处理的任务}
