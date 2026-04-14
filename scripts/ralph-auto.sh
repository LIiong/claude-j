#!/bin/bash
# =================================================================
# Ralph Auto — 全自动需求交付
# 给一个需求描述，自动走完 Spec → Review → Build → Verify → Ship
#
# 用法:
#   ./scripts/ralph-auto.sh <task-id> "需求描述"
#   ./scripts/ralph-auto.sh 003-user-management "实现用户注册、登录、个人信息管理"
#
# 可选参数:
#   --skip-review        跳过架构评审
#   --max-iterations N   每阶段最大迭代数（默认 20）
#   --dev-only           只执行到 Build 完成，不自动 QA
# =================================================================

set -uo pipefail

# ─── 颜色 ───
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ─── 参数解析 ───
TASK_ID=""
REQUIREMENT=""
SKIP_REVIEW=false
MAX_ITERATIONS=20
DEV_ONLY=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-review)   SKIP_REVIEW=true; shift ;;
        --dev-only)      DEV_ONLY=true; shift ;;
        --max-iterations) MAX_ITERATIONS="$2"; shift 2 ;;
        -*)              echo "未知选项: $1"; exit 1 ;;
        *)
            if [ -z "$TASK_ID" ]; then
                TASK_ID="$1"
            elif [ -z "$REQUIREMENT" ]; then
                REQUIREMENT="$1"
            fi
            shift ;;
    esac
done

if [ -z "$TASK_ID" ] || [ -z "$REQUIREMENT" ]; then
    echo "用法: $0 <task-id> \"需求描述\" [选项]"
    echo ""
    echo "  task-id:     任务标识（如 003-user-management）"
    echo "  需求描述:     一段描述需求的文字"
    echo ""
    echo "选项:"
    echo "  --skip-review        跳过架构评审"
    echo "  --max-iterations N   每阶段最大迭代数（默认 20）"
    echo "  --dev-only           只执行到 Build 完成"
    echo ""
    echo "示例:"
    echo "  $0 003-user-management \"实现用户注册、登录、个人信息管理功能\""
    echo "  $0 004-payment --skip-review \"集成支付宝支付\""
    exit 1
fi

cd "$(dirname "$0")/.."

TASK_DIR="docs/exec-plan/active/$TASK_ID"

# ─── 前置检查 ───
if ! command -v claude >/dev/null 2>&1; then
    echo -e "${RED}❌ claude CLI 未找到${NC}"
    exit 1
fi

# ─── 辅助函数 ───

phase_header() {
    echo ""
    echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  $1${NC}"
    echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
    echo ""
}

check_exit() {
    if [ $1 -ne 0 ]; then
        echo -e "${RED}❌ $2 失败（exit code: $1）${NC}"
        echo -e "${YELLOW}   请检查 ${TASK_DIR}/ 下的文档了解详情${NC}"
        exit 1
    fi
}

# ─── 阶段 1: Spec ───
phase_header "阶段 1/5: Spec — 需求拆解与领域设计"

if [ -d "$TASK_DIR" ] && [ -f "$TASK_DIR/requirement-design.md" ] && [ -f "$TASK_DIR/task-plan.md" ]; then
    echo -e "${YELLOW}⏭  Spec 文档已存在，跳过${NC}"
else
    echo -e "任务: ${TASK_ID}"
    echo -e "需求: ${REQUIREMENT}"
    echo ""

    claude --print -p "
你正在执行 /dev-spec ${TASK_ID}

需求描述：${REQUIREMENT}

请严格按照 /dev-spec skill 的步骤执行：
1. 创建任务目录 docs/exec-plan/active/${TASK_ID}/
2. 需求分析和领域建模
3. 填写 requirement-design.md（领域分析、API、DDL）
4. 填写 task-plan.md（任务拆解）
5. 创建 dev-log.md
6. 创建 handoff.md（to: architect, status: pending-review）

完成后 git commit 所有文件。
" 2>&1

    check_exit $? "Spec 阶段"
fi

# 验证 Spec 产出
for f in requirement-design.md task-plan.md; do
    if [ ! -f "$TASK_DIR/$f" ]; then
        echo -e "${RED}❌ Spec 产出缺失: $TASK_DIR/$f${NC}"
        exit 1
    fi
done
echo -e "${GREEN}✅ Spec 完成${NC}"

# ─── 阶段 2: Architect Review ───
if [ "$SKIP_REVIEW" = true ]; then
    phase_header "阶段 2/5: Review — 已跳过（--skip-review）"

    # 直接将 handoff 设为 approved
    if [ -f "$TASK_DIR/handoff.md" ]; then
        sed -i '' 's/status: pending-review/status: approved/' "$TASK_DIR/handoff.md" 2>/dev/null || true
    fi
else
    phase_header "阶段 2/5: Review — 架构评审"

    claude --agent architect --print -p "
你正在执行 /architect-review ${TASK_ID}

请阅读 docs/exec-plan/active/${TASK_ID}/requirement-design.md 并进行架构评审。
按照 7 项检查清单逐项评审，在 requirement-design.md 追加「架构评审」章节。
更新 handoff.md 状态为 approved 或 changes-requested。
完成后 git commit。
" 2>&1

    check_exit $? "Review 阶段"

    # 检查评审结果
    if [ -f "$TASK_DIR/handoff.md" ]; then
        STATUS=$(grep "^status:" "$TASK_DIR/handoff.md" 2>/dev/null | head -1 | cut -d: -f2 | tr -d ' ')
        if [ "$STATUS" = "changes-requested" ]; then
            echo -e "${RED}❌ 架构评审要求修改，请手动处理后重新运行${NC}"
            echo -e "   查看评审意见: $TASK_DIR/requirement-design.md"
            exit 1
        fi
    fi
