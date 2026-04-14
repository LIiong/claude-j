#!/bin/bash
# =================================================================
# guard-agent-scope.sh — PreToolUse Hook: Agent 写作域守护
# 根据当前 Agent 角色限制文件写入范围
# 角色通过 .claude-current-role 标记文件识别
# 违规时 exit 2 阻断写入
# =================================================================

set -euo pipefail

INPUT=$(cat)

# 解析目标文件路径
FILE_PATH=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    ti = data.get('tool_input', {})
    print(ti.get('file_path', ''))
except:
    print('')
" 2>/dev/null)

# 无文件路径则放行
if [ -z "$FILE_PATH" ]; then
    exit 0
fi

cd "$(dirname "$0")/../.."

# 读取当前角色标记文件
ROLE_FILE=".claude-current-role"
if [ ! -f "$ROLE_FILE" ]; then
    # 无角色标记，放行（兼容非 skill 场景）
    exit 0
fi

ROLE=$(cat "$ROLE_FILE" 2>/dev/null | tr -d '[:space:]')

case "$ROLE" in
    qa)
        # @qa 禁止写入 src/main/java/
        if [[ "$FILE_PATH" == */src/main/java/*.java ]]; then
            echo "❌ AGENT SCOPE: @qa 禁止写入业务代码 (src/main/java/)" >&2
            echo "   发现问题请通知 @dev 修复，不要自行修改业务代码" >&2
            exit 2
        fi
        # @qa 禁止写入 requirement-design.md 和 dev-log.md
        BASENAME=$(basename "$FILE_PATH")
        if [[ "$BASENAME" == "requirement-design.md" || "$BASENAME" == "dev-log.md" ]]; then
            echo "❌ AGENT SCOPE: @qa 禁止写入 $BASENAME（@dev 职责）" >&2
            exit 2
        fi
        ;;
    architect)
        # @architect 禁止写入任何 Java 文件
        if [[ "$FILE_PATH" == *.java ]]; then
            echo "❌ AGENT SCOPE: @architect 禁止写入 Java 代码" >&2
            echo "   架构评审仅修改文档，不直接修改代码" >&2
            exit 2
        fi
        # @architect 禁止写入 task-plan.md, dev-log.md, test-*.md
        BASENAME=$(basename "$FILE_PATH")
        if [[ "$BASENAME" == "task-plan.md" || "$BASENAME" == "dev-log.md" || "$BASENAME" == "test-case-design.md" || "$BASENAME" == "test-report.md" ]]; then
            echo "❌ AGENT SCOPE: @architect 禁止写入 $BASENAME" >&2
            exit 2
        fi
        ;;
    dev)
        # @dev 禁止写入 test-case-design.md 和 test-report.md
        BASENAME=$(basename "$FILE_PATH")
        if [[ "$BASENAME" == "test-case-design.md" || "$BASENAME" == "test-report.md" ]]; then
            echo "❌ AGENT SCOPE: @dev 禁止写入 $BASENAME（@qa 职责）" >&2
            echo "   测试用例设计和测试报告由 @qa 负责" >&2
            exit 2
        fi
        ;;
    *)
        # 未知角色或空角色，放行
        ;;
esac

exit 0
