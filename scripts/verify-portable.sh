#!/usr/bin/env bash
# verify-portable.sh — 校验 bootstrap-project.sh 产出的新项目目录是否自洽
#
# 典型用法：
#   ./scripts/bootstrap-project.sh --project-name foo --package-root com.foo \
#       --description "demo" --target-dir /tmp/foo --dry-run   # 先 dry-run 看看
#   ./scripts/bootstrap-project.sh --project-name foo --package-root com.foo \
#       --description "demo" --target-dir /tmp/foo             # 实际生成
#   ./scripts/verify-portable.sh /tmp/foo                      # 自检
#
# 退出码：
#   0 — 全通过
#   1 — 至少一项 FAIL
#
# 检查项：
#   1. 必需文件/目录存在（.claude、docs、scripts、CLAUDE.md、PORTING.md）
#   2. scripts/ 下的符号链接全部解析成功（无悬空链接）
#   3. .claude/settings.json 里每个 hook 命令引用的脚本都存在且可执行
#   4. 关键 skill 脚本齐全（session-start / pre-archive-check / entropy-check / ralph-*）

set -uo pipefail

TARGET="${1:-}"
if [[ -z "${TARGET}" ]]; then
    echo "用法: $0 <target-dir>"
    exit 2
fi

if [[ ! -d "${TARGET}" ]]; then
    echo "❌ 目标目录不存在: ${TARGET}"
    exit 1
fi

cd "${TARGET}"

PASS=0
FAIL=0

pass() { echo "  ✅ $*"; PASS=$((PASS + 1)); }
fail() { echo "  ❌ $*"; FAIL=$((FAIL + 1)); }

header() { echo ""; echo "▸ $*"; }

# ─── 1. 必需文件/目录 ───────────────────────────────────────────
header "1. 必需文件/目录"

REQUIRED_PATHS=(
    ".claude"
    ".claude/settings.json"
    ".claude/agents/dev.md"
    ".claude/agents/qa.md"
    ".claude/agents/architect.md"
    ".claude/skills/ralph/SKILL.md"
    ".claude/skills/qa-ship/SKILL.md"
    ".claude/skills/full-check/SKILL.md"
    ".claude/skills/using-claude-j-workflow/SKILL.md"
    "docs/exec-plan/active"
    "docs/exec-plan/archived"
    "docs/exec-plan/templates/dev-log.template.md"
    "docs/exec-plan/templates/handoff.template.md"
    "docs/templates/project/CLAUDE.template.md"
    "scripts/hooks"
    "CLAUDE.md"
    "PORTING.md"
)

for p in "${REQUIRED_PATHS[@]}"; do
    if [[ -e "$p" ]]; then
        pass "$p"
    else
        fail "缺失: $p"
    fi
done

# ─── 2. 符号链接完整性 ──────────────────────────────────────────
header "2. scripts/ 下的符号链接（必须全部解析成功）"

EXPECTED_LINKS=(
    "scripts/ralph-init.sh"
    "scripts/ralph-loop.sh"
    "scripts/ralph-auto.sh"
    "scripts/entropy-check.sh"
    "scripts/quick-check.sh"
)

for link in "${EXPECTED_LINKS[@]}"; do
    if [[ ! -L "$link" ]]; then
        fail "$link 不是符号链接或不存在"
        continue
    fi
    # -e 跟随链接，若目标不存在返回 false
    if [[ ! -e "$link" ]]; then
        target=$(readlink "$link")
        fail "$link → $target（悬空链接，目标缺失）"
    else
        pass "$link → $(readlink "$link")"
    fi
done

# ─── 3. settings.json 引用的 hook 脚本存在且可执行 ───────────────
header "3. .claude/settings.json 引用的脚本"

if [[ ! -f ".claude/settings.json" ]]; then
    fail "无 .claude/settings.json，跳过 hook 路径校验"
else
    # 用 python3 抽取所有 $CLAUDE_PROJECT_DIR/... 路径
    HOOK_PATHS=$(python3 - <<'PY'
import json, re, sys
try:
    s = json.load(open('.claude/settings.json'))
except Exception as e:
    print("ERR:" + str(e))
    sys.exit(0)
paths = set()
def walk(o):
    if isinstance(o, dict):
        for v in o.values(): walk(v)
    elif isinstance(o, list):
        for v in o: walk(v)
    elif isinstance(o, str):
        for m in re.findall(r'\$CLAUDE_PROJECT_DIR/([^"\s]+)', o):
            paths.add(m)
walk(s)
for p in sorted(paths):
    print(p)
PY
)

    if [[ "${HOOK_PATHS}" == ERR:* ]]; then
        fail "settings.json 解析失败: ${HOOK_PATHS#ERR:}"
    elif [[ -z "${HOOK_PATHS}" ]]; then
        fail "settings.json 未抽取到任何 hook 脚本路径（结构异常？）"
    else
        while IFS= read -r p; do
            [[ -z "$p" ]] && continue
            if [[ ! -e "$p" ]]; then
                fail "hook 脚本缺失: $p"
            elif [[ ! -x "$p" ]]; then
                fail "hook 脚本无执行权限: $p"
            else
                pass "$p"
            fi
        done <<< "${HOOK_PATHS}"
    fi
fi

# ─── 4. 关键 skill 脚本齐全 ─────────────────────────────────────
header "4. 关键 skill 脚本齐全"

CRITICAL_SCRIPTS=(
    ".claude/skills/using-claude-j-workflow/scripts/session-start.sh"
    ".claude/skills/qa-ship/scripts/pre-archive-check.sh"
    ".claude/skills/full-check/scripts/entropy-check.sh"
    ".claude/skills/full-check/scripts/quick-check.sh"
    ".claude/skills/ralph/scripts/ralph-init.sh"
    ".claude/skills/ralph/scripts/ralph-loop.sh"
    ".claude/skills/ralph/scripts/ralph-auto.sh"
)

for s in "${CRITICAL_SCRIPTS[@]}"; do
    if [[ ! -f "$s" ]]; then
        fail "缺失: $s"
    elif [[ ! -x "$s" ]]; then
        fail "无执行权限: $s"
    else
        pass "$s"
    fi
done

# ─── 汇总 ─────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  PASS: ${PASS}    FAIL: ${FAIL}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [[ ${FAIL} -gt 0 ]]; then
    echo "❌ 移植产物不完整，请按上列 FAIL 项修补。"
    exit 1
fi

echo "✅ 目标目录移植产物自洽。"
exit 0
