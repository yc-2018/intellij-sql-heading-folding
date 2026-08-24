package com.github.cgl.sqlheadings.toolwindow

import com.github.cgl.sqlheadings.editor.SqlHeadingPresentationListener
import com.github.cgl.sqlheadings.editor.SqlHeadingStyleSettings
import com.github.cgl.sqlheadings.model.SqlEmphasisMarkers
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColorChooserService
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import com.intellij.ui.components.JBCheckBox

internal class SqlHeadingStyleDialog(private val project: Project) : DialogWrapper(project, true) {
    private val settings = SqlHeadingStyleSettings.getInstance()
    private val headingColors: MutableMap<Int, Color?> =
        (1..5).associateWith(settings::headingColor).toMutableMap()
    private val emphasisColors: MutableMap<Char, Color?> =
        SqlEmphasisMarkers.all.associateWith(settings::emphasisColor).toMutableMap()
    private val emphasisEnabled = JBCheckBox("启用颜色注释", settings.isEmphasisEnabled())
    private val swatches = mutableMapOf<Any, ColorSwatch>()

    init {
        title = "配置样式"
        init()
    }

    override fun createCenterPanel(): JComponent = JBPanel<JBPanel<*>>(BorderLayout(0, 12)).apply {
        preferredSize = Dimension(560, 410)
        add(createHeadingSection(), BorderLayout.NORTH)
        add(createEmphasisSection(), BorderLayout.CENTER)
    }

    override fun createSouthPanel(): JComponent = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(JButton("重置").apply { addActionListener { resetColors() } }, BorderLayout.WEST)
        add(
            DialogWrapper.layoutButtonsPanel(
                listOf(createJButtonForAction(cancelAction), createJButtonForAction(okAction)),
            ),
            BorderLayout.EAST,
        )
    }

    override fun doOKAction() {
        settings.update(emphasisEnabled.isSelected, headingColors, emphasisColors)
        SqlHeadingPresentationListener.refreshAllPresentations()
        super.doOKAction()
    }

    private fun resetColors() {
        (1..5).forEach { headingColors[it] = null }
        SqlEmphasisMarkers.all.forEach { marker ->
            emphasisColors[marker] = SqlHeadingStyleSettings.defaultColor(marker)
        }
        updateSwatches()
    }

    private fun createHeadingSection(): JPanel = JPanel(GridLayout(1, 5, 6, 0)).apply {
        border = com.intellij.ui.IdeBorderFactory.createTitledBorder("标题颜色", false)
        (1..5).forEach { level -> add(createEntry("H$level", level, compact = true)) }
    }

    private fun createEmphasisSection(): JPanel = JBPanel<JBPanel<*>>(BorderLayout(0, 6)).apply {
        add(JBPanel<JBPanel<*>>(BorderLayout(8, 0)).apply {
            add(JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                add(JBLabel("@ 注释颜色"))
                add(emphasisEnabled)
            }, BorderLayout.WEST)
            add(JSeparator(), BorderLayout.CENTER)
        }, BorderLayout.NORTH)
        add(JPanel(GridLayout(6, 5, 3, 4)).apply {
            SqlEmphasisMarkers.all.forEach { marker -> add(createEntry(marker.toString(), marker)) }
            repeat(4) { add(JPanel()) }
        }, BorderLayout.CENTER)
    }

    private fun createEntry(label: String, key: Any, compact: Boolean = false): JPanel =
        JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
        add(JBLabel(label).apply { preferredSize = Dimension(18, 24) })
        val swatch = ColorSwatch(if (compact) Dimension(40, 26) else Dimension(56, 24)).apply {
            preferredSize = if (compact) Dimension(40, 26) else Dimension(56, 24)
            toolTipText = "点击选择 $label 颜色"
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    val selected = ColorChooserService.getInstance()
                        .showDialog(project, this@apply, "选择 $label 颜色", colorFor(key), true)
                    if (selected != null) setColor(key, selected)
                }
            })
        }
        swatches[key] = swatch
        add(swatch)
        add(JButton(AllIcons.Actions.Close).apply {
            preferredSize = Dimension(24, 24)
            toolTipText = "清除 $label 颜色"
            addActionListener { setColor(key, null) }
        })
        updateSwatches()
    }

    private fun colorFor(key: Any): Color? = when (key) {
        is Int -> headingColors[key]
        is Char -> emphasisColors[key]
        else -> null
    }

    private fun setColor(key: Any, color: Color?) {
        when (key) {
            is Int -> headingColors[key] = color
            is Char -> emphasisColors[key] = color
        }
        updateSwatches()
    }

    private fun updateSwatches() {
        swatches.forEach { (key, button) ->
            button.color = colorFor(key)
        }
    }

    private class ColorSwatch(size: Dimension) : JComponent() {
        var color: Color? = null
            set(value) {
                field = value
                repaint()
            }

        init {
            preferredSize = size
            minimumSize = size
            maximumSize = size
            isOpaque = false
        }

        override fun paintComponent(graphics: Graphics) {
            val selectedColor = color
            if (selectedColor != null) {
                graphics.color = selectedColor
                graphics.fillRect(0, 0, width, height)
                return
            }

            graphics.color = foreground
            graphics.drawRect(0, 0, width - 1, height - 1)
            val metrics = graphics.fontMetrics
            val text = "无"
            graphics.drawString(text, (width - metrics.stringWidth(text)) / 2, (height - metrics.height) / 2 + metrics.ascent)
        }
    }
}
