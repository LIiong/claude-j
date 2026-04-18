# Ralph Skill — 打包脚本

本目录存放 ralph skill 的配套脚本。脚本与 skill 打包后可整体移植。

## 脚本清单

| 脚本 | 作用 | 被调用方 |
|------|------|---------|
| `ralph-init.sh` | 初始化 Ralph Loop 环境（从 task-plan 生成 progress.md） | 用户（终端）/ ralph skill 模式 C |
| `ralph-loop.sh` | Ralph Loop 多会话迭代器（每轮启动全新 Claude Code 会话） | 用户（终端）/ ralph skill 模式 C |
| `ralph-auto.sh` | 全自动交付（Shell 入口，等价于 skill 模式 A） | 用户（终端） |

## 使用方式

**推荐**（通过 skill 调用）：
```
/ralph 001-feature-name 需求描述
```

**直接运行**（终端）：
```bash
# 通过符号链接（兼容旧路径）
./scripts/ralph-auto.sh 001-feature-name "需求描述"

# 或使用规范路径
./.claude/skills/ralph/scripts/ralph-auto.sh 001-feature-name "需求描述"
```

## 符号链接

为兼容历史 `./scripts/ralph-*.sh` 调用方式，`scripts/` 下保留对本目录的符号链接：
```
scripts/ralph-init.sh  → ../.claude/skills/ralph/scripts/ralph-init.sh
scripts/ralph-loop.sh  → ../.claude/skills/ralph/scripts/ralph-loop.sh
scripts/ralph-auto.sh  → ../.claude/skills/ralph/scripts/ralph-auto.sh
```

## 移植

将 `.claude/skills/ralph/` 整个目录（含本 scripts/ 子目录）复制到新项目即可。需在新项目 `scripts/` 下重建符号链接以兼容旧路径。
