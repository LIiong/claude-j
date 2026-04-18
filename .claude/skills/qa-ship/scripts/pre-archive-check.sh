#!/usr/bin/env bash
# =================================================================
# pre-archive-check.sh — QA Ship 阶段归档前置闸
#
# 用法：
#   ./pre-archive-check.sh <task-id>                 # 4 项必查 + 重跑三项 pre-flight
#   ./pre-archive-check.sh <task-id> --skip-reverify # 仅 4 项必查（用于快速预演）
#   ./pre-archive-check.sh <task-id> --archived      # 从 archived/ 读（回归审计用）
#
# 退出码：
#   0 = PASS，可继续 git mv 归档
#   1 = FAIL，拒绝归档，终端打印失败清单
# =================================================================

set -euo pipefail

TASK_ID="${1:-}"
SKIP_REVERIFY=0
FROM_ARCHIVED=0
shift || true
for arg in "$@"; do
  case "$arg" in
    --skip-reverify) SKIP_REVERIFY=1 ;;
    --archived)      FROM_ARCHIVED=1 ;;
    *) echo "unknown flag: $arg" >&2; exit 2 ;;
  esac
done

if [ -z "$TASK_ID" ]; then
  echo "usage: pre-archive-check.sh <task-id> [--skip-reverify] [--archived]" >&2
  exit 2
fi

# 定位仓库根（无论从哪调用）
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$REPO_ROOT"

if [ "$FROM_ARCHIVED" -eq 1 ]; then
  TASK_DIR="docs/exec-plan/archived/${TASK_ID}"
else
  TASK_DIR="docs/exec-plan/active/${TASK_ID}"
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

FAILS=0
pass() { echo -e "  ${GREEN}PASS${NC}: $1"; }
fail() { echo -e "  ${RED}FAIL${NC}: $1"; FAILS=$((FAILS + 1)); }
warn() { echo -e "  ${YELLOW}WARN${NC}: $1"; }

echo "============================================"
echo "  QA Ship 归档前置检查 — $TASK_ID"
echo "  路径：$TASK_DIR"
echo "============================================"

# ------ 0. 任务目录存在 ------
if [ ! -d "$TASK_DIR" ]; then
  fail "task dir not found: $TASK_DIR"
  echo ""
  echo -e "${RED}归档前置检查失败（0 项准入）${NC}"
  exit 1
fi

# ------ 1. handoff.md: status:approved 且 to in {qa, ralph} ------
echo ""
echo "--- [1/4] handoff.md 状态 ---"
HANDOFF="$TASK_DIR/handoff.md"
if [ ! -f "$HANDOFF" ]; then
  fail "handoff.md 不存在"
else
  # 兼容两种写法：
  #   YAML 原生   : `status: approved`
  #   Markdown 加粗: `- **status**: approved`
  if grep -qiE "^(- )?(\*\*)?status(\*\*)?:[[:space:]]*approved" "$HANDOFF"; then
    pass "status: approved"
  else
    fail "handoff.md 中 status 非 approved（当前：$(grep -iE '^(- )?(\*\*)?status' "$HANDOFF" | head -1 || echo '缺失'))"
  fi
  # to 字段：可为 qa（规则 agent-collaboration.md 定义）或 ralph（实际编排惯例）
  TO_LINE=$(grep -iE "^(- )?(\*\*)?to(\*\*)?:[[:space:]]" "$HANDOFF" | head -1 || true)
  if echo "$TO_LINE" | grep -qiE "(qa|ralph)"; then
    pass "to: $(echo "$TO_LINE" | sed -E 's/.*to[^:]*:[[:space:]]*//i')"
  else
    fail "handoff.md 中 to 非 qa/ralph（当前：${TO_LINE:-缺失}）"
  fi
fi