fi
echo -e "${GREEN}✅ Review 完成${NC}"

# ─── 阶段 3: Build（Ralph Loop） ───
phase_header "阶段 3/5: Build — TDD 开发（Ralph Loop）"

# 初始化 progress.md
if [ ! -f "$TASK_DIR/progress.md" ]; then
    ./scripts/ralph-init.sh "$TASK_DIR"
    check_exit $? "Ralph Init"
fi

# 启动 Ralph Loop for dev
./scripts/ralph-loop.sh dev "$TASK_DIR" "$MAX_ITERATIONS"
BUILD_EXIT=$?

if [ $BUILD_EXIT -ne 0 ]; then
    # Ralph Loop 可能因 stall 退出，检查是否还有任务
    REMAINING=$(grep -c '^\- \[ \]' "$TASK_DIR/progress.md" 2>/dev/null || echo 0)
    if [ "$REMAINING" -gt 0 ]; then
        echo -e "${RED}❌ Build 未完成，剩余 ${REMAINING} 个任务${NC}"
        echo -e "   查看进度: $TASK_DIR/progress.md"
        exit 1
    fi
fi
echo -e "${GREEN}✅ Build 完成${NC}"

# ─── 阶段 4: Verify（Ralph Loop） ───
if [ "$DEV_ONLY" = true ]; then
    phase_header "阶段 4/5: Verify — 已跳过（--dev-only）"
    echo -e "${GREEN}✅ 开发完成，跳过 QA 验证${NC}"
else
    phase_header "阶段 4/5: Verify — QA 验收（Ralph Loop）"

    # 重置 progress.md 为 QA 阶段任务
    QA_PROGRESS="$TASK_DIR/progress-qa.md"
    TIMESTAMP=$(date '+%Y-%m-%dT%H:%M:%S')
    cat > "$QA_PROGRESS" << EOF
# QA 验收进度

## 当前状态
- 阶段：qa-verify
- 当前迭代：0
- 最后更新：${TIMESTAMP}

## 任务清单
- [ ] 1. 独立重跑三项预飞检查（mvn test + checkstyle + entropy-check）
- [ ] 2. 编写测试用例设计（test-case-design.md）
- [ ] 3. 编写集成测试（start 模块）
- [ ] 4. 代码审查（聚合设计、值对象、转换链、异常处理）
- [ ] 5. 编写测试报告（test-report.md）
- [ ] 6. 更新 handoff.md（approved 或 changes-requested）

## 迭代日志
EOF

    # 备份 dev 的 progress.md，用 QA 的替换
    mv "$TASK_DIR/progress.md" "$TASK_DIR/progress-dev.md" 2>/dev/null || true
    mv "$QA_PROGRESS" "$TASK_DIR/progress.md"

    ./scripts/ralph-loop.sh qa "$TASK_DIR" "$MAX_ITERATIONS"
    QA_EXIT=$?

    # 恢复 dev progress 供参考
    mv "$TASK_DIR/progress.md" "$TASK_DIR/progress-qa.md" 2>/dev/null || true
    mv "$TASK_DIR/progress-dev.md" "$TASK_DIR/progress.md" 2>/dev/null || true

    if [ $QA_EXIT -ne 0 ]; then
        echo -e "${YELLOW}⚠️  QA 阶段未正常完成，请检查测试报告${NC}"
    fi

    # 检查验收结果
    if [ -f "$TASK_DIR/test-report.md" ]; then
        if grep -q "验收通过" "$TASK_DIR/test-report.md"; then
            echo -e "${GREEN}✅ QA 验收通过${NC}"
        else
            echo -e "${RED}❌ QA 验收未通过，请查看: $TASK_DIR/test-report.md${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}⚠️  未找到 test-report.md${NC}"
    fi
fi

# ─── 阶段 5: Ship ───
if [ "$DEV_ONLY" = false ]; then
    phase_header "阶段 5/5: Ship — 归档"

    claude --print -p "
你正在执行 /qa-ship ${TASK_ID}

请：
1. 确认 handoff.md status: approved 且 test-report.md 含'验收通过'
2. 移动 docs/exec-plan/active/${TASK_ID}/ 到 docs/exec-plan/archived/${TASK_ID}/
3. 检查是否有新聚合，更新 CLAUDE.md 聚合列表
4. git commit 归档变更
" 2>&1

    echo -e "${GREEN}✅ Ship 完成${NC}"
fi

# ─── 总结 ───
echo ""
echo -e "${GREEN}╔══════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║  🎉 需求交付完成: ${TASK_ID}${NC}"
echo -e "${GREEN}╚══════════════════════════════════════════╝${NC}"
echo ""
echo "  产出物:"
if [ "$DEV_ONLY" = false ]; then
    echo "    📁 docs/exec-plan/archived/${TASK_ID}/"
else
    echo "    📁 docs/exec-plan/active/${TASK_ID}/"
fi
echo "    📝 requirement-design.md  — 需求设计"
echo "    📋 task-plan.md           — 任务计划"
echo "    📓 dev-log.md             — 开发日志"
[ "$DEV_ONLY" = false ] && echo "    🧪 test-report.md         — 测试报告"
echo ""
echo "  git log --oneline -20  # 查看提交历史"
