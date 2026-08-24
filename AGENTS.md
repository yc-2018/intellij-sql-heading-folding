# 仓库指南

## 范围

本仓库包含一个 IntelliJ Platform 插件，为 SQL 控制台和 SQL 文件提供层级标题、折叠、阅读模式标签和大纲导航功能。

主要验证目标为 IntelliJ IDEA Ultimate 和 DataGrip。插件应兼容所有提供 `com.intellij.database` 的 JetBrains IDE；Marketplace 根据此依赖确定适用产品。不得引入依赖 Java 专用 IDEA 模块的依赖，也不要无必要地缩小产品兼容范围。

## 工具链

- Gradle 和编译使用 JDK 21。
- 使用仓库中已提交的 Gradle Wrapper，不要求全局安装 Gradle。
- JVM 字节码版本保持为 21。
- 最低 IntelliJ Platform build 为 `262`（2026.2）。
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

每次完成影响插件界面或行为的改动后，必须使用 JDK 21 运行：

```powershell
.\gradlew.bat test buildPlugin
```

- 构建成功后，向用户提供 `build/distributions/` 下最新 ZIP 的完整路径和 SHA-256。
- ZIP 文件名中的版本必须与 `build.gradle.kts` 中的插件版本一致。
- 编译输出中出现 deprecated 或 scheduled-for-removal API 警告时，应优先迁移到当前最低平台支持的新 API，不得仅通过压制警告处理。
- 完成用户要求的界面或行为改动，并通过完整测试和打包后，默认直接提交、创建对应版本标签并推送；只有用户明确要求“先别推送”或“只供测试”时才保留在本地。

解析器行为变更需要在 `SqlHeadingParserTest` 中添加有针对性的单元测试。

兼容性变更时，应在 IntelliJ IDEA Ultimate 和 DataGrip 中测试打包 ZIP；条件允许时，再抽样测试其他内置 Database Tools 的 JetBrains IDE。在发布到 Marketplace 前，使用 JetBrains Plugin Verifier 验证有代表性的 IDE 版本。

测试沙箱出现平台插件警告时，需要以 Gradle 最终任务结果、单元测试结果和 ZIP 是否成功生成为准；不得忽略真实的编译或测试失败。

## 文档与发布

- README 内容保持中英文双语。
- 面向用户的发布需要更新 Marketplace 元数据和变更说明。
- 每次可分发的行为变更都要提升插件版本。
- 每次影响插件界面或行为的改动完成后，都必须重新执行 `buildPlugin` 并提供最新 ZIP，供用户安装测试。
- 每次可分发版本通过验证后，先执行 `git diff --check` 并确认变更范围，再提交相关文件；无需等待用户再次要求推送。
- 正式版本标签必须使用 `v{build.gradle.kts 中的版本}`，例如版本 `1.1.2` 对应标签 `v1.1.2`。
- 推送正式版本时，依次推送 `main` 和对应版本标签；推送前确认该标签尚不存在。
- 不得移动、覆盖或重新创建已经发布的版本标签。
- 推送到 `main` 会运行测试、构建 ZIP、上传 Actions 构件并刷新 `continuous` 预发行版。
- 符合 `v*` 的版本标签会再次运行验证、发布到 JetBrains Marketplace，并创建不可变的 GitHub Release。
- GitHub Actions 必须使用 JDK 21 和仓库提交的 Gradle Wrapper，不得依赖全局 Gradle。
- Marketplace Token 只能通过 GitHub Secret `JETBRAINS_MARKETPLACE_TOKEN` 注入。
- 不得提交 `.gradle/`、`.intellijPlatform/`、`.idea/`、`build/` 或本地凭据。
