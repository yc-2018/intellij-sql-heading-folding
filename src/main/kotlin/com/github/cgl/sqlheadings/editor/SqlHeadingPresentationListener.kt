package com.github.cgl.sqlheadings.editor

import com.github.cgl.sqlheadings.model.SqlCommentParser
import com.github.cgl.sqlheadings.model.SqlEmphasisComment
import com.github.cgl.sqlheadings.model.SqlHeading
import com.github.cgl.sqlheadings.model.SqlHeadingParser
import com.github.cgl.sqlheadings.toolwindow.SqlLanguageSupport
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.command.WriteCommandAction
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
import com.intellij.util.Alarm
import java.awt.Font
import java.util.IdentityHashMap
import java.util.TreeSet

internal class SqlHeadingPresentationListener : EditorFactoryListener, DumbAware {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        controllers[editor] = SqlHeadingPresentationController(project, editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        controllers.remove(event.editor)?.dispose()
    }

    companion object {
        private val controllers = IdentityHashMap<Editor, SqlHeadingPresentationController>()

        fun refreshAllPresentations() {
            controllers.values.toList().forEach(SqlHeadingPresentationController::refreshAfterStyleChange)
        }
    }
}

private class SqlHeadingPresentationController(
    private val project: Project,
    private val editor: Editor,
) : Disposable, CaretListener, DocumentListener {
    private val alarm = Alarm(this)
    private val presentationHighlighters = mutableListOf<RangeHighlighter>()
    private val shorthandCandidateLines = TreeSet<Int>()
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
        val hasShorthandCandidate = collectShorthandCandidateLines(event)
        needsFoldingUpdate = true
        scheduleRefresh(if (hasShorthandCandidate) 0 else 150, true)
    }

    fun refreshAfterStyleChange() = scheduleRefresh(0, false)

    private fun scheduleRefresh(delay: Int, updateFolding: Boolean = needsFoldingUpdate) {
        needsFoldingUpdate = needsFoldingUpdate || updateFolding
        alarm.cancelAllRequests()
        alarm.addRequest(::refresh, delay)
    }

    private fun refresh() {
        if (editor.isDisposed) return

        val styleSettings = SqlHeadingStyleSettings.getInstance()
        if (styleSettings.isEmphasisEnabled() && normalizeShorthandComments()) return

        if (!isSqlDocument()) {
            clearPresentationHighlighters()
            shorthandCandidateLines.clear()
            lastActiveLine = null
            return
        }

        val headings = SqlHeadingParser.parse(editor.document.charsSequence)

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
        headings.filter { heading ->
            editor.document.getLineNumber(heading.offset) != activeLine &&
                heading.titleStartOffset < heading.titleEndOffset
        }.forEach { heading ->
            presentationHighlighters += editor.markupModel.addRangeHighlighter(
                heading.titleStartOffset,
                heading.titleEndOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                TextAttributes().apply {
                    fontType = Font.BOLD
                    foregroundColor = styleSettings.headingColor(heading.level)
                },
                HighlighterTargetArea.EXACT_RANGE,
            )
        }

        if (!styleSettings.isEmphasisEnabled()) return
        SqlCommentParser.parseEmphasisComments(editor.document.charsSequence).forEach { comment ->
            presentationHighlighters += editor.markupModel.addRangeHighlighter(
                comment.markerStartOffset,
                comment.lineEndOffset,
                HighlighterLayer.ADDITIONAL_SYNTAX,
                emphasisAttributes(comment, styleSettings),
                HighlighterTargetArea.EXACT_RANGE,
            )
        }
    }

    private fun emphasisAttributes(
        comment: SqlEmphasisComment,
        styleSettings: SqlHeadingStyleSettings,
    ): TextAttributes {
        val attributes = editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT)
            ?.clone()
            ?: TextAttributes()
        if (comment.bold) attributes.fontType = attributes.fontType or Font.BOLD
        comment.colorMarker?.let { marker ->
            styleSettings.emphasisColor(marker)?.let { attributes.foregroundColor = it }
        }
        return attributes
    }

    private fun isSqlDocument(): Boolean = runReadActionBlocking {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
        SqlLanguageSupport.isSql(psiFile?.language)
    }

    private fun collectShorthandCandidateLines(event: DocumentEvent): Boolean {
        val document = editor.document
        val firstLine = document.getLineNumber(event.offset.coerceAtMost(document.textLength))
        val lastOffset = (event.offset + event.newLength).coerceAtMost(document.textLength)
        val lastLine = document.getLineNumber(lastOffset)
        (firstLine..lastLine).forEach(shorthandCandidateLines::add)
        return (firstLine..lastLine).any { line ->
            val startOffset = document.getLineStartOffset(line)
            val endOffset = document.getLineEndOffset(line)
            SqlCommentParser.shorthandCommentPrefixOffset(
                document.charsSequence.subSequence(startOffset, endOffset),
            ) != null
        }
    }

    private fun normalizeShorthandComments(): Boolean {
        if (shorthandCandidateLines.isEmpty()) return false

        val document = editor.document
        val candidates = shorthandCandidateLines.toList()
        shorthandCandidateLines.clear()
        val insertOffsets = candidates.mapNotNull { line ->
            if (line >= document.lineCount) return@mapNotNull null
            val startOffset = document.getLineStartOffset(line)
            val endOffset = document.getLineEndOffset(line)
            SqlCommentParser.shorthandCommentPrefixOffset(
                document.charsSequence.subSequence(startOffset, endOffset),
            )?.let { startOffset + it }
        }.sortedDescending()
        if (insertOffsets.isEmpty()) return false

        WriteCommandAction.runWriteCommandAction(project, "补全 SQL 强调注释", null, Runnable {
            insertOffsets.forEach { offset -> document.insertString(offset, "-- ") }
        })
        return true
    }

    private fun clearPresentationHighlighters() {
        presentationHighlighters.toList().forEach { highlighter ->
            if (highlighter.isValid) editor.markupModel.removeHighlighter(highlighter)
        }
        presentationHighlighters.clear()
    }

}
