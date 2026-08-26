package com.github.cgl.sqlheadings.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

internal class SqlConsoleChooserDialog(
    project: Project,
    dialogTitle: String,
    private val prompt: String,
    labels: List<String>,
) : DialogWrapper(project, true) {
    private val consoleList = JBList(labels).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        selectedIndex = 0
    }

    val selectedIndex: Int
        get() = consoleList.selectedIndex

    init {
        title = dialogTitle
        init()
    }

    override fun createCenterPanel(): JComponent = JPanel(BorderLayout(0, 8)).apply {
        preferredSize = Dimension(460, 240)
        add(JBLabel(prompt), BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(consoleList, true), BorderLayout.CENTER)
    }

    override fun getPreferredFocusedComponent(): JComponent = consoleList
}
