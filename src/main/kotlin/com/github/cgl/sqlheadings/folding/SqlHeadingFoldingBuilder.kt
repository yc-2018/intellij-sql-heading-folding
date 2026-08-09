package com.github.cgl.sqlheadings.folding

import com.github.cgl.sqlheadings.model.SqlHeadingParser
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.Collections

internal class SqlHeadingFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        SqlHeadingParser.parse(document.charsSequence).forEach { heading ->
            if (heading.titleStartOffset < heading.titleEndOffset) {
                descriptors += FoldingDescriptor(
                    root.node,
                    TextRange(heading.markerStartOffset, heading.titleStartOffset),
                    null,
                    Collections.emptySet(),
                    false,
                    "${"  ".repeat(heading.level - 1)}H${heading.level}  ",
                    true,
                )
            }

            if (heading.foldStartOffset < heading.sectionEndOffset) {
                descriptors += FoldingDescriptor(
                    root.node,
                    TextRange(heading.foldStartOffset, heading.sectionEndOffset),
                    null,
                    "...",
                )
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
