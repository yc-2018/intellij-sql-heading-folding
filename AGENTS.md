# 仓库指南

## 范围

本仓库包含一个 IntelliJ Platform 插件，为 SQL 控制台和 SQL 文件提供层级标题、折叠、阅读模式标签和大纲导航功能。

主要验证目标为 IntelliJ IDEA Ultimate 和 DataGrip。插件应兼容所有提供 `com.intellij.database` 的 JetBrains IDE；Marketplace 根据此依赖确定适用产品。不得引入依赖 Java 专用 IDEA 模块的依赖，也不要无必要地缩小产品兼容范围。

## 工具链

- Gradle 和编译使用 JDK 17。
- 使用仓库中已提交的 Gradle Wrapper，不要求全局安装 Gradle。
- JVM 字节码版本保持为 17。
- 最低 IntelliJ Platform build 为 `232`（2023.2）。
- 除非已验证的不兼容问题需要临时限制，否则不设置平台最高 build 限制。
- 不要添加开发者机器的 JDK 路径或其他绝对本地路径。

## 架构

- `model/`：标题数据和纯文本解析器。
- `folding/`：用于标题标签和 SQL 区段的 IntelliJ 折叠描述符。
- `editor/`：感知光标位置的阅读模式展示。
- `toolwindow/`：标题大纲、导航和批量折叠控制。
- `src/main/resources/META-INF/plugin.xml`：插件元数据、依赖和扩展注册。

尽可能保持解析逻辑独立于 IntelliJ API。UI 和编辑器集成可以使用解析结果，但不得重复实现标题语法规则。

## 标题约定

- 仅识别形如 `-- # 标题` 至 `-- ##### 标题` 的完整行 SQL 注释。
- 保留文档原始文本；阅读模式只能进行视觉展示。
- 一个区段在遇到下一个同级或更高级标题时结束。
- 区段折叠标记保留在标题行。
- 当光标不在标题行时，显示紧凑的 `H1` 至 `H5` 标签和加粗标题。
- 当光标进入标题行时，恢复原始注释展示。
- 阅读模式标签的背景不得包含缩进或填充空格。

## 编码风格

- 遵循现有 Kotlin 风格和 IntelliJ Platform API。
- 优先使用小而聚焦的类和纯解析函数。
- 注释保持简洁，仅解释不明显的行为。
- 保持主题兼容性，避免硬编码编辑器前景色或背景色。
- 正确释放监听器、Alarm、高亮器和编辑器资源。
- 当按行更新已足够时，避免在每次光标偏移变化时执行工作。

## 验证

提交行为变更前运行：

```powershell
.\gradlew.bat test buildPlugin
```

解析器行为变更需要在 `SqlHeadingParserTest` 中添加有针对性的单元测试。

兼容性变更时，应在 IntelliJ IDEA Ultimate 和 DataGrip 中测试打包 ZIP；条件允许时，再抽样测试其他内置 Database Tools 的 JetBrains IDE。在发布到 Marketplace 前，使用 JetBrains Plugin Verifier 验证有代表性的 IDE 版本。

Community 测试沙箱不包含 `com.intellij.database`；在生成 searchable options 时出现缺少插件的警告属于预期现象，不能误判为编译或单元测试失败。

## 文档与发布

- README 内容保持中英文双语。
- 面向用户的发布需要更新 Marketplace 元数据和变更说明。
- 每次可分发的行为变更都要提升插件版本。
- 每次影响插件界面或行为的改动完成后，都必须重新执行 `buildPlugin` 并提供最新 ZIP，供用户安装测试。
- 推送到 `main` 会刷新 `continuous` 预发行版。
- 符合 `v*` 的版本标签会创建不可变的 GitHub Release。
- 不得提交 `.gradle/`、`.intellijPlatform/`、`.idea/`、`build/` 或本地凭据。
