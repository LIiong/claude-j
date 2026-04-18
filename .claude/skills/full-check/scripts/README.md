# Full-Check Skill — 打包脚本

本目录存放 full-check skill 的配套验证脚本。这些脚本也被 dev-build、architect-review、qa-verify 等其他 skills 间接引用。

## 脚本清单

| 脚本 | 作用 | 被调用方 |
|------|------|---------|
| `entropy-check.sh` | 架构漂移检测（12 项全局一致性扫描，L3 守护） | full-check / dev-build / architect-review / qa-verify / post-commit Hook |
| `quick-check.sh` | 快速三项检查（编译 + 单测 + 风格） | full-check / dev-build / post-commit Hook |

## 使用方式

**通过 skill 调用**（推荐）：
```
/full-check
```

**直接运行**（终端或其他 skill 内部）：
```bash
# 通过符号链接（兼容旧路径）
./scripts/entropy-check.sh
./scripts/quick-check.sh

# 或使用规范路径
./.claude/skills/full-check/scripts/entropy-check.sh
./.claude/skills/full-check/scripts/quick-check.sh
```

## 为什么放在 full-check skill 里

这两个脚本本质上是 full-check skill 的核心实现 —— full-check 做的事就是"运行这两个脚本 + mvn test 并汇总结果"。其他 skill（dev-build / qa-verify 等）对这两个脚本的依赖可理解为"调用 full-check 的子能力"。

## 符号链接

为兼容 Hook 与 CLI 的 `./scripts/entropy-check.sh` 调用方式，`scripts/` 下保留符号链接：
```
scripts/entropy-check.sh → ../.claude/skills/full-check/scripts/entropy-check.sh
scripts/quick-check.sh   → ../.claude/skills/full-check/scripts/quick-check.sh
```

**重要**：修改这两个脚本后，所有引用方自动跟进（符号链接透明）。

## 移植

将 `.claude/skills/full-check/` 整个目录复制到新项目即可。新项目的 `scripts/` 需重建符号链接。
