package com.github.cgl.sqlheadings.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.Dimension
import javax.swing.JComponent

internal class SqlHeadingHelpDialog(project: Project) : DialogWrapper(project, true) {
    init {
        title = "SQL 标题使用说明"
        init()
    }

    override fun createCenterPanel(): JComponent = JBScrollPane(
        JBTextArea(HELP_TEXT).apply {
            isEditable = false
            isOpaque = false
            lineWrap = true
            wrapStyleWord = true
            border = null
        },
    ).apply {
        preferredSize = Dimension(680, 500)
    }

    private companion object {
        val HELP_TEXT = """
            一、分级标题

            使用 -- # 到 -- ##### 创建一级到五级标题。
            标题区块在遇到下一个同级或更高级标题时结束。
            光标离开标题行后显示 H1 到 H5 标签，回到标题行时恢复原始注释。

            二、强调注释

            可直接从行首输入 @颜色 或 @@颜色，插件会自动补成 SQL 注释的 -- 前缀。
            例如：@r 红色说明 会自动变为 -- @r 红色说明；@@g 会自动变为 -- @@g。
            只有输入颜色字母后面的空格才会转换，单独的 @r、@变量、@@ROWCOUNT 不会改变。

            -- @r  红色
            -- @y  黄色
            -- @b  蓝色
            -- @g  绿色
            -- @c  青色
            -- @o  橙色
            -- @p  紫色
            -- @m  品红
            -- @@   加粗并保留普通注释颜色
            -- @@r  加粗并变成红色，其他颜色也可以同样组合

            a 到 z 都可以作为颜色标记，默认只有上述八个字母设置了颜色，其他字母可在“配置样式”中设置。
            颜色字母不区分大小写。强调注释不会创建折叠区块；勾选侧栏顶部的“有色节点”后，可将已设置颜色的注释加入目录。
            可在“配置样式”中关闭“启用颜色注释”，关闭后不会自动补注释或应用颜色样式。

            三、侧栏操作

            点击标题可以跳转到对应位置并展开该区块。
            “有色节点”默认不勾选，勾选后目录会显示彩色注释，点击同样可以跳转。
            工具栏可以刷新目录、全部折叠或全部展开。
        """.trimIndent()
    }
}
