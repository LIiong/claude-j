#!/usr/bin/env bash
# session-start.sh — SessionStart hook
# 会话开始时向 agent 注入 claude-j 工作流上下文：
#   1. 元 skill 指引（using-claude-j-workflow）
#   2. 当前进行中的任务清单（docs/exec-plan/active/）
#   3. 当前角色标记（.claude-current-role）
#   4. 待提交改动摘要（git status --short）
#
# 输出通过 stdout 注入到 agent 的系统上下文。
# 保持简短（< 2KB），不要重复 CLAUDE.md 已有内容。

set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$PROJECT_DIR"

echo "=== claude-j 会话上下文（由 session-start hook 注入） ==="
echo ""

# 1. 元 skill 指引
echo "📖 工作流导航：阅读 .claude/skills/using-claude-j-workflow/SKILL.md 了解 5 阶段流水线与 skill 入口"
echo ""

# 2. 当前进行中的任务
ACTIVE_DIR="docs/exec-plan/active"
if [ -d "$ACTIVE_DIR" ] && [ -n "$(ls -A "$ACTIVE_DIR" 2>/dev/null)" ]; then
  echo "🚧 进行中的任务（docs/exec-plan/active/）："
  for task in "$ACTIVE_DIR"/*/; do
    [ -d "$task" ] || continue
    task_id=$(basename "$task")
    handoff="$task/handoff.md"
    if [ -f "$handoff" ]; then
      status=$(grep -m1 -E '^status:' "$handoff" 2>/dev/null | awk '{print $2}' || echo "unknown")
      to=$(grep -m1 -E '^to:' "$handoff" 2>/dev/null | awk '{print $2}' || echo "unknown")
      echo "   - $task_id (status=$status, to=$to)"
    else
      echo "   - $task_id (no handoff.md)"
    fi
  done
  echo ""
  echo "   → 续跑任一任务：/ralph <task-id>"
else
  echo "✨ 无进行中任务。启动新任务：/ralph <task-id> <需求描述>"
fi
echo ""

# 3. 当前角色标记
if [ -f ".claude-current-role" ]; then
  role=$(cat .claude-current-role 2>/dev/null || echo "")
  if [ -n "$role" ]; then
    echo "👤 当前角色标记：@$role（guard-agent-scope.sh 会据此拦截越权写）"
    echo ""
  fi
fi

# 4. 待提交改动
if git rev-parse --git-dir >/dev/null 2>&1; then
  dirty_count=$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')
  if [ "$dirty_count" != "0" ]; then
    echo "📝 工作区有 $dirty_count 处未提交改动（git status --short 查看）"
    echo "   → 动手前弄清它属于哪个任务/阶段，避免误 commit"
    echo ""
  fi
fi

echo "🔒 三条铁律：TDD / VERIFICATION / DEBUG（见 CLAUDE.md）"
echo "=== 上下文注入结束 ==="
