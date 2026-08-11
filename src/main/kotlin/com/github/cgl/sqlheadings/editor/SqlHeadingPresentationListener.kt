package com.github.cgl.sqlheadings.editor

import com.github.cgl.sqlheadings.model.SqlCommentParser
import com.github.cgl.sqlheadings.model.SqlEmphasisColor
import com.github.cgl.sqlheadings.model.SqlEmphasisComment
import com.github.cgl.sqlheadings.model.SqlHeading
import com.github.cgl.sqlheadings.model.SqlHeadingParser
import com.github.cgl.sqlheadings.toolwindow.SqlLanguageSupport
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
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
import com.intellij.ui.JBColor
import com.intellij.util.Alarm
import java.awt.Color
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
    private val presentationHighlighters = mutableListOf<RangeHighlighter>()
    private var needsFoldingUpdate = true
    private var lastActiveLine: Int? = null

    init {
        editor.caretModel.addCaretListener(this, this)
        editor.document.addDocumentListener(this, this)
        scheduleRefresh(350)
    }

    override fun dispose() {
        alarm.cancelAllRequests()
        clearPresentationHighlighters()
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
            clearPresentationHighlighters()
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
                        region.endOffset == candidate.labelFoldEndOffset
                } ?: return@forEach
                region.isExpanded = editor.document.getLineNumber(heading.offset) == activeLine
            }
        }

        clearPresentationHighlighters()
        val attributes = TextAttributes().apply { fontType = Font.BOLD }
        headings.filter { heading ->
            editor.document.getLineNumber(heading.offset) != activeLine &&
                heading.titleStartOffset < heading.titleEndOffset
        }.forEach { heading ->
            presentationHighlighters += editor.markupModel.addRangeHighlighter(
                heading.titleStartOffset,
                heading.titleEndOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                attributes,
                HighlighterTargetArea.EXACT_RANGE,
            )
        }

        SqlCommentParser.parseEmphasisComments(editor.document.charsSequence).forEach { comment ->
            presentationHighlighters += editor.markupModel.addRangeHighlighter(
                comment.markerStartOffset,
                comment.lineEndOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                emphasisAttributes(comment),
                HighlighterTargetArea.EXACT_RANGE,
            )
        }
    }

    private fun emphasisAttributes(comment: SqlEmphasisComment): TextAttributes {
        val attributes = editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT)
            ?.clone()
            ?: TextAttributes()
        if (comment.bold) attributes.fontType = attributes.fontType or Font.BOLD
        attributes.foregroundColor = when (comment.color) {
            SqlEmphasisColor.RED -> JBColor(Color(0xB3261E), Color(0xFF7B72))
            SqlEmphasisColor.YELLOW -> JBColor(Color(0x8A6100), Color(0xF2C94C))
            SqlEmphasisColor.BLUE -> JBColor(Color(0x075DB7), Color(0x75B7FF))
            SqlEmphasisColor.GREEN -> JBColor(Color(0x17753A), Color(0x70C989))
            SqlEmphasisColor.CYAN -> JBColor(Color(0x007C91), Color(0x56D4DD))
            SqlEmphasisColor.ORANGE -> JBColor(Color(0xA64B00), Color(0xFF9B5E))
            SqlEmphasisColor.PURPLE -> JBColor(Color(0x6F42C1), Color(0xC59CFF))
            SqlEmphasisColor.MAGENTA -> JBColor(Color(0xA12C78), Color(0xFF79C6))
            null -> attributes.foregroundColor
        }
        return attributes
    }

    private fun readSqlHeadings(): List<SqlHeading>? = ReadAction.compute<List<SqlHeading>?, RuntimeException> {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        if (!SqlLanguageSupport.isSql(psiFile?.language)) return@compute null
        SqlHeadingParser.parse(editor.document.charsSequence)
    }

    private fun clearPresentationHighlighters() {
        presentationHighlighters.toList().forEach { highlighter ->
            if (highlighter.isValid) editor.markupModel.removeHighlighter(highlighter)
        }
        presentationHighlighters.clear()
    }

}
