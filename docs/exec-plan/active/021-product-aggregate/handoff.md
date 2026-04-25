---
task-id: "021-product-aggregate"
from: dev
to: architect
status: pending-review
timestamp: "2026-04-25T14:30:00"
pre-flight:
  mvn-test: pending            # Build 阶段填写
  checkstyle: pending           # Build 阶段填写
  entropy-check: pass           # Spec 阶段已执行 (exit 0, 12 WARN, 0 FAIL)
  tdd-evidence: []              # Build 阶段填写
artifacts:
  - requirement-design.md
  - task-plan.md
  - test-case-design.md
summary: "Spec 阶段完成（已修复 architect 评审发现的问题）：AC-2 定价规则歧义已明确决策（上架后允许调价，禁止负数/零）、test-case-design.md 已补充（6 条 AC 全部映射到自动化测试）。待 architect 重新评审。"
---

# 交接文档

> 每次 Agent 间交接时更新此文件。
> 状态流转：pending-review → approved / changes-requested

## 交接说明

**Spec 阶段产出**：
1. `requirement-design.md` — 领域分析（Product 聚合根 + 3 个值对象 + ProductStatus 枚举 + Repository 端口）、API 设计（创建/查询/调价/上架/下架/分页）、DDL 设计、与 OrderItem 的解耦边界
2. `task-plan.md` — 按 domain→application→infrastructure→adapter→start 拆解 14 个原子任务，含验证命令和预期输出
3. `test-case-design.md` — 分层测试设计（Domain/Application/Infrastructure/Adapter），AC 自动化覆盖矩阵（6 条 AC 全部映射）

**设计要点**：
- SKU 作为值对象嵌入 Product（不独立实体化）
- 定价：原价必填 + 促销价可选（getEffectivePrice 返回有效售价）
- 状态机：DRAFT → ACTIVE → INACTIVE（上架/下架操作）
- 与 OrderItem 解耦：OrderItem 仅持 productId + snapshotPrice

**假设与待确认**（见 requirement-design.md 「假设与待确认」章节）：
1. SKU 单一设计 vs 多 SKU 变体
2. 上架后允许调价（电商常见场景：促销结束恢复原价）
3. 价格范围校验（必须 > 0）

## 修正说明

**修正项 1：AC-2 定价规则歧义已明确**
- 原问题：AC-2 写法「价格调整仅允许 DRAFT 状态（上架后不可调价？需确认）」含歧义标记
- 修正决策：上架后允许调价（电商常见场景如促销结束恢复原价），但禁止调整为负数或零
- 修正后 AC-2：「价格调整允许 DRAFT/ACTIVE/INACTIVE 状态，但禁止调整为负数或零」
- 相关更新：
  - requirement-design.md 验收条件章节已更新
  - 假设与待确认章节新增 #5 条目明确上架后调价规则
  - 需求质量检查项已全部标记为通过

**修正项 2：test-case-design.md 已补充**
- 原问题：缺失测试设计文档，违反可测性检查项「AC 自动化全覆盖」
- 修正内容：
  - AC 自动化覆盖矩阵（6 条 AC 全部映射到自动化测试）
  - Domain 层测试场景（40 个场景：ProductId/ProductName/SKU/ProductStatus/Product）
  - Application 层测试场景（12 个场景：创建/查询/调价/上架/下架/分页）
  - Infrastructure 层测试场景（7 个场景：保存/查询/转换/逻辑删除）
  - Adapter 层测试场景（12 个场景：HTTP 契约 200/400/404）
  - 集成测试场景（3 个场景：完整生命周期）
- 相关更新：
  - 可测性保障检查项已标记为通过

---

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

### 2026-04-25 — @dev → @architect（重新提交）
- 状态：pending-review
- 说明：已修复 2 项必须修改（AC-2 定价规则歧义 + test-case-design.md 补充），请求重新评审

### 2026-04-25 — @architect → @dev
- 状态：changes-requested
- 说明：2 项必须修改，修改后重新提交评审

### 2026-04-25 — @dev → @architect
- 状态：pending-review
- 说明：Spec 阶段完成，请求架构评审