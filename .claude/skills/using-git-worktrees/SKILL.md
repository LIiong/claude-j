---
name: using-git-worktrees
description: "多任务并行时用 git worktree 做物理隔离。适用于同时推进 2+ 个独立 feature/bug 分支，避免在一个工作区里频繁 checkout、工作区脏污互相干扰。附带 worktree.sh 管理脚本（new/list/remove）。"
user-invocable: true
disable-model-invocation: true
argument-hint: "<action: new|list|remove> [task-id] [base-branch]"
allowed-tools: "Bash(git worktree *) Bash(git branch *) Bash(git status *) Bash(git log *) Bash(./.claude/skills/using-git-worktrees/scripts/*) Bash(ls *) Bash(cat *) Read Write"
---

# 使用 Git Worktrees（多任务并行隔离）

## 何时用（判定表）

| 场景 | 用 worktree？ |
|------|--------------|
| 单任务顺序完成 | ❌ 不需要 |
| 同时推进 2+ 个独立任务 / 分支 | ✅ 推荐 |
| 对已发布版本做 hotfix 的同时主干仍在开发 | ✅ 推荐 |
| 长时间 code review 期间继续写新代码 | ✅ 推荐 |
| 仅想切换分支查看 | ❌ `git switch` 即可 |
| 同一任务内多角色协作（dev / reviewer / QA） | ❌ 子 agent / 独立上下文已够，worktree 反而增加复杂度 |

## 核心约束（必读）

每个 worktree 限定**一个任务**。理由：

- 工作流状态通常存在文件系统（进度文件、角色标记、任务目录）。多任务共用 worktree → 状态互相覆盖。
- 分支与工作目录 1:1 绑定。两个任务混放同一 worktree 等于退化为无 worktree。
- 跨 worktree 的共享资源（maven/node_modules 缓存、CI 数据库）如有并发写冲突，串行执行即可，不要强并行。

## 命令入口

```
/using-git-worktrees new <task-id> [base-branch]   # 创建 worktree + 分支
/using-git-worktrees list                           # 列出所有 worktree
/using-git-worktrees remove <task-id>               # 移除 worktree（任务完成后）
```

实际执行 `./.claude/skills/using-git-worktrees/scripts/worktree.sh`。

## 目录约定

```
~/projects/
  <repo>/                    # 主 worktree（主分支）
  <repo>-wt/                 # 副 worktree 父目录（脚本自动创建于主仓库同级）
    011-feature-a/           # 分支 task/011-feature-a
    012-bugfix-b/            # 分支 task/012-bugfix-b
```

- 副 worktree 放在**主仓库同级**（不是内部），避免 IDE / 构建工具扫描重复代码
- 分支名默认 `task/<task-id>`（脚本会检查冲突）
- 每个 worktree 可独立启动 shell / IDE / claude 会话

## 典型工作流

### 场景：同时推进 2 个任务

```bash
# 1. 主 worktree 仍在主分支，用于 review / 查资料
cd ~/projects/myrepo

# 2. 为任务 A 开 worktree
/using-git-worktrees new 011-feature-a
# → 创建 ~/projects/myrepo-wt/011-feature-a/ 分支 task/011-feature-a

# 3. 新终端进入 worktree 开工
cd ~/projects/myrepo-wt/011-feature-a
# 这里跑任务 A 的全部流程（编码、测试、提交、push）

# 4. 回主仓库开任务 B
cd ~/projects/myrepo
/using-git-worktrees new 012-bugfix-b

# 5. A 合并后清理
/using-git-worktrees remove 011-feature-a
```

### 场景：Review 与开发并行

```bash
# PR 审阅在主 worktree（切到 PR 分支查看）
cd ~/projects/myrepo
git fetch origin pull/123/head:pr-123
git switch pr-123

# 同时在副 worktree 继续主干任务，互不干扰
cd ~/projects/myrepo-wt/013-xxx
```

## 冲突风险与规避

| 风险 | 规避 |
|------|------|
| 两个 worktree 改同一文件 | 任务边界按模块/目录划分，冲突面最小化；最终靠 PR 合并时 rebase 解决 |
| 共享的工具运行时状态（例如单例后台进程、端口占用） | 明确哪个 worktree 是"当前活动"，其他不启动相同端口服务 |
| 包管理缓存并发写（maven/npm/pip） | 绝大多数缓存实现是幂等的；如遇冲突，串行执行安装命令 |
| IDE 索引重复 | 副 worktree 放主仓库外；必要时在 IDE 排除 `*-wt/` |
| Git Hook 路径混乱 | Hook 脚本用工作目录相对路径或 `git rev-parse --show-toplevel` 解析，避免硬编码 |

## 合并回主干

每个 task 在其 worktree 内完成所有步骤后：

```bash
# 在 worktree 内
git push origin task/011-feature-a
# 开 PR，合并到主干

# 合并后回主仓库清理
cd ~/projects/myrepo
git pull
/using-git-worktrees remove 011-feature-a
# （可选）删除本地已合并分支
git branch -d task/011-feature-a
```

## 不要做

- ❌ 同一 worktree 并发处理多个任务（文件状态会互相覆盖）
- ❌ worktree 放在主仓库子目录（构建工具会把它当内部模块）
- ❌ 跨 worktree 共享未提交改动（stash 不跨 worktree；走 commit + cherry-pick）
- ❌ 忘记 `remove`（`.git/worktrees/` 元数据膨胀；脚本会在移除时清理空父目录）
- ❌ 对同一分支在多个 worktree 中 checkout（git 本身会拒绝，但别绕过）

## 与其他 skill 的关系

- `dispatching-parallel-agents` — 单任务内"能不能并行子 agent"的决策；本 skill 是**任务间**的物理隔离，不冲突，两者互补。
- 单任务内多角色（编码 + 评审 + 测试）通过子 agent 独立上下文即可，不需要 worktree。

## 参考

- `man git-worktree`
- Git 官方文档：https://git-scm.com/docs/git-worktree
