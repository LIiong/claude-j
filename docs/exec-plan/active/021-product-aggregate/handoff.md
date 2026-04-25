---
task-id: "021-product-aggregate"
from: dev
to: architect
status: changes-requested
timestamp: "2026-04-25T12:00:00"
pre-flight:
  mvn-test: pending            # Build 阶段填写
  checkstyle: pending           # Build 阶段填写
  entropy-check: pass           # Spec 阶段已执行 (exit 0, 12 WARN, 0 FAIL)
  tdd-evidence: []              # Build 阶段填写
artifacts:
  - requirement-design.md
  - task-plan.md
summary: "Spec 阶段完成：Product 聚合领域建模（ProductId/ProductName/SKU 值对象、ProductStatus 状态机、Product 聚合根）、API 设计（6 个端点）、DDL 设计（V8__product_init.sql）、与 OrderItem 解耦边界明确。待 architect 评审。"
---

# 交接文档

> 每次 Agent 间交接时更新此文件。
> 状态流转：pending-review → approved / changes-requested

## 交接说明

**Spec 阶段产出**：
1. `requirement-design.md` — 领域分析（Product 聚合根 + 3 个值对象 + ProductStatus 枚举 + Repository 端口）、API 设计（创建/查询/调价/上架/下架/分页）、DDL 设计、与 OrderItem 的解耦边界
2. `task-plan.md` — 按 domain→application→infrastructure→adapter→start 拆解 14 个原子任务，含验证命令和预期输出

**设计要点**：
- SKU 作为值对象嵌入 Product（不独立实体化）
- 定价：原价必填 + 促销价可选（getEffectivePrice 返回有效售价）
- 状态机：DRAFT → ACTIVE → INACTIVE（上架/下架操作）
- 与 OrderItem 解耦：OrderItem 仅持 productId + snapshotPrice

**待确认**（见 requirement-design.md 「假设与待确认」章节）：
1. SKU 单一设计 vs 多 SKU 变体
2. 上架后是否允许调价
3. 价格范围校验

## 评审回复

**评审人**：@architect
**日期**：2026-04-25
**结论**：❌ changes-requested（2 项必须修改）

### 必须修改项

1. **AC-2 定价规则歧义必须明确**
   - 当前：「价格调整仅允许 DRAFT 状态（上架后不可调价？需确认）」含歧义标记
   - 要求：明确定价规则决策并移除歧义标记
   - 参考：Coupon 聚合设计无歧义标记，验收条件明确

2. **补充 test-case-design.md**
   - 当前缺失，违反可测性检查项「AC 自动化全覆盖」
   - 要求：补充 AC 自动化覆盖矩阵 + 分层测试用例设计
   - 参考：`docs/exec-plan/templates/test-case-design.template.md`

### 建议改进（不阻塞）

- SKU 单一设计假设需用户确认后再开工
- 价格范围校验规则（>=0，无上限）补充到 AC
- ErrorCode 命名参考 `INVALID_COUPON_STATUS_TRANSITION`

### entropy-check.sh 基线

- 退出码：0
- FAIL：0
- WARN：12
- 结论：架构基线通过

详见 requirement-design.md「架构评审」章节。

---

## 交接历史

### 2026-04-25 — @architect → @dev
- 状态：changes-requested
- 说明：2 项必须修改，修改后重新提交评审

### 2026-04-25 — @dev → @architect
- 状态：pending-review
- 说明：Spec 阶段完成，请求架构评审