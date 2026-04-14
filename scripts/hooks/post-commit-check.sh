#!/bin/bash
# =================================================================
# post-commit-check.sh — PostToolUse Hook: commit 后质量检查
# 在 git commit 后自动运行 quick-check
# 包含 Java 业务代码的 commit：失败时 exit 2 阻断（强制修复）
# 仅包含非 Java 文件的 commit：失败时仅警告（非阻断）
# =================================================================

set -uo pipefail

cd "$(dirname "$0")/../.."

# 仅在 quick-check.sh 存在时运行
if [ ! -f "./scripts/quick-check.sh" ]; then
    exit 0
fi

# 检查最近一次 commit 是否包含 Java 业务代码
HAS_JAVA=$(git diff-tree --no-commit-id --name-only -r HEAD 2>/dev/null | grep "src/main/java/.*\.java$" || true)

echo "--- Post-Commit Quick Check ---" >&2

# 运行快速检查（编译 + 单测 + 风格）
if ./scripts/quick-check.sh >/dev/null 2>&1; then
    echo "✅ Post-commit quick-check 通过" >&2
    exit 0
fi

# quick-check 失败
if [ -n "$HAS_JAVA" ]; then
    echo "❌ Post-commit quick-check 失败（包含 Java 业务代码变更）" >&2
    echo "   请运行 ./scripts/quick-check.sh 查看详情并修复后重新提交" >&2
    echo "   提示：修复后创建新 commit，不要 amend" >&2
    exit 2
fi

# 非 Java 文件的 commit，仅警告
echo "⚠️ Post-commit quick-check 失败（非业务代码变更，不阻断）" >&2
echo "   建议运行 ./scripts/quick-check.sh 查看详情" >&2
exit 0
