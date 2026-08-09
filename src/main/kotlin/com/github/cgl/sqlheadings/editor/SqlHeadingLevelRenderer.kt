package com.github.cgl.sqlheadings.editor

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

internal class SqlHeadingLevelRenderer(
    private val level: Int,
) : EditorCustomElementRenderer {
    private val label = "H$level"

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val metrics = inlay.editor.contentComponent.getFontMetrics(labelFont(inlay))
        return indentation(metrics.charWidth(' ')) + metrics.stringWidth(label) + metrics.charWidth(' ') * 2
    }

    override fun paint(
        inlay: Inlay<*>,
        graphics: Graphics,
        targetRegion: Rectangle,
        textAttributes: TextAttributes,
    ) {
        val oldFont = graphics.font
        val oldColor = graphics.color
        val font = labelFont(inlay)
        val metrics = graphics.getFontMetrics(font)
        val x = targetRegion.x + indentation(metrics.charWidth(' '))
        val y = targetRegion.y + (targetRegion.height - metrics.height) / 2 + metrics.ascent

        graphics.font = font
        graphics.color = textAttributes.foregroundColor ?: inlay.editor.colorsScheme.defaultForeground
        graphics.drawString(label, x, y)
        graphics.font = oldFont
        graphics.color = oldColor
    }

    private fun labelFont(inlay: Inlay<*>): Font =
        inlay.editor.colorsScheme.getFont(EditorFontType.BOLD)

    private fun indentation(spaceWidth: Int): Int = (level - 1) * spaceWidth * 2
}
