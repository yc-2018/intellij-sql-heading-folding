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
        get() = value("全部折叠", "Collapse All")

    val collapseAllDescription: String
        get() = value("折叠所有 SQL 标题区块", "Collapse all SQL heading sections")

    val expandAll: String
        get() = value("全部展开", "Expand All")

    val expandAllDescription: String
        get() = value("展开所有 SQL 标题区块", "Expand all SQL heading sections")

    val untitledHeading: String
        get() = value("（未命名标题）", "(Untitled heading)")

    val noSqlEditor: String
        get() = value("请打开 SQL 控制台或 SQL 文件", "Open a SQL console or SQL file")

    val noHeadings: String
        get() = value("暂无标题，请添加 -- # 标题", "No headings. Add -- # Heading")
}
