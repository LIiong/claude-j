---
name: using-git-worktrees
description: "多任务并行时用 git worktree 做隔离。适用于同时推进 2+ 个独立 task（例如 011-flyway 与 012-multi-profile），避免分支频繁切换和工作区脏污干扰。"
user-invocable: true
disable-model-invocation: true
argument-hint: "<action: new|list|remove> [task-id] [base-branch]"
allowed-tools: "Bash(git worktree *) Bash(git branch *) Bash(git status *) Bash(git log *) Bash(./.claude/skills/using-git-worktrees/scripts/*) Bash(ls *) Bash(cat *) Read Write"
---

# 使用 Git Worktrees（多任务并行隔离）

## 何时用（判定表）

| 场景 | 用 worktree？ |
|------|--------------|
| 单任务顺序走 5 阶段（Spec→...→Ship） | ❌ 不需要 |
| 同时推进 2 个独立任务（如 011 和 012） | ✅ 推荐 |
| 对已归档任务做 hotfix 的同时主干在开发 | ✅ 推荐 |
| 长时间 code review 期间继续写新代码 | ✅ 推荐 |
| 仅仅想分支切换 | ❌ 用 `git switch` 即可 |

## 核心约束

Ralph 流水线依赖**文件系统状态**（`.claude-current-role`、`docs/exec-plan/active/`、`handoff.md`）。这些在每个 worktree **独立存在**，所以：

- 每个 worktree 只处理 **一个 task**
- 不同 worktree 的 Claude 会话**不共享** `.claude-current-role`，角色标记不会互相干扰
- `docs/exec-plan/active/{task-id}/` 在各自 worktree 独立，最终合并到 main 时按任务归档

## 命令入口

```
/using-git-worktrees new <task-id> [base-branch]   # 创建 worktree + 分支
/using-git-worktrees list                           # 列出所有 worktree
/using-git-worktrees remove <task-id>               # 移除 worktree（任务完成后）
```

实际执行 `./.claude/skills/using-git-worktrees/scripts/worktree.sh`。

## 目录约定

```
~/aiProject/
  claude-j/                    # 主 worktree（main 分支）
  claude-j-wt/                 # 副 worktree 父目录（由脚本创建）
    011-flyway-migration/      # 分支 task/011-flyway-migration
    012-multi-profile/         # 分支 task/012-multi-profile
```

- 副 worktree 放在**主目录之外**（`../claude-j-wt/`）避免 IDE 扫描重复代码
- 分支名自动为 `task/{task-id}`
- 每个 worktree 内可独立启动 `claude` CLI

## 典型工作流

### 场景：同时推进 011 和 012

```bash
# 1. 主 worktree（claude-j/）仍在 main，用来做 review / 查资料
cd ~/aiProject/claude-j

# 2. 为 011 开 worktree
/using-git-worktrees new 011-flyway-migration
# → 创建 ~/aiProject/claude-j-wt/011-flyway-migration/ 分支 task/011-flyway-migration
# → 输出：cd ~/aiProject/claude-j-wt/011-flyway-migration && claude

# 3. 新终端进入 worktree 跑 Ralph
cd ~/aiProject/claude-j-wt/011-flyway-migration
claude
# 在 claude 里：/ralph 011-flyway-migration "Flyway 数据库迁移..."

# 4. 回主 worktree 开 012（或开第二个副 worktree）
cd ~/aiProject/claude-j
/using-git-worktrees new 012-multi-profile

# 5. 011 完成合并后清理
/using-git-worktrees remove 011-flyway-migration
```

### 场景：code review 期间写新代码

```bash
# PR 审阅用主 worktree（切到 PR 分支查看）
cd ~/aiProject/claude-j
git fetch origin pull/123/head:pr-123
git switch pr-123

# 同时在副 worktree 继续写主干任务
cd ~/aiProject/claude-j-wt/013-xxx
# 不会被 pr-123 的代码干扰
```

## 冲突风险与规避

| 风险 | 规避 |
|------|------|
| 两个 worktree 修改同一文件 | 每个 worktree 限定一个 task 的 docs/exec-plan/active 子目录；代码改动按聚合隔离（011 动 infra，012 动 start config —— 冲突面小） |
| 共享 `.claude-current-role` 冲突 | worktree 各有独立 `.claude-current-role` 文件（gitignored） |
| maven 本地仓库并发 | `~/.m2/repository` 是共享的；并发 `mvn install` 罕见冲突，出现则串行跑 |
| Hook 路径混乱 | Hook 用 `$CLAUDE_PROJECT_DIR`，每个 worktree 独立 resolve，不会串台 |
| IDE 索引重复 | 副 worktree 放主目录外；必要时在 IDE 排除 `claude-j-wt/` |

## 合并回主干

每个 task 在其 worktree 内走完 Ralph 5 阶段（包括 Ship 归档到 `docs/exec-plan/archived/`），然后：

```bash
# 在 worktree 内
git push origin task/011-flyway-migration
# GitHub 开 PR，合并到 main

# 合并后清理
cd ~/aiProject/claude-j
git pull
/using-git-worktrees remove 011-flyway-migration
```

## 不要做

- ❌ 不要在同一 worktree 内同时跑多个 task（Ralph 状态会打架）
- ❌ 不要用 worktree 做 hot-swap 角色（@dev/@qa 通过子 agent 隔离，不需要 worktree）
- ❌ 不要 `git worktree add` 到 `claude-j/` 子目录（会被 maven/IDE 当作内部模块）
- ❌ 不要忘记 `remove`（长期遗留的 worktree 会让 `.git/worktrees/` 膨胀）

## 参考

- Git 官方文档：`man git-worktree`
- `dispatching-parallel-agents` skill — 单任务内的子 agent 并行（更细粒度）
