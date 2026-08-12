package com.github.cgl.sqlheadings.model

internal data class SqlEmphasisComment(
    val markerStartOffset: Int,
    val lineEndOffset: Int,
    val bold: Boolean,
    val colorMarker: Char?,
)

internal object SqlEmphasisMarkers {
    val all: List<Char> = ('a'..'z').toList()
    val defaults: Set<Char> = setOf('r', 'y', 'b', 'g', 'c', 'o', 'p', 'm')
}
