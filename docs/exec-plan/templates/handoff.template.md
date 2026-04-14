---
task-id: "{task-id}"
from: dev
to: architect
status: pending-review
timestamp: "{YYYY-MM-DDTHH:MM:SS}"
pre-flight:
  mvn-test: pending
  checkstyle: pending
  entropy-check: pending
artifacts:
  - requirement-design.md
  - task-plan.md
summary: ""
---

# 交接文档

> 每次 Agent 间交接时更新此文件。
> 状态流转：pending-review → approved / changes-requested

## 交接说明
{发送方填写：本次交接的内容摘要、需要关注的重点、已知风险}

## 评审回复
{接收方填写：评审意见、问题清单、通过/待修改结论}

---

## 交接历史

### {日期} — @dev → @architect
- 状态：pending-review
- 说明：{设计评审请求}

### {日期} — @architect → @dev
- 状态：approved / changes-requested
- 说明：{评审结论}

### {日期} — @dev → @qa
- 状态：pending-review
- Pre-flight：mvn-test: pass | checkstyle: pass | entropy-check: pass
- 说明：{验收请求}

### {日期} — @qa → (Ship)
- 状态：approved
- 说明：{验收通过，归档}
