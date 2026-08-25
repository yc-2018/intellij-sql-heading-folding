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
}
