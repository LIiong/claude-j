# 项目模板目录

本目录存放可移植到其他项目的**通用模板**，作为 `PORTING.md` 的实操素材。

## 内容

| 文件 | 用途 |
|------|------|
| `variables.md` | 所有占位变量的定义与默认值 |
| `CLAUDE.template.md` | 项目入口指令文件（CLAUDE.md）模板 |
| `architecture-overview.template.md` | 架构概览文档模板 |

## 使用流程

1. 阅读 `variables.md`，明确要替换的变量集
2. 选择一种替换方法（手动 / `sed` / `envsubst` / `bootstrap-project.sh`）
3. 将 `*.template.md` 中的 `${VARIABLE}` 全部替换为实际值
4. 将替换后的文件放入新项目的对应位置：
   - `CLAUDE.template.md` → 新项目根目录 `CLAUDE.md`
   - `architecture-overview.template.md` → 新项目 `docs/architecture/overview.md`

## 自动化

直接运行：
```bash
./scripts/bootstrap-project.sh \
  --project-name "my-app" \
  --package-root "com.mycompany" \
  --description "我的 DDD 项目" \
  --target-dir /path/to/new-project
```

脚本会自动：
1. 复制 `.claude/`、`docs/exec-plan/templates/`、`docs/templates/`、`scripts/hooks/` 到目标项目
2. 对所有文件做变量替换
3. 生成初始 `CLAUDE.md`、`docs/architecture/overview.md`
4. 初始化 `.claude-current-role`、`.gitignore` 等运行时文件

## 注意事项

- 这些模板仅覆盖"方法论级"资产，不包含具体业务代码
- 架构测试（ArchUnit 等）、熵扫描脚本在非 Java 项目中需按语言重写
- 替换后请运行 `/ralph 001-hello-world 实现一个 hello 端点` 走通全流程以验证移植
