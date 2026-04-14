#!/bin/bash
# =================================================================
# guard-java-layer.sh — PreToolUse Hook: 分层架构守护
# 在 Edit/Write 工具执行前检查目标文件是否违反架构约束
# 违规时 exit 2 阻断写入，stderr 输出违规详情
# =================================================================

set -euo pipefail

INPUT=$(cat)

# 使用 python3 解析 JSON（避免 jq 依赖）
eval "$(echo "$INPUT" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    tool = data.get('tool_name', '')
    ti = data.get('tool_input', {})
    fp = ti.get('file_path', '')
    if tool == 'Write':
        content = ti.get('content', '')
    elif tool == 'Edit':
        content = ti.get('new_string', '')
    else:
        content = ''
    # Escape single quotes for shell
    fp = fp.replace(\"'\", \"'\\\"'\\\"'\")
    content = content.replace(\"'\", \"'\\\"'\\\"'\")
    print(f\"TOOL_NAME='{tool}'\")
    print(f\"FILE_PATH='{fp}'\")
    # Write content to a temp approach - use base64 to avoid escaping issues
    import base64
    b64 = base64.b64encode(content.encode()).decode()
    print(f\"CONTENT_B64='{b64}'\")
except:
    print(\"TOOL_NAME=''\")
    print(\"FILE_PATH=''\")
    print(\"CONTENT_B64=''\")
" 2>/dev/null)"

# 如果解析失败或非目标工具，放行
if [ -z "$TOOL_NAME" ] || { [ "$TOOL_NAME" != "Write" ] && [ "$TOOL_NAME" != "Edit" ]; }; then
    exit 0
fi

# 解码内容
CONTENT=$(echo "$CONTENT_B64" | base64 -d 2>/dev/null || echo "")

# 非 Java 文件直接放行
if [[ "$FILE_PATH" != *.java ]]; then
    exit 0
fi

# 测试文件放行（测试代码中可以使用 Spring 等框架）
if [[ "$FILE_PATH" == */src/test/* ]]; then
    exit 0
fi

VIOLATIONS=""

# ------ 规则 1: Domain 层纯净性 ------
if [[ "$FILE_PATH" == *claude-j-domain/* ]]; then
    if echo "$CONTENT" | grep -qE 'import\s+org\.springframework'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Domain 层禁止 Spring 依赖: import org.springframework.*"
    fi
    if echo "$CONTENT" | grep -qE 'import\s+com\.baomidou'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Domain 层禁止 MyBatis-Plus 依赖: import com.baomidou.*"
    fi
    if echo "$CONTENT" | grep -qE 'import\s+javax\.persistence'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Domain 层禁止 JPA 依赖: import javax.persistence.*"
    fi
    if echo "$CONTENT" | grep -qE '@(Service|Component|Repository|Autowired|Bean)\b'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Domain 层禁止 Spring 注解: @Service/@Component/@Repository/@Autowired/@Bean"
    fi
fi

# ------ 规则 2: 依赖方向 ------
if [[ "$FILE_PATH" == *claude-j-application/*src/main/* ]]; then
    if echo "$CONTENT" | grep -qE 'import\s+com\.claudej\.infrastructure'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Application 层禁止导入 Infrastructure（违反依赖方向: app -> domain <- infra）"
    fi
    if echo "$CONTENT" | grep -qE 'import\s+com\.claudej\.adapter'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Application 层禁止导入 Adapter（违反依赖方向）"
    fi
fi

if [[ "$FILE_PATH" == *claude-j-adapter/*src/main/* ]]; then
    if echo "$CONTENT" | grep -qE 'import\s+com\.claudej\.infrastructure'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Adapter 层禁止导入 Infrastructure（违反依赖方向: adapter -> app -> domain）"
    fi
fi

if [[ "$FILE_PATH" == *claude-j-domain/*src/main/* ]]; then
    if echo "$CONTENT" | grep -qE 'import\s+com\.claudej\.(application|infrastructure|adapter)'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Domain 层禁止导入其他层（Domain 是最内层，不依赖任何外层）"
    fi
fi

# ------ 规则 3: DO 泄漏检查 ------
if [[ "$FILE_PATH" != *claude-j-infrastructure/* ]]; then
    if echo "$CONTENT" | grep -qE 'import\s+com\.claudej\.infrastructure.*dataobject'; then
        VIOLATIONS="${VIOLATIONS}\n[BLOCKED] DO 对象禁止泄漏到 Infrastructure 层之外"
    fi
fi

# ------ 规则 4: Java 8 兼容性 ------
if echo "$CONTENT" | grep -qP '^\s+var\s+\w+\s*=' 2>/dev/null; then
    VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Java 8 禁止 var 关键字（使用显式类型声明）"
fi
if echo "$CONTENT" | grep -qE 'List\.of\(|Map\.of\(|Set\.of\('; then
    VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Java 8 禁止 List.of()/Map.of()/Set.of()（使用 Arrays.asList()/Collections.*）"
fi
if echo "$CONTENT" | grep -q '"""'; then
    VIOLATIONS="${VIOLATIONS}\n[BLOCKED] Java 8 禁止 text blocks（使用字符串拼接）"
fi

# ------ 输出结果 ------
if [ -n "$VIOLATIONS" ]; then
    echo -e "架构守护拦截 -- 文件: $FILE_PATH$VIOLATIONS" >&2
    echo -e "\n请修正后重试。参考: docs/standards/java-dev.md" >&2
    exit 2
fi

exit 0
