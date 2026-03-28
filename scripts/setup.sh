#!/bin/bash
# =================================================================
# 项目环境一键搭建脚本
# 用法: ./scripts/setup.sh
# =================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cd "$(dirname "$0")/.."

echo "============================================"
echo "  claude-j 环境搭建"
echo "============================================"
echo ""

# ------ 1. 检查 JDK ------
echo "--- [1/4] 检查 JDK ---"
if ! command -v java &> /dev/null; then
    echo -e "${RED}FAIL${NC}: 未找到 java 命令。请安装 JDK 8+。"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/' | cut -d'.' -f1)
# Handle 1.8 format
if [ "$JAVA_VERSION" = "1" ]; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/' | cut -d'.' -f2)
fi

if [ "$JAVA_VERSION" -lt 8 ] 2>/dev/null; then
    echo -e "${RED}FAIL${NC}: JDK 版本过低 ($JAVA_VERSION)，需要 JDK 8+。"
    exit 1
fi
echo -e "${GREEN}PASS${NC}: JDK $(java -version 2>&1 | head -1)"

# ------ 2. 检查 Maven ------
echo "--- [2/4] 检查 Maven ---"
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}FAIL${NC}: 未找到 mvn 命令。请安装 Maven 3.6+。"
    exit 1
fi

MVN_VERSION=$(mvn -version 2>&1 | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
echo -e "${GREEN}PASS${NC}: Maven $MVN_VERSION"

# ------ 3. 配置 Git Hooks ------
echo "--- [3/4] 配置 Git Hooks ---"
if [ -d ".git" ]; then
    git config core.hooksPath scripts/githooks
    echo -e "${GREEN}PASS${NC}: Git hooks 路径已配置 → scripts/githooks"
    echo "  已启用的 hooks:"
    for hook in scripts/githooks/*; do
        if [ -x "$hook" ]; then
            echo "    - $(basename "$hook")"
        fi
    done
else
    echo -e "${YELLOW}WARN${NC}: 非 Git 仓库，跳过 hooks 配置。请先运行 git init。"
fi

# ------ 4. 全量构建 ------
echo "--- [4/4] 全量构建 ---"
echo "  执行 mvn clean install -B ..."
mvn clean install -B
if [ $? -ne 0 ]; then
    echo -e "${RED}FAIL${NC}: 构建失败，请检查错误信息。"
    exit 1
fi
echo -e "${GREEN}PASS${NC}: 全量构建成功"

echo ""
echo "============================================"
echo -e "  ${GREEN}环境搭建完成！${NC}"
echo "============================================"
echo ""
echo "  常用命令:"
echo "    mvn spring-boot:run -pl claude-j-start -Dspring-boot.run.profiles=dev  # 启动应用"
echo "    ./scripts/quick-check.sh                                                # 快速检查"
echo "    ./scripts/entropy-check.sh                                              # 熵检查"
echo "    mvn test                                                                # 全部测试"
echo ""
