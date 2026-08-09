# SQL Heading Folding

IntelliJ IDEA SQL 控制台的分级标题、代码折叠和目录导航插件。

## 用法

在 SQL 控制台或 `.sql` 文件中，用一到五个 `#` 编写标题：

```sql
-- # 用户分析
select * from users;

-- ## 活跃用户
select * from users where active = true;

-- # 订单分析
select * from orders;
```

- 标题行左侧会出现 IDEA 原生折叠控件。
- 光标不在标题行时会隐藏 `-- #` 标记并加粗标题；光标返回该行时恢复完整注释，方便编辑。
- 一级标题折叠到下一个一级标题之前；二级标题折叠到下一个一级或二级标题之前，其余层级同理。
- 打开右侧 **SQL Headings** 工具窗口可查看完整目录。
- 单击目录标题会展开遮挡该标题的区块，并将编辑器定位到标题行。
- 工具窗口顶部可刷新、全部折叠或全部展开标题区块。

## 构建

项目面向 IntelliJ IDEA Ultimate 2023.2-2023.3。安装插件的用户不需要单独安装 JDK，插件运行时使用 IDEA 自带的 JetBrains Runtime。

只有从源码构建插件时才需要 JDK 17。请将 `JAVA_HOME` 指向任意可用的 JDK 17，或者在 IDEA 的 Gradle 设置中将 Gradle JVM 选择为 JDK 17。项目不包含任何开发者电脑上的绝对 JDK 路径。

构建插件：

```powershell
.\gradlew.bat buildPlugin
```

插件 ZIP 会生成在 `build/distributions/`。在 IDEA 中打开 **Settings > Plugins > 齿轮 > Install Plugin from Disk**，选择该 ZIP 安装。

启动独立的 IDEA 测试环境：

```powershell
.\gradlew.bat runIde
```
