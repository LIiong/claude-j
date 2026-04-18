#!/bin/bash
# =================================================================
# Ralph Loop — Agent 持续运行框架
# 基于 Ralph Wiggum Loop 模式：每次迭代启动全新 Claude Code 会话
# 解决 LLM 长会话上下文退化问题
#
# 用法:
#   ./scripts/ralph-loop.sh <agent> <task-dir> [max-iterations]
#
# 示例:
#   ./scripts/ralph-loop.sh dev docs/exec-plan/active/002-order-service
#   ./scripts/ralph-loop.sh qa docs/exec-plan/active/002-order-service
#   ./scripts/ralph-loop.sh dev docs/exec-plan/active/002-order-service 10
#
# 机制:
#   1. 读取 progress.md 确定当前进度
#   2. 构建上下文提示（progress + git log + 下一任务）
#   3. 启动 claude --agent <agent> --print（全新会话）
#   4. 检查 progress.md 是否更新（外部验证）
#   5. 若仍有未完成任务 → 重新启动（全新上下文）
#   6. 所有任务完成 或 达到安全阀 → 退出
# =================================================================

set -uo pipefail

# ─── 参数解析 ───
AGENT="${1:-}"
TASK_DIR="${2:-}"
MAX_ITERATIONS="${3:-20}"
COOLDOWN=5
STALL_LIMIT=3

if [ -z "$AGENT" ] || [ -z "$TASK_DIR" ]; then
    echo "用法: $0 <agent> <task-dir> [max-iterations]"
    echo ""
    echo "  agent:          dev | qa | architect"
    echo "  task-dir:       任务目录路径（如 docs/exec-plan/active/002-order-service）"
    echo "  max-iterations: 最大迭代次数（默认 20，安全阀）"
    echo ""
    echo "示例:"
    echo "  $0 dev docs/exec-plan/active/002-order-service"
    echo "  $0 qa docs/exec-plan/active/002-order-service 10"
    exit 1
fi

cd "$(dirname "$0")/.."

PROGRESS_FILE="$TASK_DIR/progress.md"
AGENT_FILE=".claude/agents/${AGENT}.md"

# ─── 前置检查 ───
if [ ! -f "$AGENT_FILE" ]; then
    echo "❌ Agent 定义文件不存在: $AGENT_FILE"
    echo "   可用 Agent: dev, qa, architect"
    exit 1
fi

if [ ! -f "$PROGRESS_FILE" ]; then
    echo "❌ 进度文件不存在: $PROGRESS_FILE"
    echo "   请先运行 ./scripts/ralph-init.sh $TASK_DIR 初始化"
    exit 1
fi

# ─── 辅助函数 ───

# 获取未完成任务数
count_remaining() {
    grep -c '^\- \[ \]' "$PROGRESS_FILE" 2>/dev/null || echo 0
}

# 获取下一个待办任务
next_task() {
    grep -m1 '^\- \[ \]' "$PROGRESS_FILE" 2>/dev/null | sed 's/^- \[ \] //'
}

# 获取当前 git commit hash
current_commit() {
    git rev-parse --short HEAD 2>/dev/null || echo "none"
}

# 构建 Agent 提示
build_prompt() {
    local next="$1"
    local remaining="$2"
    local git_log
    git_log=$(git log --oneline -10 2>/dev/null || echo "(no git history)")

    cat <<PROMPT
你正在 Ralph Loop 中运行（迭代 ${ITERATION}/${MAX_ITERATIONS}）。

## 当前任务目录
$TASK_DIR

## 进度概览
剩余待办任务: ${remaining}

## 下一个待办任务
${next}

## 最近 Git 提交
${git_log}

## 你需要做的
1. 阅读 ${PROGRESS_FILE} 了解完整进度
2. 阅读 ${TASK_DIR}/requirement-design.md 了解需求设计
3. 阅读 ${TASK_DIR}/task-plan.md 了解任务计划
4. 完成上述"下一个待办任务"
5. 将完成的任务在 ${PROGRESS_FILE} 中标记为 [x]（附 commit hash）
6. 在 ${PROGRESS_FILE} 的迭代日志中记录本次迭代
7. git commit 所有变更

## 重要约束
- 只完成 1-2 个任务，不要贪多
- 遇到阻塞时记录到 progress.md 并正常退出
- 遵循 DDD 分层架构和 Java 8 兼容性要求
PROMPT
}

# ─── 主循环 ───

echo "============================================"
echo "  Ralph Loop 启动"
echo "============================================"
echo "  Agent:       @${AGENT}"
echo "  任务目录:    ${TASK_DIR}"
echo "  最大迭代:    ${MAX_ITERATIONS}"
echo "  安全阀:      连续 ${STALL_LIMIT} 次无进展则退出"
echo "============================================"
echo ""

ITERATION=0
STALL_COUNT=0

while true; do
    ITERATION=$((ITERATION + 1))

    # ─── 安全阀：最大迭代次数 ───
    if [ "$ITERATION" -gt "$MAX_ITERATIONS" ]; then
        echo ""
        echo "⚠️  达到最大迭代次数 (${MAX_ITERATIONS})，退出 Ralph Loop"
        echo "    请检查 ${PROGRESS_FILE} 了解当前进度"
        exit 1
    fi

    # ─── 检查剩余任务 ───
    REMAINING=$(count_remaining)
    if [ "$REMAINING" -eq 0 ]; then
        echo ""
        echo "✅ 所有任务已完成！Ralph Loop 正常退出"
        echo "    共执行 $((ITERATION - 1)) 次迭代"
        exit 0
    fi

    NEXT=$(next_task)
    COMMIT_BEFORE=$(current_commit)

    echo "--- 迭代 ${ITERATION}/${MAX_ITERATIONS} ---"
    echo "  剩余任务: ${REMAINING}"
    echo "  下一任务: ${NEXT}"
    echo "  起始 commit: ${COMMIT_BEFORE}"
    echo ""

    # ─── 构建提示并启动全新 Claude 会话 ───
    PROMPT=$(build_prompt "$NEXT" "$REMAINING")

    # 使用 claude --agent 启动全新会话（--print 模式，非交互）
    # 注意：这里使用 claude CLI，确保已安装
    if command -v claude >/dev/null 2>&1; then
        echo "$PROMPT" | claude --agent "$AGENT" --print 2>&1 || true
    else
        echo "❌ claude CLI 未找到，请确保已安装 Claude Code"
        exit 1
    fi

    # ─── 外部验证：检查是否有新提交（不依赖模型自我评估） ───
    COMMIT_AFTER=$(current_commit)

    if [ "$COMMIT_BEFORE" = "$COMMIT_AFTER" ]; then
        STALL_COUNT=$((STALL_COUNT + 1))
        echo "  ⚠️  无新提交（连续停滞 ${STALL_COUNT}/${STALL_LIMIT}）"

        if [ "$STALL_COUNT" -ge "$STALL_LIMIT" ]; then
            echo ""
            echo "❌ 连续 ${STALL_LIMIT} 次迭代无进展，退出 Ralph Loop"
            echo "    可能原因：任务阻塞、配置问题、或需要人工干预"
            echo "    请检查 ${PROGRESS_FILE} 了解详情"
            exit 1
        fi
    else
        STALL_COUNT=0
        echo "  ✅ 新提交: ${COMMIT_AFTER}"
    fi

    echo ""

    # ─── 冷却等待 ───
    if [ "$REMAINING" -gt 0 ]; then
        echo "  冷却 ${COOLDOWN}s..."
        sleep "$COOLDOWN"
    fi
done
