#!/bin/bash
#
# Dev Gate - 开发准入检查
# 用法: ./scripts/dev-gate.sh [task-id]
# 返回值: 0=通过, 1=阻塞
#

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

TASK_ID="${1:-}"
PLAN_DIR="docs/exec-plan/active"

# 如果没有指定task-id，尝试自动检测
if [ -z "$TASK_ID" ]; then
    # 查找最新的活跃任务（按目录名排序）
    LATEST_TASK=$(ls -1 "$PLAN_DIR" 2>/dev/null | grep -E '^[0-9]+-' | sort -V | tail -1)
    if [ -n "$LATEST_TASK" ]; then
        TASK_ID="$LATEST_TASK"
        echo -e "${YELLOW}未指定任务ID，自动检测: $TASK_ID${NC}"
    fi
fi

if [ -z "$TASK_ID" ]; then
    echo -e "${RED}❌ DEV GATE 阻塞${NC}"
    echo ""
    echo "未找到活跃任务目录。"
    echo ""
    echo "请按以下步骤操作："
    echo ""
    echo "  1. 创建执行计划目录:"
    echo "     mkdir docs/exec-plan/active/00X-task-name"
    echo ""
    echo "  2. 复制并填写模板:"
    echo "     cp docs/exec-plan/templates/requirement-design.template.md \\"
    echo "        docs/exec-plan/active/00X-task-name/requirement-design.md"
    echo "     cp docs/exec-plan/templates/task-plan.template.md \\"
    echo "        docs/exec-plan/active/00X-task-name/task-plan.md"
    echo ""
    echo "  3. 填写完成后，更新状态:"
    echo "     ./scripts/dev-gate.sh --approve-spec 00X-task-name"
    echo ""
    exit 1
fi

TASK_DIR="$PLAN_DIR/$TASK_ID"

# 检查目录是否存在
if [ ! -d "$TASK_DIR" ]; then
    echo -e "${RED}❌ DEV GATE 阻塞${NC}"
    echo ""
    echo "任务目录不存在: $TASK_DIR"
    exit 1
fi

echo "=========================================="
echo "  Dev Gate 检查 - $TASK_ID"
echo "=========================================="
echo ""

# 检查1: requirement-design.md 是否存在
if [ ! -f "$TASK_DIR/requirement-design.md" ]; then
    echo -e "${RED}❌ 缺少需求设计文档${NC}"
    echo "   文件: $TASK_DIR/requirement-design.md"
    MISSING=1
else
    echo -e "${GREEN}✅ 需求设计文档存在${NC}"
fi

# 检查2: task-plan.md 是否存在
if [ ! -f "$TASK_DIR/task-plan.md" ]; then
    echo -e "${RED}❌ 缺少任务计划文档${NC}"
    echo "   文件: $TASK_DIR/task-plan.md"
    MISSING=1
else
    echo -e "${GREEN}✅ 任务计划文档存在${NC}"
fi

# 检查3: requirement-design.md 是否有实质内容（非空模板）
if [ -f "$TASK_DIR/requirement-design.md" ]; then
    # 统计非注释、非空行的数量（排除Markdown注释）
    CONTENT_LINES=$(cat "$TASK_DIR/requirement-design.md" | sed '/^#/d' | sed '/^$/d' | sed '/<!--/d' | sed '/-->/d' | wc -l | tr -d ' ')

    if [ "$CONTENT_LINES" -lt 10 ]; then
        echo -e "${RED}❌ 需求设计文档内容过少 (${CONTENT_LINES} 行有效内容)${NC}"
        echo "   请填写实质性的设计内容，而不仅仅是保留模板注释"
        INCOMPLETE=1
    else
        echo -e "${GREEN}✅ 需求设计文档内容充实 (${CONTENT_LINES} 行有效内容)${NC}"
    fi
fi

# 检查4: handoff.md 状态
if [ -f "$TASK_DIR/handoff.md" ]; then
    STATUS=$(grep "^status:" "$TASK_DIR/handoff.md" | head -1 | cut -d: -f2 | tr -d ' ')
    echo ""
    echo "当前状态: $STATUS"

    case "$STATUS" in
        "spec-complete"|"approved"|"coding-in-progress")
            echo -e "${GREEN}✅ 状态允许编码${NC}"
            ;;
        "pending-review")
            echo -e "${YELLOW}⚠️ 等待架构评审，编码可能受限${NC}"
            ;;
        "changes-requested")
            echo -e "${RED}❌ 评审要求修改，请先修改设计${NC}"
            BLOCKED=1
            ;;
        *)
            echo -e "${YELLOW}⚠️ 未知状态，建议先更新 handoff.md${NC}"
            ;;
    esac
else
    echo ""
    echo -e "${YELLOW}⚠️ 未找到 handoff.md，建议创建${NC}"
fi

echo ""
echo "=========================================="

if [ -n "$MISSING" ] || [ -n "$INCOMPLETE" ] || [ -n "$BLOCKED" ]; then
    echo -e "${RED}❌ DEV GATE 阻塞 - 不能开始编码${NC}"
    echo "=========================================="
    echo ""
    echo "请完成以下事项："
    [ -n "$MISSING" ] && echo "  1. 创建缺失的文档文件"
    [ -n "$INCOMPLETE" ] && echo "  2. 填写需求设计文档的实质内容"
    [ -n "$BLOCKED" ] && echo "  3. 解决评审提出的问题"
    echo ""
    echo "完成后重新运行: ./scripts/dev-gate.sh $TASK_ID"
    exit 1
fi

echo -e "${GREEN}✅ DEV GATE 通过 - 可以开始编码${NC}"
echo "=========================================="
exit 0
