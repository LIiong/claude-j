#!/usr/bin/env bash
# bootstrap-project.sh — 从 claude-j 方法论模板初始化新项目
# 用法示例：
#   ./scripts/bootstrap-project.sh \
#     --project-name "my-app" \
#     --package-root "com.mycompany" \
#     --description "我的 DDD 项目" \
#     --target-dir /path/to/new-project

set -euo pipefail

# ─── 参数解析 ────────────────────────────────────────────────
PROJECT_NAME=""
PROJECT_DESCRIPTION=""
PACKAGE_ROOT=""
TARGET_DIR=""
LANGUAGE="Java"
LANGUAGE_VERSION="Java 8"
FRAMEWORK="Spring Boot 2.7.18"
BUILD_TOOL="Maven"
ORM="MyBatis-Plus 3.5.5"
MAPPER_TOOL="MapStruct 1.5.5"
TEST_FRAMEWORK="JUnit 5 + AssertJ + Mockito"
DB_DEV="H2"
DB_PROD="MySQL 8"
DRY_RUN="false"

usage() {
    cat <<EOF
用法：$0 [选项]

必需参数：
  --project-name NAME         新项目名（kebab-case，如 my-app）
  --package-root PKG          根包名（如 com.mycompany）
  --description TEXT          一句话项目描述
  --target-dir PATH           目标项目目录（不存在则创建）

可选参数（默认值为 claude-j 的值）：
  --language LANG             语言（默认 Java）
  --language-version VER      语言版本（默认 "Java 8"）
  --framework FW              主框架（默认 "Spring Boot 2.7.18"）
  --build-tool TOOL           构建工具（默认 Maven）
  --orm ORM                   ORM 框架（默认 "MyBatis-Plus 3.5.5"）
  --mapper-tool MAPPER        对象映射工具（默认 "MapStruct 1.5.5"）
  --test-framework FW         测试栈（默认 "JUnit 5 + AssertJ + Mockito"）
  --db-dev DB                 开发数据库（默认 H2）
  --db-prod DB                生产数据库（默认 "MySQL 8"）
  --dry-run                   仅打印将执行的操作，不实际写入

示例：
  $0 --project-name my-app \\
     --package-root com.mycompany \\
     --description "我的 DDD 项目" \\
     --target-dir ~/projects/my-app
EOF
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --project-name)        PROJECT_NAME="$2"; shift 2 ;;
        --package-root)        PACKAGE_ROOT="$2"; shift 2 ;;
        --description)         PROJECT_DESCRIPTION="$2"; shift 2 ;;
        --target-dir)          TARGET_DIR="$2"; shift 2 ;;
        --language)            LANGUAGE="$2"; shift 2 ;;
        --language-version)    LANGUAGE_VERSION="$2"; shift 2 ;;
        --framework)           FRAMEWORK="$2"; shift 2 ;;
        --build-tool)          BUILD_TOOL="$2"; shift 2 ;;
        --orm)                 ORM="$2"; shift 2 ;;
        --mapper-tool)         MAPPER_TOOL="$2"; shift 2 ;;
        --test-framework)      TEST_FRAMEWORK="$2"; shift 2 ;;
        --db-dev)              DB_DEV="$2"; shift 2 ;;
        --db-prod)             DB_PROD="$2"; shift 2 ;;
        --dry-run)             DRY_RUN="true"; shift ;;
        -h|--help)             usage ;;
        *) echo "未知参数: $1"; usage ;;
    esac
done

# 必填校验
for var in PROJECT_NAME PACKAGE_ROOT PROJECT_DESCRIPTION TARGET_DIR; do
    if [[ -z "${!var}" ]]; then
        echo "❌ 缺少参数: --${var,,}"
        usage
    fi
done

# 派生变量
MODULE_DOMAIN="${PROJECT_NAME}-domain"
MODULE_APPLICATION="${PROJECT_NAME}-application"
MODULE_INFRASTRUCTURE="${PROJECT_NAME}-infrastructure"
MODULE_ADAPTER="${PROJECT_NAME}-adapter"
MODULE_START="${PROJECT_NAME}-start"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SOURCE_ROOT="$( cd "${SCRIPT_DIR}/.." && pwd )"

