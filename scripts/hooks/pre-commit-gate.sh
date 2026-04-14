#!/bin/bash
#
# Pre-commit hook - 提交前检查是否有执行计划
#

# 检查是否有活跃任务
ACTIVE_DIR="docs/exec-plan/active"
LATEST_TASK=$(ls -1 "$ACTIVE_DIR" 2>/dev/null | grep -E '^[0-9]+-' | sort -V | tail -1)

if [ -z "$LATEST_TASK" ]; then
    echo ""
    echo "❌ 提交被拒绝: 未找到活跃的执行计划任务"
    echo ""
    echo "请在 docs/exec-plan/active/ 目录下创建任务目录并填写需求设计文档"
    echo ""
    exit 1
fi

# 检查本次提交是否包含业务代码
if git diff --cached --name-only | grep -q "src/main/java/.*\.java$"; then
    # 包含业务代码，检查需求设计文档
    REQ_DOC="$ACTIVE_DIR/$LATEST_TASK/requirement-design.md"
    if [ ! -f "$REQ_DOC" ]; then
        echo ""
        echo "❌ 提交被拒绝: 缺少需求设计文档"
        echo "   任务: $LATEST_TASK"
        echo "   文件: $REQ_DOC"
        echo ""
        exit 1
    fi
fi

exit 0
