package com.github.cgl.sqlheadings.model

internal object SqlCommentParser {
    private val emphasisPattern = Regex(
        pattern = "^[\\t ]*--[\\t ]+(@@?)([rybgcopm]?)(?:[\\t ]+(.*?))?[\\t ]*\\r?$",
        options = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
    )

    fun parseEmphasisComments(text: CharSequence): List<SqlEmphasisComment> {
        val source = text.toString()
        return emphasisPattern.findAll(source).mapNotNull { match ->
            val marker = match.groupValues[1]
            val color = parseColor(match.groupValues[2])
            if (marker.length == 1 && color == null) return@mapNotNull null

            val lineEnd = match.range.last + 1 - if (source.getOrNull(match.range.last) == '\r') 1 else 0
            SqlEmphasisComment(
                markerStartOffset = source.indexOf("--", match.range.first).coerceAtLeast(match.range.first),
                lineEndOffset = lineEnd,
                bold = marker.length == 2,
                color = color,
            )
        }.toList()
    }

    private fun parseColor(value: String): SqlEmphasisColor? = when (value.lowercase()) {
        "r" -> SqlEmphasisColor.RED
        "y" -> SqlEmphasisColor.YELLOW
        "b" -> SqlEmphasisColor.BLUE
        "g" -> SqlEmphasisColor.GREEN
        "c" -> SqlEmphasisColor.CYAN
        "o" -> SqlEmphasisColor.ORANGE
        "p" -> SqlEmphasisColor.PURPLE
        "m" -> SqlEmphasisColor.MAGENTA
        else -> null
    }
}
