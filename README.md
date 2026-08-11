# SQL Heading Folding

[![Build and release](https://github.com/yc-2018/intellij-sql-heading-folding/actions/workflows/build-release.yml/badge.svg)](https://github.com/yc-2018/intellij-sql-heading-folding/actions/workflows/build-release.yml)
[![Continuous release](https://img.shields.io/github/v/release/yc-2018/intellij-sql-heading-folding?include_prereleases&label=download)](https://github.com/yc-2018/intellij-sql-heading-folding/releases/tag/continuous)

Hierarchical headings, native folding, reading-mode labels, and outline navigation for SQL consoles in every JetBrains IDE that provides Database Tools and SQL.

为所有提供 Database Tools and SQL 的 JetBrains IDE 提供 SQL 分级标题、原生折叠、阅读态标签和目录导航。

## Features / 功能

- Define five heading levels with `-- #` through `-- #####`. / 使用 `-- #` 到 `-- #####` 定义五级标题。
- Fold each section with the native editor gutter control. / 使用编辑器左侧原生控件折叠整个标题区块。
- Show compact `H1` to `H5` labels and bold titles when the caret leaves a heading line. / 光标离开标题行后显示紧凑的 `H1` 到 `H5` 标签并加粗标题。
- Restore the original SQL comment as soon as the caret returns to the heading line. / 光标返回标题行时立即恢复原始 SQL 注释，方便编辑。
- Color comments with `r/y/b/g/c/o/p/m` for red, yellow, blue, green, cyan, orange, purple, or magenta; use `-- @@` for bold and combine both as `-- @@r`. / 使用 `r/y/b/g/c/o/p/m` 设置红、黄、蓝、绿、青、橙、紫、品红；使用 `-- @@` 加粗，也可组合为 `-- @@r`。
- Navigate all headings from the **SQL Headings** tool window. / 在 **SQL Headings** 工具窗口中浏览并跳转全部标题。
- Refresh, collapse all sections, or expand all sections from the tool window toolbar. / 支持刷新、全部折叠和全部展开。

## Usage / 使用方法

Write headings in a SQL console or `.sql` file:

在 SQL 控制台或 `.sql` 文件中编写标题：

```sql
-- # User analysis / 用户分析
select * from users;

-- ## Active users / 活跃用户
select * from users where active = true;

-- # Order analysis / 订单分析
select * from orders;

-- @r Red note / 红色说明
-- @@ Bold note / 加粗说明
-- @@g Bold green note / 加粗绿色说明
-- @c Cyan note / 青色说明
-- @@p Bold purple note / 加粗紫色说明
```

Section boundaries follow heading levels. An `H2` section ends at the next `H1` or `H2`; an `H1` section ends at the next `H1`.

区块边界遵循标题层级：二级标题在下一个一级或二级标题前结束，一级标题在下一个一级标题前结束。

## Compatibility / 兼容性

| Product / 产品 | Supported versions / 支持版本 | Notes / 说明 |
| --- | --- | --- |
| IntelliJ IDEA Ultimate | 2023.2 and later / 2023.2 及后续版本 | Primary target; Database Tools is bundled / 重点测试产品，已内置 Database Tools |
| DataGrip | 2023.2 and later / 2023.2 及后续版本 | Primary target; Database Tools is bundled / 重点测试产品，已内置数据库功能 |
| Other JetBrains IDEs / 其他 JetBrains IDE | 2023.2 and later / 2023.2 及后续版本 | Supported whenever the product provides `com.intellij.database`; Marketplace exposes all eligible products automatically / 只要产品提供该依赖就支持，Marketplace 会自动覆盖全部符合条件的产品 |
| Products without Database Tools / 不含数据库工具的产品 | Not available / 不可用 | The required `com.intellij.database` plugin is missing / 缺少必需依赖 |

The plugin declares build `232` as its minimum and intentionally has no upper build limit. New IDE releases should still be tested before being marked as verified on JetBrains Marketplace.

插件最低版本为 build `232`，不设置最高版本限制。新的 IDE 大版本仍应在 JetBrains Marketplace 标记为已验证前进行实际测试。

## Installation / 安装

### JetBrains Marketplace

After Marketplace approval, open **Settings > Plugins > Marketplace**, search for **SQL Heading Folding**, and select **Install**.

Marketplace 审核通过后，打开 **Settings > Plugins > Marketplace**，搜索 **SQL Heading Folding** 并安装。

### Install from disk / 从磁盘安装

Download the latest ZIP from the [continuous release](https://github.com/yc-2018/intellij-sql-heading-folding/releases/tag/continuous), then open **Settings > Plugins > Install Plugin from Disk**.

从 [continuous release](https://github.com/yc-2018/intellij-sql-heading-folding/releases/tag/continuous) 下载最新 ZIP，再通过 **Settings > Plugins > Install Plugin from Disk** 安装。

## Build / 构建

Building from source requires JDK 17. Installing the plugin does not require a separate JDK because JetBrains IDEs use their bundled runtime.

源码构建需要 JDK 17。普通用户安装插件不需要额外安装 JDK，插件使用 JetBrains IDE 自带运行时。

```powershell
.\gradlew.bat test buildPlugin
```

The plugin ZIP is generated under `build/distributions/`.

插件 ZIP 生成在 `build/distributions/`。

## Development / 开发

- Kotlin 1.9 and JVM 17
- IntelliJ Platform Gradle Plugin 2.x
- Minimum platform build: `232`
- Plugin ID: `com.github.cgl.sql-heading-folding`
- Required runtime plugin: `com.intellij.database` (Marketplace uses this dependency to determine compatible JetBrains products)

Run an isolated IDE sandbox with:

使用独立 IDE 沙盒运行：

```powershell
.\gradlew.bat runIde
```

## Release / 发布

Every push to `main` runs tests, builds the plugin, uploads an Actions artifact, and refreshes the `continuous` prerelease. Pushing a `v*` tag publishes the plugin to JetBrains Marketplace and creates a versioned GitHub Release. Before the first tagged release, add the Marketplace permanent token as the `JETBRAINS_MARKETPLACE_TOKEN` Actions secret under `Settings > Secrets and variables > Actions`.

每次推送到 `main` 都会运行测试、构建插件并刷新 `continuous` 预发行版；推送 `v*` 标签会将插件发布到 JetBrains Marketplace，并创建正式 GitHub Release。首次发布前，请在 GitHub 仓库的 `Settings > Secrets and variables > Actions` 中，将 Marketplace 永久 Token 添加为名为 `JETBRAINS_MARKETPLACE_TOKEN` 的 Actions Secret。

```powershell
git tag v1.0.0
git push origin v1.0.0
```

## Source / 源码

[github.com/yc-2018/intellij-sql-heading-folding](https://github.com/yc-2018/intellij-sql-heading-folding)
