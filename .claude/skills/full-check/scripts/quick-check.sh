#!/bin/bash
# =================================================================
# 快速本地检查脚本 — 开发过程中快速验证
# 仅执行：编译 + domain/application 单测 + checkstyle
# 跳过：集成测试、覆盖率、entropy 全量检查
# 用法: ./scripts/quick-check.sh
# =================================================================

set -e
cd "$(dirname "$0")/.."

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

START_TIME=$(date +%s)

echo "============================================"
echo "  claude-j 快速检查 (Quick Check)"
echo "============================================"
echo ""

# ------ 1. 编译 ------
echo "--- [1/3] 编译检查 ---"
mvn compile -q -B 2>/dev/null
if [ $? -ne 0 ]; then
    echo -e "${RED}FAIL${NC}: 编译失败"
    exit 1
fi
echo -e "${GREEN}PASS${NC}: 编译成功"
echo ""

# ------ 2. 单元测试（仅 domain + application） ------
echo "--- [2/3] 单元测试 (Domain + Application) ---"
mvn test -pl claude-j-domain,claude-j-application -q -B 2>/dev/null
if [ $? -ne 0 ]; then
    echo -e "${RED}FAIL${NC}: 单元测试失败"
    exit 1
fi
echo -e "${GREEN}PASS${NC}: 单元测试通过"
echo ""

# ------ 3. Checkstyle ------
echo "--- [3/3] Checkstyle ---"
mvn checkstyle:check -q -B 2>/dev/null
if [ $? -ne 0 ]; then
    echo -e "${RED}FAIL${NC}: Checkstyle 违规"
    exit 1
fi
echo -e "${GREEN}PASS${NC}: Checkstyle 通过"
echo ""

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo "============================================"
echo -e "  ${GREEN}快速检查通过${NC} (${ELAPSED}s)"
echo "============================================"
echo ""
echo "  如需完整检查，请运行:"
echo "    mvn test                    # 全量测试（含 ArchUnit）"
echo "    ./scripts/entropy-check.sh  # 熵检查（架构漂移检测）"
echo ""
