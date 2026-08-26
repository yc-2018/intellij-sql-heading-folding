package com.github.cgl.sqlheadings.toolwindow

import com.github.cgl.sqlheadings.editor.SqlHeadingStyleSettings
import com.github.cgl.sqlheadings.model.SqlCommentParser
import com.github.cgl.sqlheadings.model.SqlEmphasisComment
import com.github.cgl.sqlheadings.model.SqlHeading
import com.github.cgl.sqlheadings.model.SqlHeadingParser
import com.intellij.icons.AllIcons
import com.intellij.database.console.JdbcConsole
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.BorderFactory
import java.util.Locale
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

internal class SqlHeadingsPanel(
    private val project: Project,
) : SimpleToolWindowPanel(true, true), Disposable {
    private val tree = Tree()
    private val fileLabel = JBLabel()
    private val showColoredNodes = JBCheckBox(SqlHeadingsText.coloredNodes, false)
    private val refreshAlarm = Alarm(this)
    private var activeDocument: Document? = null
    private var headings: List<SqlHeading> = emptyList()

    init {
        configureTree()
        setToolbar(createToolbar())

        val body = JPanel(BorderLayout())
        fileLabel.border = BorderFactory.createEmptyBorder(0, 6, 0, 6)
        body.add(fileLabel, BorderLayout.NORTH)
        body.add(ScrollPaneFactory.createScrollPane(tree, true), BorderLayout.CENTER)
        setContent(body)

        showColoredNodes.addActionListener {
            refresh()
            expandAllTreeRows()
        }

        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = scheduleRefresh()
                override fun fileOpened(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) =
                    scheduleRefresh()
            },
        )

        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    if (event.document == activeDocument) scheduleRefresh()
                }
            },
            this,
        )

        scheduleRefresh(0)
    }

    override fun dispose() = Unit

    private fun configureTree() {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        tree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean,
            ) {
                when (val item = (value as? DefaultMutableTreeNode)?.userObject) {
                    is SqlHeading -> {
                        append(item.title.ifBlank { SqlHeadingsText.untitledHeading })
                        append("  H${item.level}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                    is ColoredCommentNode -> {
                        val style = if (item.comment.bold) {
                            SimpleTextAttributes.STYLE_BOLD
                        } else {
                            SimpleTextAttributes.STYLE_PLAIN
                        }
                        append(item.label, SimpleTextAttributes(style, item.color))
                    }
                }
            }
        }

        TreeSpeedSearch.installOn(tree)

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.button != MouseEvent.BUTTON1) return
                val path = tree.getPathForLocation(event.x, event.y) ?: return
                navigateTo((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject)
            }
        })

        tree.registerKeyboardAction(
            { navigateTo(selectedItem()) },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED,
        )
    }

    private fun createToolbar(): JComponent {
        val actions = DefaultActionGroup(
            object : AnAction(
                SqlHeadingsText.refresh,
                SqlHeadingsText.refreshDescription,
                AllIcons.Actions.Refresh,
            ) {
                override fun actionPerformed(event: AnActionEvent) = refresh()
            },
            object : AnAction(
                SqlHeadingsText.collapseAll,
                SqlHeadingsText.collapseAllDescription,
                AllIcons.Actions.Collapseall,
            ) {
                override fun actionPerformed(event: AnActionEvent) = setAllSectionsExpanded(false)
            },
            object : AnAction(
                SqlHeadingsText.expandAll,
                SqlHeadingsText.expandAllDescription,
                AllIcons.Actions.Expandall,
            ) {
                override fun actionPerformed(event: AnActionEvent) = setAllSectionsExpanded(true)
            },
            Separator.getInstance(),
            DefaultActionGroup(
                SqlHeadingsText.transfer,
                SqlHeadingsText.transfer,
                AllIcons.Actions.Upload,
            ).apply {
                isPopup = true
                add(importLocalAction())
                add(importConsoleAction())
                add(Separator.getInstance())
                add(exportLocalAction())
                add(exportConsoleAction())
            },
            object : AnAction("帮助", "查看功能使用说明", AllIcons.Actions.Help) {
                override fun actionPerformed(event: AnActionEvent) {
                    SqlHeadingHelpDialog(project).show()
                }
            },
            object : AnAction("配置样式", "配置标题和注释颜色", AllIcons.General.Settings) {
                override fun actionPerformed(event: AnActionEvent) {
                    SqlHeadingStyleDialog(project).show()
                    refresh()
                }
            },
        )
        val actionToolbar = ActionManager.getInstance()
            .createActionToolbar("SqlHeadingsToolbar", actions, true)
            .apply { targetComponent = this@SqlHeadingsPanel }
            .component
        return JPanel(BorderLayout()).apply {
            add(actionToolbar, BorderLayout.CENTER)
            add(JPanel(BorderLayout()).apply {
                border = javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 6)
                add(showColoredNodes.apply {
                    isOpaque = false
                    toolTipText = SqlHeadingsText.coloredNodesDescription
                }, BorderLayout.CENTER)
            }, BorderLayout.EAST)
        }
    }

    private fun scheduleRefresh(delay: Int = 150) {
        refreshAlarm.cancelAllRequests()
        refreshAlarm.addRequest(::refresh, delay)
    }

    private fun refresh() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val preserveExpansion = activeDocument === editor?.document
        activeDocument = editor?.document

        if (editor == null || !isSqlEditor(editor)) {
            headings = emptyList()
            fileLabel.text = ""
            setTreeModel(emptyList(), preserveExpansion = false)
            tree.emptyText.text = SqlHeadingsText.noSqlEditor
            return
        }

        headings = SqlHeadingParser.parse(editor.document.charsSequence)
        fileLabel.text = FileDocumentManager.getInstance().getFile(editor.document)?.presentableName.orEmpty()
        val coloredComments = if (showColoredNodes.isSelected && SqlHeadingStyleSettings.getInstance().isEmphasisEnabled()) {
            val settings = SqlHeadingStyleSettings.getInstance()
            SqlCommentParser.parseEmphasisComments(editor.document.charsSequence).mapNotNull { comment ->
                val marker = comment.colorMarker ?: return@mapNotNull null
                settings.emphasisColor(marker)?.let { color -> ColoredCommentNode(comment, color) }
            }
        } else {
            emptyList()
        }
        setTreeModel(headings, coloredComments, preserveExpansion)
        tree.emptyText.text = SqlHeadingsText.noHeadings
    }

    private fun isSqlEditor(editor: Editor): Boolean = ApplicationManager.getApplication().runReadAction(Computable {
        val psiFile = com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        SqlLanguageSupport.isSql(psiFile?.language)
    })

    private fun setTreeModel(
        headingItems: List<SqlHeading>,
        coloredComments: List<ColoredCommentNode> = emptyList(),
        preserveExpansion: Boolean = true,
    ) {
        val expandedPaths = if (preserveExpansion) captureExpandedIndexPaths() else emptySet()
        val root = DefaultMutableTreeNode()
        val parents = ArrayDeque<Pair<Int, DefaultMutableTreeNode>>()

        (headingItems + coloredComments).sortedBy(::itemOffset).forEach { item ->
            when (item) {
                is SqlHeading -> {
                    while (parents.isNotEmpty() && parents.last().first >= item.level) {
                        parents.removeLast()
                    }

                    val node = DefaultMutableTreeNode(item)
                    (parents.lastOrNull()?.second ?: root).add(node)
                    parents.addLast(item.level to node)
                }
                is ColoredCommentNode -> (parents.lastOrNull()?.second ?: root).add(DefaultMutableTreeNode(item))
            }
        }

        tree.model = DefaultTreeModel(root)
        if (preserveExpansion) {
            restoreExpandedIndexPaths(expandedPaths)
        } else {
            expandAllTreeRows()
        }
    }

    private fun importLocalAction() = object : AnAction(
        SqlHeadingsText.importLocal,
        SqlHeadingsText.importLocalDescription,
        AllIcons.Actions.Download,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            val editor = currentSqlEditor() ?: return
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle(SqlHeadingsText.importLocal)
                .withDescription(SqlHeadingsText.value("选择要插入当前 SQL 编辑器的 .sql 文件", "Select a .sql file to insert into the current editor"))
                .withFileFilter { file -> !file.isDirectory && file.extension?.lowercase(Locale.ROOT) == "sql" }
            val file = FileChooser.chooseFile(descriptor, project, null) ?: return
            runCatching { VfsUtil.loadText(file) }
                .onSuccess { insertAtCaret(editor, it, SqlHeadingsText.importLocal) }
                .onFailure { showTransferError(SqlHeadingsText.value("读取 SQL 文件失败", "Failed to read the SQL file"), it) }
        }
    }

    private fun importConsoleAction() = object : AnAction(
        SqlHeadingsText.importConsole,
        SqlHeadingsText.importConsoleDescription,
        AllIcons.Actions.Download,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            val targetEditor = currentSqlEditor() ?: return
            val source = chooseOtherConsole(targetEditor, selectingSource = true) ?: return
            insertAtCaret(targetEditor, source.document.text, SqlHeadingsText.importConsole)
        }
    }

    private fun exportLocalAction() = object : AnAction(
        SqlHeadingsText.exportLocal,
        SqlHeadingsText.exportLocalDescription,
        AllIcons.Actions.Upload,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            val editor = currentSqlEditor() ?: return
            val currentFile = FileDocumentManager.getInstance().getFile(editor.document)
            val suggestedName = defaultExportName(editor, currentFile)
            val descriptor = createFileSaverDescriptor(
                SqlHeadingsText.exportLocal,
                SqlHeadingsText.value("选择保存 SQL 文件的位置", "Choose where to save the SQL file"),
            )
            val wrapper = FileChooserFactory.getInstance()
                .createSaveFileDialog(descriptor, project)
                .save(null as VirtualFile?, suggestedName) ?: return
            val savedFile = wrapper.getVirtualFile(true) ?: return
            runCatching {
                ApplicationManager.getApplication().runWriteAction {
                    VfsUtil.saveText(savedFile, editor.document.text)
                }
            }
                .onFailure { showTransferError(SqlHeadingsText.value("保存 SQL 文件失败", "Failed to save the SQL file"), it) }
        }
    }

    private fun exportConsoleAction() = object : AnAction(
        SqlHeadingsText.exportConsole,
        SqlHeadingsText.exportConsoleDescription,
        AllIcons.Actions.Upload,
    ) {
        override fun actionPerformed(event: AnActionEvent) {
            val sourceEditor = currentSqlEditor() ?: return
            val target = chooseOtherConsole(sourceEditor, selectingSource = false) ?: return
            val separator = if (target.document.text.isBlank() || target.document.text.endsWith("\n")) "" else "\n\n"
            WriteCommandAction.runWriteCommandAction(project, SqlHeadingsText.exportConsole, null, Runnable {
                target.document.insertString(target.document.textLength, separator + sourceEditor.document.text)
            })
            FileEditorManager.getInstance(project).openFile(target.virtualFile, true)
        }
    }

    private fun currentSqlEditor(): Editor? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        if (!isSqlEditor(editor)) {
            Messages.showWarningDialog(project, SqlHeadingsText.noSqlEditor, SqlHeadingsText.toolWindowTitle)
            return null
        }
        return editor
    }

    private fun chooseOtherConsole(editor: Editor, selectingSource: Boolean): JdbcConsole? {
        val currentConsole = JdbcConsole.getActiveConsoles(project)
            .firstOrNull { it.document === editor.document }
        val currentDbms = currentConsole?.dataSource?.dbms
        val candidates = JdbcConsole.getActiveConsoles(project)
            .filter { it.document !== editor.document }
            .filter { console -> currentDbms == null || console.dataSource.dbms == currentDbms }
            .distinctBy { it.document }
        if (candidates.isEmpty()) {
            val message = if (currentDbms == null) {
                SqlHeadingsText.value(
                    "没有找到其他已打开的 SQL 控制台。请先打开目标数据源的 SQL 控制台。",
                    "No other open SQL console was found. Open a SQL console for the source or target data source first.",
                )
            } else {
                SqlHeadingsText.value(
                    "没有找到其他同数据库类型的已打开 SQL 控制台。请先打开目标数据源的 SQL 控制台。",
                    "No other open SQL console of the same database type was found. Open the target console first.",
                )
            }
            Messages.showInfoMessage(project, message, SqlHeadingsText.transfer)
            return null
        }
        val labels = candidates.map { console ->
            val sourceName = console.dataSource.name.takeIf { it.isNotBlank() }
                ?: SqlHeadingsText.value("未命名数据源", "Unnamed data source")
            "$sourceName — ${console.title}"
        }
        val chooser = SqlConsoleChooserDialog(
            project,
            if (selectingSource) {
                SqlHeadingsText.value("选择导入来源", "Choose Import Source")
            } else {
                SqlHeadingsText.value("选择导出目标", "Choose Export Target")
            },
            if (selectingSource) {
                SqlHeadingsText.value(
                    "选择要从中读取 SQL 的控制台（只复制文本，不会执行 SQL）",
                    "Choose the console to read from. SQL will be copied but never executed.",
                )
            } else {
                SqlHeadingsText.value(
                    "选择要写入 SQL 的控制台（只复制文本，不会执行 SQL）",
                    "Choose the console to write to. SQL will be copied but never executed.",
                )
            },
            labels,
        )
        if (!chooser.showAndGet()) return null
        return candidates.getOrNull(chooser.selectedIndex)
    }

    private fun insertAtCaret(editor: Editor, text: String, commandName: String) {
        if (text.isEmpty()) return
        val caretOffset = editor.caretModel.offset.coerceIn(0, editor.document.textLength)
        val prefix = if (caretOffset > 0 && editor.document.text[caretOffset - 1] != '\n') "\n\n" else ""
        WriteCommandAction.runWriteCommandAction(project, commandName, null, Runnable {
            editor.document.insertString(caretOffset, prefix + text)
        })
    }

    private fun defaultExportName(editor: Editor, file: VirtualFile?): String {
        val console = JdbcConsole.getActiveConsoles(project).firstOrNull { it.document === editor.document }
        val dataSourceName = console?.dataSource?.name?.takeIf { it.isNotBlank() }
            ?: SqlHeadingsText.value("未命名数据源", "Unnamed data source")
        val sqlName = file?.nameWithoutExtension?.takeIf { it.isNotBlank() } ?: "sql"
        return "${sanitizeFileName(dataSourceName)}-${sanitizeFileName(sqlName)}.sql"
    }

    private fun createFileSaverDescriptor(title: String, description: String): FileSaverDescriptor {
        val descriptorClass = FileSaverDescriptor::class.java
        return runCatching {
            descriptorClass
                .getConstructor(String::class.java, String::class.java)
                .newInstance(title, description)
        }.getOrElse {
            descriptorClass
                .getConstructor(String::class.java, String::class.java, Array<String>::class.java)
                .newInstance(title, description, emptyArray<String>())
        }
    }

    private fun sanitizeFileName(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .ifBlank { "sql" }

    private fun showTransferError(message: String, error: Throwable) {
        Messages.showErrorDialog(
            project,
            "$message: ${error.message.orEmpty()}",
            SqlHeadingsText.value("导入/导出失败", "Import / Export Failed"),
        )
    }

    private fun captureExpandedIndexPaths(): Set<List<Int>> {
        val root = tree.model.root as? TreeNode ?: return emptySet()
        val expanded = tree.getExpandedDescendants(TreePath(root)) ?: return emptySet()
        return buildSet {
            while (expanded.hasMoreElements()) {
                val components = expanded.nextElement().path
                var parent = root
                val indexes = mutableListOf<Int>()
                for (component in components.drop(1)) {
                    val child = component as? TreeNode ?: break
                    val index = parent.getIndex(child)
                    if (index < 0) break
                    indexes += index
                    parent = child
                }
                if (indexes.isNotEmpty()) add(indexes)
            }
        }
    }

    private fun restoreExpandedIndexPaths(paths: Set<List<Int>>) {
        val root = tree.model.root as? TreeNode ?: return
        tree.expandPath(TreePath(root))
        paths.sortedBy(List<Int>::size).forEach { indexes ->
            var node = root
            val components = mutableListOf<Any>(root)
            for (index in indexes) {
                if (index !in 0 until node.childCount) break
                node = node.getChildAt(index)
                components += node
            }
            if (components.size == indexes.size + 1) {
                tree.expandPath(TreePath(components.toTypedArray()))
            }
        }
    }

    private fun expandAllTreeRows() {
        var row = 0
        while (row < tree.rowCount) {
            tree.expandRow(row)
            row++
        }
    }

    private fun itemOffset(item: Any): Int = when (item) {
        is SqlHeading -> item.offset
        is ColoredCommentNode -> item.comment.markerStartOffset
        else -> Int.MAX_VALUE
    }

    private fun selectedItem(): Any? =
        (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject

    private fun navigateTo(item: Any?) {
        val offset = when (item) {
            is SqlHeading -> item.offset
            is ColoredCommentNode -> item.comment.markerStartOffset
            else -> return
        }
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        if (editor.document != activeDocument) return

        editor.foldingModel.runBatchFoldingOperation {
            editor.foldingModel.allFoldRegions
                .filter { region ->
                    offset in region.startOffset until region.endOffset ||
                        item is SqlHeading && region.startOffset == item.foldStartOffset
                }
                .forEach { region -> region.isExpanded = true }
        }
        editor.caretModel.moveToOffset(offset.coerceAtMost(editor.document.textLength))
        editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        editor.contentComponent.requestFocusInWindow()
    }

    private fun setAllSectionsExpanded(expanded: Boolean) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        if (editor.document != activeDocument) return
        val sectionStarts = headings.mapTo(hashSetOf()) { it.foldStartOffset }

        editor.foldingModel.runBatchFoldingOperation {
            editor.foldingModel.allFoldRegions
                .filter { region -> region.startOffset in sectionStarts }
                .forEach { region -> region.isExpanded = expanded }
        }
    }
}

private data class ColoredCommentNode(
    val comment: SqlEmphasisComment,
    val color: Color,
) {
    val label: String
        get() = comment.text.ifBlank {
            comment.colorMarker?.let { marker -> "@$marker" }.orEmpty()
        }
}