# ─── 打印配置 ────────────────────────────────────────────────
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  claude-j 移植脚手架"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "源项目          : ${SOURCE_ROOT}"
echo "目标项目        : ${TARGET_DIR}"
echo "项目名          : ${PROJECT_NAME}"
echo "描述            : ${PROJECT_DESCRIPTION}"
echo "包根            : ${PACKAGE_ROOT}"
echo "语言            : ${LANGUAGE_VERSION}"
echo "框架            : ${FRAMEWORK}"
echo "构建工具        : ${BUILD_TOOL}"
echo "ORM             : ${ORM}"
echo "Mapper          : ${MAPPER_TOOL}"
echo "测试栈          : ${TEST_FRAMEWORK}"
echo "开发 DB         : ${DB_DEV}"
echo "生产 DB         : ${DB_PROD}"
echo "Dry-run         : ${DRY_RUN}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [[ "${DRY_RUN}" == "false" ]]; then
    read -rp "确认执行？[y/N] " confirm
    [[ "${confirm}" == "y" || "${confirm}" == "Y" ]] || { echo "取消"; exit 0; }
fi

# ─── 创建目标目录 ─────────────────────────────────────────────
run() {
    if [[ "${DRY_RUN}" == "true" ]]; then
        echo "[DRY-RUN] $*"
    else
        eval "$@"
    fi
}

run "mkdir -p '${TARGET_DIR}'"
run "mkdir -p '${TARGET_DIR}/docs/architecture/decisions'"
run "mkdir -p '${TARGET_DIR}/docs/exec-plan/active'"
run "mkdir -p '${TARGET_DIR}/docs/exec-plan/archived'"
run "mkdir -p '${TARGET_DIR}/docs/standards'"
run "mkdir -p '${TARGET_DIR}/docs/guides'"

# ─── 复制方法论资产 ──────────────────────────────────────────
# 打包结构说明：
#   - Skill 专属脚本放在 .claude/skills/<skill>/scripts/，随 .claude 一并复制
#   - scripts/ 下只保留独立脚本（hooks/, setup.sh 等）+ 指向 skill scripts 的符号链接
echo ""
echo "▸ 复制方法论资产..."
run "mkdir -p '${TARGET_DIR}/scripts'"
run "cp -r '${SOURCE_ROOT}/.claude'                        '${TARGET_DIR}/.claude'"
run "cp -r '${SOURCE_ROOT}/docs/exec-plan/templates'       '${TARGET_DIR}/docs/exec-plan/templates'"
run "cp -r '${SOURCE_ROOT}/docs/templates'                 '${TARGET_DIR}/docs/templates'"
run "cp -r '${SOURCE_ROOT}/scripts/hooks'                  '${TARGET_DIR}/scripts/hooks'"
run "cp -r '${SOURCE_ROOT}/scripts/githooks'               '${TARGET_DIR}/scripts/githooks' 2>/dev/null || true"
run "cp    '${SOURCE_ROOT}/scripts/setup.sh'               '${TARGET_DIR}/scripts/setup.sh' 2>/dev/null || true"
run "cp    '${SOURCE_ROOT}/scripts/claude-gate.sh'         '${TARGET_DIR}/scripts/claude-gate.sh' 2>/dev/null || true"
run "cp    '${SOURCE_ROOT}/scripts/dev-gate.sh'            '${TARGET_DIR}/scripts/dev-gate.sh' 2>/dev/null || true"
run "cp '${SOURCE_ROOT}/PORTING.md'                        '${TARGET_DIR}/PORTING.md'"
run "cp '${SOURCE_ROOT}/docs/architecture/decisions/000-template.md' '${TARGET_DIR}/docs/architecture/decisions/000-template.md' || true"

# ─── 重建打包脚本符号链接 ────────────────────────────────────
# Skill 脚本已随 .claude 复制，此处重建 scripts/ 下的符号链接保证向后兼容
echo ""
echo "▸ 重建 scripts/ 下的符号链接..."
for link_pair in \
    "ralph-init.sh:../.claude/skills/ralph/scripts/ralph-init.sh" \
    "ralph-loop.sh:../.claude/skills/ralph/scripts/ralph-loop.sh" \
    "ralph-auto.sh:../.claude/skills/ralph/scripts/ralph-auto.sh" \
    "entropy-check.sh:../.claude/skills/full-check/scripts/entropy-check.sh" \
    "quick-check.sh:../.claude/skills/full-check/scripts/quick-check.sh"; do
    name="${link_pair%%:*}"
    target="${link_pair##*:}"
    run "ln -sfn '${target}' '${TARGET_DIR}/scripts/${name}'"
done

# ─── 变量替换 ────────────────────────────────────────────────
echo ""
echo "▸ 变量替换..."

