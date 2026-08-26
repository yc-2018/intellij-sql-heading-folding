package com.github.cgl.sqlheadings.toolwindow

import java.util.Locale

internal object SqlHeadingsText {
    private val chineseLocale = Locale.getDefault().language.startsWith("zh", ignoreCase = true)

    fun value(chinese: String, english: String): String = if (chineseLocale) chinese else english

    val toolWindowTitle: String
        get() = value("SQL 标题", "SQL Headings")

    val refresh: String
        get() = value("刷新", "Refresh")

    val refreshDescription: String
        get() = value("刷新 SQL 标题", "Refresh SQL headings")

    val collapseAll: String
        get() = value("折叠文件区块", "Collapse File Sections")

    val collapseAllDescription: String
        get() = value("折叠当前 SQL 文件中的所有标题区块（编辑器内容）", "Collapse all heading sections in the current SQL file")

    val expandAll: String
        get() = value("展开文件区块", "Expand File Sections")

    val expandAllDescription: String
        get() = value("展开当前 SQL 文件中的所有标题区块（编辑器内容）", "Expand all heading sections in the current SQL file")

    val untitledHeading: String
        get() = value("（未命名标题）", "(Untitled heading)")

    val noSqlEditor: String
        get() = value("请打开 SQL 控制台或 SQL 文件", "Open a SQL console or SQL file")

    val noHeadings: String
        get() = value("暂无标题，请添加 -- # 标题", "No headings. Add -- # Heading")

    val coloredNodes: String
        get() = value("有色节点", "Colored nodes")

    val coloredNodesDescription: String
        get() = value("将带颜色的注释显示在侧边栏目录中", "Show colored comments in the outline")

    val transfer: String
        get() = value("导入/导出", "Import / Export")

    val importLocal: String
        get() = value("导入本地 SQL", "Import SQL from Local File")

    val importLocalDescription: String
        get() = value("将本地 SQL 文件插入当前光标位置", "Insert a local SQL file at the current caret")

    val importConsole: String
        get() = value("从其他数据源导入", "Import from Another Data Source")

    val importConsoleDescription: String
        get() = value("从其他已打开的同类型 SQL 控制台插入文本", "Insert text from another open SQL console of the same database type")

    val exportLocal: String
        get() = value("导出到本地", "Export to Local File")

    val exportLocalDescription: String
        get() = value("将当前 SQL 保存为本地文件", "Save the current SQL to a local file")

    val exportConsole: String
        get() = value("导出到其他数据源", "Export to Another Data Source")

    val exportConsoleDescription: String
        get() = value("将当前 SQL 复制到其他已打开的同类型 SQL 控制台", "Copy the current SQL to another open SQL console of the same database type")
}
