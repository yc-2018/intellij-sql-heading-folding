package com.github.cgl.sqlheadings.toolwindow

import com.github.cgl.sqlheadings.model.SqlHeading
import com.github.cgl.sqlheadings.model.SqlHeadingParser
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ReadAction
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.Alarm
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

internal class SqlHeadingsPanel(
    private val project: Project,
) : SimpleToolWindowPanel(true, true), Disposable {
    private val tree = Tree()
    private val fileLabel = JBLabel()
    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var activeDocument: Document? = null
    private var headings: List<SqlHeading> = emptyList()

    init {
        configureTree()
        setToolbar(createToolbar())

        val body = JPanel(BorderLayout())
        body.add(fileLabel, BorderLayout.NORTH)
        body.add(ScrollPaneFactory.createScrollPane(tree, true), BorderLayout.CENTER)
        setContent(body)

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
                val heading = (value as? DefaultMutableTreeNode)?.userObject as? SqlHeading ?: return
                append(heading.title.ifBlank { "(Untitled heading)" })
                append("  H${heading.level}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            }
        }

        TreeSpeedSearch.installOn(tree)

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.button != MouseEvent.BUTTON1) return
                val path = tree.getPathForLocation(event.x, event.y) ?: return
                navigateTo((path.lastPathComponent as? DefaultMutableTreeNode)?.userObject as? SqlHeading)
            }
        })

        tree.registerKeyboardAction(
            { navigateTo(selectedHeading()) },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED,
        )
    }

    private fun createToolbar(): JComponent {
        val actions = DefaultActionGroup(
            object : AnAction("Refresh", "Refresh SQL headings", AllIcons.Actions.Refresh) {
                override fun actionPerformed(event: AnActionEvent) = refresh()
            },
            object : AnAction("Collapse All", "Collapse all SQL heading sections", AllIcons.Actions.Collapseall) {
                override fun actionPerformed(event: AnActionEvent) = setAllSectionsExpanded(false)
            },
            object : AnAction("Expand All", "Expand all SQL heading sections", AllIcons.Actions.Expandall) {
                override fun actionPerformed(event: AnActionEvent) = setAllSectionsExpanded(true)
            },
        )
        return ActionManager.getInstance()
            .createActionToolbar("SqlHeadingsToolbar", actions, true)
            .apply { targetComponent = this@SqlHeadingsPanel }
            .component
    }

    private fun scheduleRefresh(delay: Int = 150) {
        refreshAlarm.cancelAllRequests()
        refreshAlarm.addRequest(::refresh, delay)
    }

    private fun refresh() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        activeDocument = editor?.document

        if (editor == null || !isSqlEditor(editor)) {
            headings = emptyList()
            fileLabel.text = ""
            setTreeModel(emptyList())
            tree.emptyText.text = "Open a SQL console or SQL file"
            return
        }

        headings = SqlHeadingParser.parse(editor.document.charsSequence)
        fileLabel.text = FileDocumentManager.getInstance().getFile(editor.document)?.presentableName.orEmpty()
        setTreeModel(headings)
        tree.emptyText.text = "No headings. Add -- # Heading"
    }

    private fun isSqlEditor(editor: Editor): Boolean = ReadAction.compute<Boolean, RuntimeException> {
        val psiFile = com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        SqlLanguageSupport.isSql(psiFile?.language)
    }

    private fun setTreeModel(items: List<SqlHeading>) {
        val root = DefaultMutableTreeNode()
        val parents = ArrayDeque<Pair<Int, DefaultMutableTreeNode>>()

        items.forEach { heading ->
            while (parents.isNotEmpty() && parents.last().first >= heading.level) {
                parents.removeLast()
            }

            val node = DefaultMutableTreeNode(heading)
            (parents.lastOrNull()?.second ?: root).add(node)
            parents.addLast(heading.level to node)
        }

        tree.model = DefaultTreeModel(root)
        repeat(tree.rowCount) { row -> tree.expandRow(row) }
    }

    private fun selectedHeading(): SqlHeading? =
        (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? SqlHeading

    private fun navigateTo(heading: SqlHeading?) {
        heading ?: return
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        if (editor.document != activeDocument) return

        editor.foldingModel.runBatchFoldingOperation {
            editor.foldingModel.allFoldRegions
                .filter { region ->
                    heading.offset in region.startOffset until region.endOffset ||
                        region.startOffset == heading.foldStartOffset
                }
                .forEach { region -> region.isExpanded = true }
        }
        editor.caretModel.moveToOffset(heading.offset.coerceAtMost(editor.document.textLength))
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
