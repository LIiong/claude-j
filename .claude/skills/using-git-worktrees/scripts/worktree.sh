#!/usr/bin/env bash
# worktree.sh — 多任务并行 worktree 管理
# 用法：
#   worktree.sh new <task-id> [base-branch]   # 默认 base = main
#   worktree.sh list
#   worktree.sh remove <task-id>

set -euo pipefail

ACTION="${1:-}"
TASK_ID="${2:-}"
BASE_BRANCH="${3:-main}"

# 解析主 worktree 根目录（脚本无论从何处调用都能正确定位）
PROJECT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
PROJECT_NAME="$(basename "$PROJECT_DIR")"
WT_PARENT="$(dirname "$PROJECT_DIR")/${PROJECT_NAME}-wt"

usage() {
  cat <<EOF
worktree.sh — 多任务并行管理

用法：
  $0 new <task-id> [base-branch]   创建 worktree（分支 task/<task-id>）
  $0 list                          列出所有 worktree
  $0 remove <task-id>              删除 worktree（保留分支）

示例：
  $0 new 011-flyway-migration
  $0 new 012-multi-profile main
  $0 list
  $0 remove 011-flyway-migration
EOF
  exit 2
}

ensure_task_id() {
  if [ -z "$TASK_ID" ]; then
    echo "❌ 缺少 task-id" >&2
    usage
  fi
  # 约束：task-id 形如 011-xxx
  if ! [[ "$TASK_ID" =~ ^[0-9]{3}-[a-z0-9-]+$ ]]; then
    echo "❌ task-id 格式应为 '###-kebab-case'（如 011-flyway-migration），实际：$TASK_ID" >&2
    exit 1
  fi
}

action_new() {
  ensure_task_id
  local branch="task/${TASK_ID}"
  local wt_dir="${WT_PARENT}/${TASK_ID}"

  if [ -d "$wt_dir" ]; then
    echo "⚠️  worktree 已存在：$wt_dir"
    echo "   进入：cd $wt_dir"
    exit 0
  fi

  mkdir -p "$WT_PARENT"

  # 若分支已存在，直接 checkout；否则基于 base 新建
  if git show-ref --verify --quiet "refs/heads/$branch"; then
    echo "🔄 复用已有分支 $branch"
    git worktree add "$wt_dir" "$branch"
  else
    echo "🌱 从 $BASE_BRANCH 新建分支 $branch"
    git worktree add -b "$branch" "$wt_dir" "$BASE_BRANCH"
  fi

  echo ""
  echo "✅ 创建成功"
  echo "   路径：$wt_dir"
  echo "   分支：$branch（base=$BASE_BRANCH）"
  echo ""
  echo "下一步："
  echo "   cd $wt_dir"
  echo "   # 在该 worktree 启动 shell / IDE / claude 会话开展任务"
}

action_list() {
  echo "📋 当前 worktree："
  git worktree list
  echo ""

  if [ -d "$WT_PARENT" ]; then
    echo "📁 副 worktree 父目录：$WT_PARENT"
    local count
    count=$(find "$WT_PARENT" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | wc -l | tr -d ' ')
    echo "   含 $count 个副 worktree"
  fi
}

action_remove() {
  ensure_task_id
  local wt_dir="${WT_PARENT}/${TASK_ID}"
  local branch="task/${TASK_ID}"

  if [ ! -d "$wt_dir" ]; then
    echo "⚠️  worktree 不存在：$wt_dir"
    echo "   用 '$0 list' 查看实际路径"
    exit 1
  fi

  # 检查是否有未提交改动
  if [ -n "$(git -C "$wt_dir" status --porcelain 2>/dev/null)" ]; then
    echo "❌ worktree 有未提交改动，拒绝删除：$wt_dir"
    echo "   请先提交或 stash，或手动 rm 后用 'git worktree prune'"
    exit 1
  fi

  git worktree remove "$wt_dir"
  echo "✅ 已移除 worktree：$wt_dir"
  echo "   分支 $branch 保留（如需删除：git branch -d $branch）"

  # 清理空父目录
  if [ -d "$WT_PARENT" ] && [ -z "$(ls -A "$WT_PARENT" 2>/dev/null)" ]; then
    rmdir "$WT_PARENT"
    echo "   清理空父目录：$WT_PARENT"
  fi
}

case "$ACTION" in
  new)    action_new ;;
  list)   action_list ;;
  remove) action_remove ;;
  -h|--help|help) usage ;;
  *)      usage ;;
esac