# ------ 2. test-report.md: 含「验收通过」 ------
echo ""
echo "--- [2/4] test-report.md 验收结论 ---"
REPORT="$TASK_DIR/test-report.md"
if [ ! -f "$REPORT" ]; then
  fail "test-report.md 不存在"
elif grep -q "验收通过" "$REPORT"; then
  pass "test-report.md 含「验收通过」"
else
  fail "test-report.md 未标记「验收通过」"
fi

# ------ 3. task-plan.md: QA 行已完成 ------
echo ""
echo "--- [3/4] task-plan.md QA 行状态 ---"
PLAN="$TASK_DIR/task-plan.md"
if [ ! -f "$PLAN" ]; then
  fail "task-plan.md 不存在"
else
  # 抓取所有含 "QA" 的行，逐行判断是否已完成
  QA_LINES=$(grep -nE "QA" "$PLAN" || true)
  if [ -z "$QA_LINES" ]; then
    warn "task-plan.md 未显式出现 'QA' 任务行（可能为非业务任务）"
  else
    BAD_LINES=$(echo "$QA_LINES" | grep -E "(待办|进行中|待验收|待修复)" || true)
    if [ -n "$BAD_LINES" ]; then
      fail "task-plan.md 存在未完成 QA 行："
      echo "$BAD_LINES" | sed 's/^/      /'
    else
      # 至少有一个 QA 行显式标「完成 / 验收通过」
      GOOD_LINES=$(echo "$QA_LINES" | grep -E "(完成|验收通过)" || true)
      if [ -n "$GOOD_LINES" ]; then
        pass "task-plan.md QA 行已完成（$(echo "$GOOD_LINES" | wc -l | tr -d ' ') 行命中）"
      else
        warn "task-plan.md QA 行未明确标「完成/验收通过」，但也无「待办/进行中/待验收/待修复」"
      fi
    fi
  fi
fi

# ------ 4. 三项 pre-flight 重跑 ------
echo ""
echo "--- [4/4] 三项 pre-flight 重跑 ---"
if [ "$SKIP_REVERIFY" -eq 1 ]; then
  warn "已 --skip-reverify，跳过（请确保已另行验证）"
else
  echo "      预计耗时 2-5 分钟；如需快速预演请加 --skip-reverify"
  set +e

  echo "      [4.1] mvn test ..."
  mvn -q test >/tmp/pre-archive-mvn-test.log 2>&1
  RC=$?
  if [ $RC -eq 0 ]; then
    pass "mvn test 通过"
  else
    fail "mvn test 失败（RC=$RC，日志：/tmp/pre-archive-mvn-test.log）"
  fi

  echo "      [4.2] mvn checkstyle:check ..."
  mvn -q checkstyle:check >/tmp/pre-archive-checkstyle.log 2>&1
  RC=$?
  if [ $RC -eq 0 ]; then
    pass "mvn checkstyle:check 通过"
  else
    fail "checkstyle 失败（RC=$RC，日志：/tmp/pre-archive-checkstyle.log）"
  fi

  echo "      [4.3] ./scripts/entropy-check.sh ..."
  ./scripts/entropy-check.sh >/tmp/pre-archive-entropy.log 2>&1
  RC=$?
  if [ $RC -eq 0 ]; then
    pass "entropy-check 通过"
  else
    fail "entropy-check 失败（RC=$RC，日志：/tmp/pre-archive-entropy.log）"
  fi

  set -e
fi

# ------ 汇总 ------
echo ""
echo "============================================"
if [ "$FAILS" -eq 0 ]; then
  echo -e "  ${GREEN}归档前置检查全部通过${NC}（$TASK_ID）"
  echo "  可继续：git mv docs/exec-plan/active/$TASK_ID docs/exec-plan/archived/$TASK_ID"
  exit 0
else
  echo -e "  ${RED}归档前置检查失败（$FAILS 项）${NC}"
  echo "  修复后重跑：./.claude/skills/qa-ship/scripts/pre-archive-check.sh $TASK_ID"
  exit 1
fi
