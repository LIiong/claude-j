#!/bin/bash
# =================================================================
# 熵检查脚本 — Entropy Management / Garbage Collection
# 定期运行此脚本检测代码库中的模式漂移、架构违规和知识库一致性
# 用法: ./scripts/entropy-check.sh
# =================================================================

set -e
cd "$(dirname "$0")/.."

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ISSUES=0
WARNINGS=0

echo "============================================"
echo "  claude-j 熵检查 (Entropy Check)"
echo "============================================"
echo ""

# ------ 1. Domain 层纯净性 ------
echo "--- [1/12] Domain 层纯净性 ---"

SPRING_IN_DOMAIN=$(grep -r "import org.springframework" claude-j-domain/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$SPRING_IN_DOMAIN" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: domain 层发现 $SPRING_IN_DOMAIN 处 Spring import"
    grep -rn "import org.springframework" claude-j-domain/src/main/java/ 2>/dev/null
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: domain 层零 Spring import"
fi

MYBATIS_IN_DOMAIN=$(grep -r "import com.baomidou" claude-j-domain/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$MYBATIS_IN_DOMAIN" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: domain 层发现 $MYBATIS_IN_DOMAIN 处 MyBatis-Plus import"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: domain 层零 MyBatis-Plus import"
fi
echo ""

# ------ 2. 依赖方向 ------
echo "--- [2/12] 依赖方向检查 ---"

# adapter 不应 import infrastructure
ADAPTER_INFRA=$(grep -r "import com.claudej.infrastructure" claude-j-adapter/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$ADAPTER_INFRA" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: adapter 层导入 infrastructure ($ADAPTER_INFRA 处)"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: adapter 未导入 infrastructure"
fi

# application 不应 import infrastructure
APP_INFRA=$(grep -r "import com.claudej.infrastructure" claude-j-application/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$APP_INFRA" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: application 层导入 infrastructure ($APP_INFRA 处)"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: application 未导入 infrastructure"
fi

# domain 不应 import 任何其他层
DOMAIN_OTHERS=$(grep -r "import com.claudej\.\(application\|infrastructure\|adapter\)" claude-j-domain/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$DOMAIN_OTHERS" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: domain 层导入其他层 ($DOMAIN_OTHERS 处)"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: domain 未导入其他层"
fi
echo ""

# ------ 3. Java 8 兼容性 ------
echo "--- [3/12] Java 8 兼容性 ---"

VAR_USAGE=$(grep -rn "^\s*var " claude-j-*/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$VAR_USAGE" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: 发现 var 关键字 ($VAR_USAGE 处)"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: 无 var 关键字"
fi

LIST_OF=$(grep -rn "List\.of(" claude-j-*/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
MAP_OF=$(grep -rn "Map\.of(" claude-j-*/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$LIST_OF" -gt 0 ] || [ "$MAP_OF" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: 发现 List.of/Map.of ($((LIST_OF + MAP_OF)) 处)"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: 无 List.of/Map.of"
fi
echo ""

# ------ 4. DO 泄漏检查 ------
echo "--- [4/12] DO 对象泄漏检查 ---"

DO_IN_APP=$(grep -rn "import com.claudej.infrastructure.*dataobject" claude-j-application/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
DO_IN_ADAPTER=$(grep -rn "import com.claudej.infrastructure.*dataobject" claude-j-adapter/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
DO_IN_DOMAIN=$(grep -rn "import com.claudej.infrastructure.*dataobject" claude-j-domain/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
DO_LEAK=$((DO_IN_APP + DO_IN_ADAPTER + DO_IN_DOMAIN))
if [ "$DO_LEAK" -gt 0 ]; then
    echo -e "${RED}FAIL${NC}: DO 对象泄漏到 infrastructure 之上 ($DO_LEAK 处)"
    ISSUES=$((ISSUES + 1))
else
    echo -e "${GREEN}PASS${NC}: DO 对象未泄漏"
fi
echo ""

# ------ 5. 未使用 import ------
echo "--- [5/12] 代码整洁度 ---"

STAR_IMPORTS=$(grep -rn "import .*\.\*;" claude-j-*/src/main/java/ 2>/dev/null | wc -l | tr -d ' ')
if [ "$STAR_IMPORTS" -gt 0 ]; then
    echo -e "${YELLOW}WARN${NC}: 发现 * import ($STAR_IMPORTS 处)"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}PASS${NC}: 无 * import"
fi
echo ""

# ------ 6. 文档同步检查 ------
echo "--- [6/12] 文档同步检查 ---"

# 检查 schema.sql 中的表是否在 CLAUDE.md 中有记录
TABLES_IN_SQL=$(grep -c "CREATE TABLE" claude-j-start/src/main/resources/db/schema.sql 2>/dev/null || echo 0)
echo -e "${GREEN}INFO${NC}: schema.sql 中有 $TABLES_IN_SQL 张表"

if [ ! -f "CLAUDE.md" ]; then
    echo -e "${YELLOW}WARN${NC}: 缺少 CLAUDE.md"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}PASS${NC}: CLAUDE.md 存在"
fi
echo ""

# ------ 7. 测试覆盖 ------
echo "--- [7/12] 测试文件存在性 ---"

# 检查每个 main 下的 Service/Repository 是否有对应测试
MISSING_TESTS=0
for SRC in $(find claude-j-*/src/main/java -name "*Service.java" -o -name "*RepositoryImpl.java" -o -name "*Controller.java" 2>/dev/null); do
    TEST_PATH=$(echo "$SRC" | sed 's|/main/|/test/|' | sed 's|\.java$|Test.java|')
    if [ ! -f "$TEST_PATH" ]; then
        echo -e "${YELLOW}WARN${NC}: 缺少测试: $TEST_PATH"
        MISSING_TESTS=$((MISSING_TESTS + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
done
if [ "$MISSING_TESTS" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC}: 所有 Service/Repository/Controller 均有测试"
fi
echo ""

# ------ 8. 死代码检测 ------
echo "--- [8/12] 死代码检测 ---"

DEAD_CLASSES=0
for JAVA_FILE in $(find claude-j-*/src/main/java -name "*.java" 2>/dev/null); do
    CLASS_NAME=$(basename "$JAVA_FILE" .java)
    # Skip common entry points and framework classes
    if echo "$CLASS_NAME" | grep -qE "(Application|Config|Handler|Test)$"; then
        continue
    fi
    # Search for references in other files (excluding the file itself)
    REF_COUNT=$(grep -rl "$CLASS_NAME" claude-j-*/src/main/java/ 2>/dev/null | grep -v "$JAVA_FILE" | wc -l | tr -d ' ')
    if [ "$REF_COUNT" -eq 0 ]; then
        echo -e "${YELLOW}WARN${NC}: 可能未使用的类: $JAVA_FILE"
        DEAD_CLASSES=$((DEAD_CLASSES + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
done
if [ "$DEAD_CLASSES" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC}: 未发现明显死代码"
fi
echo ""

# ------ 9. ADR 一致性 ------
echo "--- [9/12] ADR 一致性 ---"

ADR_DIR="docs/architecture/decisions"
ADR_ISSUES=0
if [ -d "$ADR_DIR" ]; then
    for ADR in $(find "$ADR_DIR" -name "*.md" ! -name "000-template.md" 2>/dev/null); do
        # Check ADR has required sections: Status, Context, Decision
        if ! grep -q "## 状态\|## Status" "$ADR" 2>/dev/null; then
            echo -e "${YELLOW}WARN${NC}: ADR 缺少状态节: $ADR"
            ADR_ISSUES=$((ADR_ISSUES + 1))
            WARNINGS=$((WARNINGS + 1))
        fi
        if ! grep -q "## 背景\|## Context" "$ADR" 2>/dev/null; then
            echo -e "${YELLOW}WARN${NC}: ADR 缺少背景节: $ADR"
            ADR_ISSUES=$((ADR_ISSUES + 1))
            WARNINGS=$((WARNINGS + 1))
        fi
        if ! grep -q "## 决策\|## Decision" "$ADR" 2>/dev/null; then
            echo -e "${YELLOW}WARN${NC}: ADR 缺少决策节: $ADR"
            ADR_ISSUES=$((ADR_ISSUES + 1))
            WARNINGS=$((WARNINGS + 1))
        fi
    done
    if [ "$ADR_ISSUES" -eq 0 ]; then
        echo -e "${GREEN}PASS${NC}: 所有 ADR 格式完整"
    fi
else
    echo -e "${YELLOW}WARN${NC}: ADR 目录不存在: $ADR_DIR"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# ------ 10. 知识库一致性 ------
echo "--- [10/12] 知识库一致性 ---"

KB_ISSUES=0
# Scan file path references in key docs and verify they exist
for DOC in CLAUDE.md docs/guides/*.md .claude/agents/*.md; do
    if [ ! -f "$DOC" ]; then
        continue
    fi
    # Extract file paths: patterns like `path/to/file.ext` or path/to/file.ext
    REFS=$(grep -oE '`[a-zA-Z0-9_./-]+\.(md|xml|yml|yaml|sh|java)`' "$DOC" 2>/dev/null | tr -d '`' || true)
    for REF in $REFS; do
        # Skip patterns with {placeholder} or wildcards
        if echo "$REF" | grep -qE '\{|\*'; then
            continue
        fi
        # Skip Java package-like paths (com.claudej...)
        if echo "$REF" | grep -qE '^com\.|^org\.'; then
            continue
        fi
        if [ ! -f "$REF" ] && [ ! -d "$(dirname "$REF")" ]; then
            echo -e "${YELLOW}WARN${NC}: $DOC 引用了不存在的文件: $REF"
            KB_ISSUES=$((KB_ISSUES + 1))
            WARNINGS=$((WARNINGS + 1))
        fi
    done
done

if [ "$KB_ISSUES" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC}: 知识库文件引用一致"
fi
echo ""

# ------ 11. 过期活跃任务检测 ------
echo "--- [11/12] 过期活跃任务检测 ---"

STALE_TASKS=0
for REPORT in docs/exec-plan/active/*/test-report.md; do
    if [ ! -f "$REPORT" ]; then
        continue
    fi
    if grep -q "验收通过" "$REPORT" 2>/dev/null; then
        TASK_NAME=$(dirname "$REPORT" | xargs basename)
        echo -e "${YELLOW}WARN${NC}: 任务 $TASK_NAME 已验收通过但未归档到 archived/"
        STALE_TASKS=$((STALE_TASKS + 1))
        WARNINGS=$((WARNINGS + 1))
    fi
done
if [ "$STALE_TASKS" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC}: 无过期活跃任务"
fi
echo ""

# ------ 12. 聚合列表同步检查 ------
echo "--- [12/12] 聚合列表同步检查 ---"

AGG_ISSUES=0
if [ -d "claude-j-domain/src/main/java/com/claudej/domain" ]; then
    for AGG_DIR in claude-j-domain/src/main/java/com/claudej/domain/*/; do
        if [ ! -d "$AGG_DIR" ]; then
            continue
        fi
        AGG_NAME=$(basename "$AGG_DIR")
        # 跳过 common 包（非聚合）
        if [ "$AGG_NAME" = "common" ]; then
            continue
        fi
        # 检查 CLAUDE.md 中是否有该聚合的记录
        if ! grep -q "$AGG_NAME" CLAUDE.md 2>/dev/null; then
            echo -e "${YELLOW}WARN${NC}: 聚合 $AGG_NAME 在 domain 层存在但未在 CLAUDE.md 聚合列表中记录"
            AGG_ISSUES=$((AGG_ISSUES + 1))
            WARNINGS=$((WARNINGS + 1))
        fi
    done
fi
if [ "$AGG_ISSUES" -eq 0 ]; then
    echo -e "${GREEN}PASS${NC}: 聚合列表与代码同步"
fi
echo ""

# ------ 汇总 ------
echo "============================================"
echo "  检查完成"
echo "============================================"
echo -e "  错误 (FAIL):  ${RED}$ISSUES${NC}"
echo -e "  警告 (WARN):  ${YELLOW}$WARNINGS${NC}"
echo ""

# JSON summary for CI consumption
echo "{\"issues\": $ISSUES, \"warnings\": $WARNINGS, \"status\": \"$([ $ISSUES -gt 0 ] && echo 'FAIL' || echo 'PASS')\"}"

if [ "$ISSUES" -gt 0 ]; then
    echo -e "${RED}存在架构违规，请修复后重新检查。${NC}"
    exit 1
else
    echo -e "${GREEN}架构合规检查通过。${NC}"
    exit 0
fi