# 根据 OS 选择 sed 兼容语法
SED_INPLACE="sed -i"
if [[ "$(uname)" == "Darwin" ]]; then
    SED_INPLACE="sed -i ''"
fi

substitute_vars() {
    local file="$1"
    if [[ "${DRY_RUN}" == "true" ]]; then
        echo "[DRY-RUN] substitute: ${file}"
        return
    fi
    [[ -f "${file}" ]] || return

    ${SED_INPLACE} -e "s|\${PROJECT_NAME}|${PROJECT_NAME}|g" \
                   -e "s|\${PROJECT_DESCRIPTION}|${PROJECT_DESCRIPTION}|g" \
                   -e "s|\${PACKAGE_ROOT}|${PACKAGE_ROOT}|g" \
                   -e "s|\${LANGUAGE}|${LANGUAGE}|g" \
                   -e "s|\${LANGUAGE_VERSION}|${LANGUAGE_VERSION}|g" \
                   -e "s|\${FRAMEWORK}|${FRAMEWORK}|g" \
                   -e "s|\${BUILD_TOOL}|${BUILD_TOOL}|g" \
                   -e "s|\${ORM}|${ORM}|g" \
                   -e "s|\${MAPPER_TOOL}|${MAPPER_TOOL}|g" \
                   -e "s|\${TEST_FRAMEWORK}|${TEST_FRAMEWORK}|g" \
                   -e "s|\${DB_DEV}|${DB_DEV}|g" \
                   -e "s|\${DB_PROD}|${DB_PROD}|g" \
                   -e "s|\${MODULE_DOMAIN}|${MODULE_DOMAIN}|g" \
                   -e "s|\${MODULE_APPLICATION}|${MODULE_APPLICATION}|g" \
                   -e "s|\${MODULE_INFRASTRUCTURE}|${MODULE_INFRASTRUCTURE}|g" \
                   -e "s|\${MODULE_ADAPTER}|${MODULE_ADAPTER}|g" \
                   -e "s|\${MODULE_START}|${MODULE_START}|g" \
                   -e "s|\${ARCHUNIT_RULE_COUNT}|14|g" \
                   -e "s|\${ENTROPY_CHECK_COUNT}|12|g" \
                   -e "s|\${TABLE_PREFIX}|t_|g" \
                   -e "s|\${COLUMN_CASE}|snake_case|g" \
                   -e "s|\${CMD_BUILD_ALL}|${BUILD_TOOL,,} clean install|g" \
                   -e "s|\${CMD_RUN_TESTS}|${BUILD_TOOL,,} test|g" \
                   -e "s|\${CMD_RUN_DEV}|${BUILD_TOOL,,} spring-boot:run -pl ${MODULE_START} -Dspring-boot.run.profiles=dev|g" \
                   -e "s|\${CMD_STYLE_CHECK}|${BUILD_TOOL,,} checkstyle:check|g" \
                   -e "s|\${CMD_QUICK_CHECK}|./scripts/quick-check.sh|g" \
                   -e "s|\${CMD_ENTROPY_CHECK}|./scripts/entropy-check.sh|g" \
                   "${file}"
}

# 生成核心文件
if [[ "${DRY_RUN}" == "false" ]]; then
    cp "${SOURCE_ROOT}/docs/templates/project/CLAUDE.template.md"               "${TARGET_DIR}/CLAUDE.md"
    cp "${SOURCE_ROOT}/docs/templates/project/architecture-overview.template.md" "${TARGET_DIR}/docs/architecture/overview.md"
    substitute_vars "${TARGET_DIR}/CLAUDE.md"
    substitute_vars "${TARGET_DIR}/docs/architecture/overview.md"
fi

# ─── 初始化运行时 ────────────────────────────────────────────
echo ""
echo "▸ 初始化运行时文件..."
run "touch '${TARGET_DIR}/.claude-current-role'"
run "echo '.claude-current-role' >> '${TARGET_DIR}/.gitignore'"

# ─── 完成提示 ────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  ✅ 脚手架完成"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "下一步："
echo "  1. cd ${TARGET_DIR}"
echo "  2. 查看并调整 CLAUDE.md"
echo "  3. 按 PORTING.md 完成 L1/L2/L3 守护适配"
echo "  4. 初始化构建骨架（${BUILD_TOOL}）"
echo "  5. 在 Claude Code 中测试：/ralph 001-hello-world 实现一个 hello 端点"
echo ""
