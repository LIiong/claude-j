#!/bin/bash
#
# Claude Code 快速检查 - 用于 PreToolUse hook
# 返回值: 0=允许编辑, 1=阻止编辑
#

# 只检查 Java 源文件的修改
FILE_PATH="${1:-}"

if [[ -z "$FILE_PATH" ]]; then
    exit 0  # 没有文件路径，放行
fi

# 只拦截 src/main/java 下的文件创建/修改
if [[ ! "$FILE_PATH" =~ src/main/java/.*\.java$ ]]; then
    exit 0  # 不是业务代码文件，放行
fi

# 检查是否有活跃的执行计划
ACTIVE_DIR="docs/exec-plan/active"
LATEST_TASK=$(ls -1 "$ACTIVE_DIR" 2>/dev/null | grep -E '^[0-9]+-' | sort -V | tail -1)

if [ -z "$LATEST_TASK" ]; then
    echo ""
    echo "⚠️  警告：未找到活跃的执行计划目录"
    echo ""
    echo "根据项目规范，开始编码前必须："
    echo "  1. 创建执行计划: mkdir docs/exec-plan/active/00X-task-name/"
    echo "  2. 填写需求设计: requirement-design.md"
    echo "  3. 填写任务计划: task-plan.md"
    echo ""
    echo "运行以下命令检查状态:"
    echo "  ./scripts/dev-gate.sh"
    echo ""
    exit 1
fi

# 检查需求设计文档是否存在且有效
REQ_DOC="$ACTIVE_DIR/$LATEST_TASK/requirement-design.md"
if [ ! -f "$REQ_DOC" ]; then
    echo ""
    echo "⚠️  警告: 缺少需求设计文档"
    echo "   文件: $REQ_DOC"
    echo ""
    exit 1
fi

# 检查内容是否为空模板
CONTENT_LINES=$(grep -v "^#" "$REQ_DOC" 2>/dev/null | grep -v "^$" | grep -v "<!--" | grep -v "-->" | wc -l | tr -d ' ')
if [ "$CONTENT_LINES" -lt 10 ]; then
    echo ""
    echo "⚠️  警告: 需求设计文档内容过少 (${CONTENT_LINES} 行)"
    echo "   请先填写完整的设计内容，再开始编码"
    echo ""
    echo "当前任务: $LATEST_TASK"
    echo ""
    exit 1
fi

# 全部检查通过
exit 0
