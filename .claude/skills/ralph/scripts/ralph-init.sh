#!/bin/bash
# =================================================================
# Ralph Init — Ralph Loop 初始化器
# 在首次启动 Ralph Loop 前运行，创建初始环境
#
# 用法:
#   ./scripts/ralph-init.sh <task-dir>
#
# 示例:
#   ./scripts/ralph-init.sh docs/exec-plan/active/002-order-service
#
# 功能:
#   1. 验证 task-dir 存在且含必要文件
#   2. 从 task-plan.md 生成 progress.md
#   3. 输出就绪状态
# =================================================================

set -euo pipefail

TASK_DIR="${1:-}"

if [ -z "$TASK_DIR" ]; then
    echo "用法: $0 <task-dir>"
    echo ""
    echo "  task-dir: 任务目录路径（如 docs/exec-plan/active/002-order-service）"
    echo ""
    echo "示例:"
    echo "  $0 docs/exec-plan/active/002-order-service"
    exit 1
fi

cd "$(dirname "$0")/.."

# ─── 前置验证 ───

if [ ! -d "$TASK_DIR" ]; then
    echo "❌ 任务目录不存在: $TASK_DIR"
    exit 1
fi

if [ ! -f "$TASK_DIR/requirement-design.md" ]; then
    echo "❌ 缺少需求设计文档: $TASK_DIR/requirement-design.md"
    echo "   请先由 @dev 完成 Spec 阶段"
    exit 1
fi

if [ ! -f "$TASK_DIR/task-plan.md" ]; then
    echo "❌ 缺少任务计划文档: $TASK_DIR/task-plan.md"
    echo "   请先由 @dev 完成 Spec 阶段"
    exit 1
fi

PROGRESS_FILE="$TASK_DIR/progress.md"

if [ -f "$PROGRESS_FILE" ]; then
    echo "⚠️  进度文件已存在: $PROGRESS_FILE"
    echo "   若要重新初始化，请先删除该文件"
    exit 1
fi

# ─── 从 task-plan.md 提取任务列表 ───

echo "📋 解析任务计划: $TASK_DIR/task-plan.md"

# 提取任务表格中的任务描述（匹配 | 编号 | 描述 | ... 格式）
# 跳过表头和分隔线
TASKS=""
TASK_COUNT=0

while IFS= read -r line; do
    # 跳过空行、表头行（含 #/任务/层 等表头词）、分隔线
    if echo "$line" | grep -qE '^\s*$|^\|.*编号|^\|.*#|^\|.*任务|^\|.*---'; then
        continue
    fi
    # 提取第二列（任务描述）和第三列（层/范围）
    TASK_DESC=$(echo "$line" | awk -F'|' '{gsub(/^[ \t]+|[ \t]+$/, "", $3); print $3}' 2>/dev/null)
    TASK_LAYER=$(echo "$line" | awk -F'|' '{gsub(/^[ \t]+|[ \t]+$/, "", $4); print $4}' 2>/dev/null)

    if [ -n "$TASK_DESC" ] && [ "$TASK_DESC" != "" ]; then
        TASK_COUNT=$((TASK_COUNT + 1))
        if [ -n "$TASK_LAYER" ]; then
            TASKS="${TASKS}- [ ] ${TASK_COUNT}. ${TASK_LAYER}：${TASK_DESC}\n"
        else
            TASKS="${TASKS}- [ ] ${TASK_COUNT}. ${TASK_DESC}\n"
        fi
    fi
done < "$TASK_DIR/task-plan.md"

# 如果无法从表格解析，创建基础模板
if [ "$TASK_COUNT" -eq 0 ]; then
    echo "⚠️  无法从 task-plan.md 自动解析任务，创建基础模板"
    TASKS="- [ ] 1. （请手动填写具体任务）\n"
    TASK_COUNT=1
fi

# ─── 生成 progress.md ───

TIMESTAMP=$(date '+%Y-%m-%dT%H:%M:%S')

cat > "$PROGRESS_FILE" << EOF
# 任务进度

## 当前状态
- 阶段：dev-build
- 当前迭代：0
- 最后更新：${TIMESTAMP}

## 任务清单
$(echo -e "$TASKS")
## 迭代日志
EOF

echo ""
echo "============================================"
echo "  Ralph Loop 初始化完成"
echo "============================================"
echo "  任务目录:    ${TASK_DIR}"
echo "  进度文件:    ${PROGRESS_FILE}"
echo "  解析任务数:  ${TASK_COUNT}"
echo ""
echo "  下一步:"
echo "    ./scripts/ralph-loop.sh dev ${TASK_DIR}"
echo "============================================"
