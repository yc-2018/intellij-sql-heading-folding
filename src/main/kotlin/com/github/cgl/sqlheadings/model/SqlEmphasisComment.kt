package com.github.cgl.sqlheadings.model

internal data class SqlEmphasisComment(
    val markerStartOffset: Int,
    val lineEndOffset: Int,
    val bold: Boolean,
    val color: SqlEmphasisColor?,
)

internal enum class SqlEmphasisColor {
    RED,
    YELLOW,
    BLUE,
    GREEN,
    CYAN,
    ORANGE,
    PURPLE,
    MAGENTA,
}
