package com.github.cgl.sqlheadings.editor

import com.github.cgl.sqlheadings.model.SqlEmphasisMarkers
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.ui.JBColor
import java.awt.Color

@State(name = "SqlHeadingStyleSettings", storages = [Storage("sql-heading-folding.xml")])
internal class SqlHeadingStyleSettings : PersistentStateComponent<SqlHeadingStyleSettings.State> {
    data class State(
        var initialized: Boolean = false,
        var emphasisEnabled: Boolean = true,
        var headingColors: MutableMap<Int, String> = mutableMapOf(),
        var emphasisColors: MutableMap<String, String> = defaultEmphasisColors(),
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        state.headingColors = state.headingColors.toMutableMap()
        state.emphasisColors = if (state.initialized) {
            state.emphasisColors.toMutableMap()
        } else {
            defaultEmphasisColors()
        }
    }

    fun headingColor(level: Int): Color? = state.headingColors[level]?.toColor()

    fun isEmphasisEnabled(): Boolean = state.emphasisEnabled

    fun emphasisColor(marker: Char): Color? = state.emphasisColors[marker.toString()]?.toColor()

    fun update(
        emphasisEnabled: Boolean,
        headingColors: Map<Int, Color?>,
        emphasisColors: Map<Char, Color?>,
    ) {
        state.initialized = true
        state.emphasisEnabled = emphasisEnabled
        state.headingColors = headingColors.mapNotNull { (level, color) ->
            color?.toHex()?.let { level to it }
        }.toMap().toMutableMap()
        state.emphasisColors = state.emphasisColors.toMutableMap().apply {
            emphasisColors.forEach { (marker, value) ->
                if (value == null) remove(marker.toString()) else put(marker.toString(), value.toHex())
            }
        }
    }

    fun reset() {
        state = State(initialized = true)
    }

    companion object {
        fun getInstance(): SqlHeadingStyleSettings =
            ApplicationManager.getApplication().getService(SqlHeadingStyleSettings::class.java)

        fun defaultColor(marker: Char): Color? = when (marker) {
            'r' -> JBColor(Color(0xB3261E), Color(0xFF7B72))
            'y' -> JBColor(Color(0x8A6100), Color(0xF2C94C))
            'b' -> JBColor(Color(0x075DB7), Color(0x75B7FF))
            'g' -> JBColor(Color(0x17753A), Color(0x70C989))
            'c' -> JBColor(Color(0x007C91), Color(0x56D4DD))
            'o' -> JBColor(Color(0xA64B00), Color(0xFF9B5E))
            'p' -> JBColor(Color(0x6F42C1), Color(0xC59CFF))
            'm' -> JBColor(Color(0xA12C78), Color(0xFF79C6))
            else -> null
        }

        private fun defaultEmphasisColors(): MutableMap<String, String> =
            SqlEmphasisMarkers.defaults.associate { it.toString() to defaultColor(it)!!.toHex() }.toMutableMap()

        private fun String.toColor(): Color? = runCatching { Color.decode(this) }.getOrNull()

        private fun Color.toHex(): String = "#%06X".format(rgb and 0xFFFFFF)
    }
}
