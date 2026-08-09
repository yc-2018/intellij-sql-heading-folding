package com.github.cgl.sqlheadings.folding

import com.github.cgl.sqlheadings.model.SqlHeadingParser
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

internal class SqlHeadingFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ): Array<FoldingDescriptor> = SqlHeadingParser.parse(document.charsSequence)
        .asSequence()
        .filter { heading -> heading.foldStartOffset < heading.sectionEndOffset }
        .map { heading ->
            FoldingDescriptor(
                root.node,
                TextRange(heading.foldStartOffset, heading.sectionEndOffset),
                null,
                "...",
            )
        }
        .toList()
        .toTypedArray()

    override fun getPlaceholderText(node: ASTNode): String = "..."

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
