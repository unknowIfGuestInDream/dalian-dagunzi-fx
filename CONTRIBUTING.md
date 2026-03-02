# 贡献指南

欢迎为本项目贡献代码，让这个项目变得更好！

欢迎提交 Pull Request 或 Issue，但请确保使用相应的模板。

我们采用了一系列自动化工具进行代码检查，不符合规范的贡献可能会被拒绝。

以下是我们希望您遵循的贡献规范：

## 代码风格指南

### Commit 消息规范

每次提交应包含相对独立的变更（即不允许在一次 commit 中混合提交多种类型的修改），并在消息中明确说明具体变更内容。

本项目的 commit 消息规范主要参考广泛使用的 [AngularJS Git Commit Message Conventions](https://docs.google.com/document/d/1QrDFcIiPjSLDn3EL15IJygNPiHORgU1_OOAqWjiDU5Y/edit#heading=h.uyo6cb12dt6w)。

消息格式如下：

> `<类型>(<范围>): <主题>`
>
> // 空行
>
> `<正文>`
>
> // 空行
>
> `<页脚>`

`<头部>`（第一行）对任何项目都是必填项。`<正文>` 和 `<页脚>` 可根据实际情况选填。

各部分之间必须有一个空行。

此外，`<头部>`（仅一行）不得超过 50 个字符，`<正文>` 中每行不得超过 72 个字符。

这样可以使 commit 消息在 GitHub 及各种 Git 工具中更易于阅读。

#### 关于 `<头部>` 部分

`<头部>` 仅包含一行，其中三个字段（`<类型>`、`<范围>` 和 `<主题>`）需满足以下要求：

`类型` 字段主要说明本次提交的类型。在 `AngularJS Git Commit Message Conventions` 中，仅允许使用以下 9 种类型：

- feat：新功能
- fix：Bug 修复
- docs：仅修改文档
- style：不影响代码含义的变更（空格、格式化、缺少分号等）
- refactor：既不修复 Bug 也不新增功能的代码重构
- perf：提升性能的代码变更
- test：添加缺失的测试或修正现有测试
- chore：对构建流程或辅助工具及库（如文档生成）的变更
- revert：如果本次 commit 撤销了之前的提交，应跟上被撤销 commit 的 `<头部>`，正文中注明：`This reverts commit <hash>.`，其中 hash 为被撤销 commit 的 SHA

对于本项目，必要时还可使用以下 2 种类型：

- build：构建工具或依赖项的变更（webpack、npm 等）
- ci：持续集成相关的变更

如果一次提交涉及多种 `<类型>`，优先使用 `feat` 和 `fix`，其次为 `AngularJS Git Commit Message Conventions` 中规定的其余七种类型，最后两种仅用于特殊需求。

`<范围>` 主要描述本次提交的影响范围，通常为文件、路径或功能。例如，可填写修改的文件名（若修改了多个文件，可使用模块名或项目名），也可填写受影响的功能。若为全局影响，可使用字符 `*`。

`<主题>` 主要概括本次提交的目的和变更内容，应以动词开头，使用祈使句、现在时态，首字母小写，结尾不加句点（.）。

#### 关于 `<正文>` 部分

`<正文>` 为文本部分，包含本次提交的详细说明，应使用祈使句、现在时态。

如果 `<头部>` 已足以概括本次提交的全部变更，可省略此部分。

建议使用短横线（-）创建无序列表，说明本次提交解决了什么问题、如何解决，以及是否引入了其他变更（如必要的文档更新等）。

#### 关于 `<页脚>` 部分

`<页脚>` 通常可省略，仅在以下 2 种情况下使用：

一是破坏性变更，即当前版本与上一版本不兼容。应以 `BREAKING CHANGE:` 开头，后跟一个空格或两个换行符，其余内容为变更描述、理由和迁移说明。

二是关联本次提交关闭的 GitHub Issues。使用 `Closes #123, #456` 格式来关闭一个或多个 Issue。

#### Commit 消息示例

以下是一些 commit 消息示例：

> 例如，若新功能是为贡献者头像添加圆形选项，commit 消息可以写成：

```text
feat(contributor): add a option for round avatar

- add a option to choose the avatar in circle or in square
- add new template in the python script to handle it
- update usage and example in README.md

Closes #123
```

> 若新增了 Linux 命令 ls 的文档，commit 消息可以写成：

```text
docs(command): add linux command ls

- add basic usage format of command ls
- add arguments of command ls
- add considerations of command ls
- plan to add more typical examples in future
- plan to add descriptions in the future
```

> 若修复了文档 ls.md 中的一个错别字，commit 消息可以写成：

```text
docs(ls.md): fix a typo

- change `-` to `--`

Closes #456
```

### Pull Request 规范

> **注意**：请使用 `rebase` 或 `pull --rebase` 方式更新您的分支，以保持提交历史的整洁。

本项目已配置多个自动化检查工具。提交 Pull Request 后，请稍等片刻，并根据检查工具的评论和详情处理相关问题。

#### 分支命名

建议从一个与变更内容相关的新分支提交 Pull Request（PR）。**从 master 分支提交 PR 可能会给您的后续工作带来麻烦**，因为从分支提交 PR 后，您仍可通过向该分支提交和推送来更新 PR，而 master 分支可用于跟踪最新变更。

对于本项目，PR 的分支命名应遵循以下规范：

- 新功能分支应以 `feature/` 开头，后跟具体功能名称，例如：开发新功能 `md2pdf` 用 `feature/md2pdf`，优化该功能用 `feature/optimize_md2pdf`
- Bug 修复分支应以 `fix/` 开头，后跟修复的功能名称，例如：修复 `yapf` 功能的 Bug 用 `fix/yapf`
- 仅修改文档的分支应以 `docs/` 开头，后跟文档变更范围，例如：修改使用说明文档用 `docs/usage`
- 其他情况，请先提交 Issue 与维护者讨论

#### Pull Request 标题

PR 标题应概括变更内容，并以表示变更类型的前缀开头。以下是一些示例：

新功能的标题应以 **`feature(<您的新功能>):`** 开头

Bug 修复的标题应以 **`fix(<您修复的功能>)`** 开头

文档变更的标题应以 **`docs(<您的文档变更范围>)`** 开头

#### Pull Request 描述

请遵循 [pull_request_template](.github/PULL_REQUEST_TEMPLATE.md) 来描述 PR 的变更内容，以便审阅者更清楚地了解您的改动，此部分不可为空。

描述中应说明本次 PR 的动机（例如解决了什么问题、优化了什么功能），详细描述已实现的功能，并介绍所使用的技术栈。还需声明其他必要的变更（如相关文档的更新）。

建议使用 `任务列表` 格式描述变更步骤或技术栈。所有 `草稿 Pull Request` 建议在描述中包含 `任务列表`，并随开发进展持续更新。

> 任务列表格式如下：
>
> [x] 这是您已完成的内容及实现方式。
>
> [ ] 这是您计划完成的内容及实现方案。

如果本次 PR 修复了某个 Issue，应在描述中使用正确格式进行关联。可使用 `Resolves: #123` 或 `Closes: #123` 格式，在 PR 合并时自动关闭该 Issue；使用 `Ref: #123` 格式仅作引用。

此外，请确保您的 PR 与任何已指派的 Issue 或现有 PR 不重复。**修改 HTML 或 CSS 文件时需附上截图**。

### 语言风格

不同语言的文件应按照以下规范在本地进行检查。

所有检查通过后再进行提交。
