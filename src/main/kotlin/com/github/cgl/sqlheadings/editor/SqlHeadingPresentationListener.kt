package com.github.cgl.sqlheadings.editor

import com.github.cgl.sqlheadings.model.SqlHeading
import com.github.cgl.sqlheadings.model.SqlHeadingParser
import com.github.cgl.sqlheadings.toolwindow.SqlLanguageSupport
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.Alarm
import java.awt.Font
import java.util.IdentityHashMap

internal class SqlHeadingPresentationListener : EditorFactoryListener, DumbAware {
    private val controllers = IdentityHashMap<Editor, SqlHeadingPresentationController>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        controllers[editor] = SqlHeadingPresentationController(project, editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        controllers.remove(event.editor)?.dispose()
    }
}

private class SqlHeadingPresentationController(
    private val project: Project,
    private val editor: Editor,
) : Disposable, CaretListener, DocumentListener {
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val titleHighlighters = mutableListOf<RangeHighlighter>()
    private val levelInlays = mutableListOf<Inlay<*>>()
    private var needsFoldingUpdate = true
    private var lastActiveLine: Int? = null

    init {
        editor.caretModel.addCaretListener(this, this)
        editor.document.addDocumentListener(this, this)
        scheduleRefresh(350)
    }

    override fun dispose() {
        alarm.cancelAllRequests()
        clearPresentation()
    }

    override fun caretPositionChanged(event: CaretEvent) {
        val activeLine = editor.document.getLineNumber(editor.caretModel.offset)
        if (activeLine != lastActiveLine) scheduleRefresh(0, false)
    }

    override fun documentChanged(event: DocumentEvent) {
        needsFoldingUpdate = true
        scheduleRefresh(150, true)
    }

    private fun scheduleRefresh(delay: Int, updateFolding: Boolean = needsFoldingUpdate) {
        needsFoldingUpdate = needsFoldingUpdate || updateFolding
        alarm.cancelAllRequests()
        alarm.addRequest(::refresh, delay)
    }

    private fun refresh() {
        if (editor.isDisposed) return

        val headings = readSqlHeadings() ?: run {
            clearPresentation()
            lastActiveLine = null
            return
        }

        if (needsFoldingUpdate) {
            CodeFoldingManager.getInstance(project).updateFoldRegions(editor)
            needsFoldingUpdate = false
        }

        val activeLine = editor.document.getLineNumber(editor.caretModel.offset)
        lastActiveLine = activeLine
        editor.foldingModel.runBatchFoldingOperation {
            editor.foldingModel.allFoldRegions.forEach { region ->
                val heading = headings.firstOrNull { candidate ->
                    region.startOffset == candidate.markerStartOffset &&
                        region.endOffset == candidate.titleStartOffset
                } ?: return@forEach
                region.isExpanded = editor.document.getLineNumber(heading.offset) == activeLine
            }
        }

        clearPresentation()
        val attributes = TextAttributes().apply { fontType = Font.BOLD }
        headings.filter { heading ->
            editor.document.getLineNumber(heading.offset) != activeLine &&
                heading.titleStartOffset < heading.titleEndOffset
        }.forEach { heading ->
            editor.inlayModel.addInlineElement(
                heading.titleStartOffset,
                false,
                SqlHeadingLevelRenderer(heading.level),
            )?.let(levelInlays::add)

            titleHighlighters += editor.markupModel.addRangeHighlighter(
                heading.titleStartOffset,
                heading.titleEndOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                attributes,
                HighlighterTargetArea.EXACT_RANGE,
            )
        }
    }

    private fun readSqlHeadings(): List<SqlHeading>? = ReadAction.compute<List<SqlHeading>?, RuntimeException> {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        if (!SqlLanguageSupport.isSql(psiFile?.language)) return@compute null
        SqlHeadingParser.parse(editor.document.charsSequence)
    }

    private fun clearTitleHighlighters() {
        titleHighlighters.toList().forEach { highlighter ->
            if (highlighter.isValid) editor.markupModel.removeHighlighter(highlighter)
        }
        titleHighlighters.clear()
    }

    private fun clearPresentation() {
        clearTitleHighlighters()
        levelInlays.toList().forEach { inlay ->
            if (inlay.isValid) inlay.dispose()
        }
        levelInlays.clear()
    }
}
