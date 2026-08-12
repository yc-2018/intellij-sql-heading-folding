package com.github.cgl.sqlheadings.model

internal object SqlCommentParser {
    private val shorthandPattern = Regex(
        pattern = "^[\\t ]*@@?[a-z](?=[\\t ])",
        option = RegexOption.IGNORE_CASE,
    )

    private val emphasisPattern = Regex(
        pattern = "^[\\t ]*--[\\t ]+(@@?)([a-z]?)(?:[\\t ]+(.*?))?[\\t ]*\\r?$",
        options = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
    )

    fun parseEmphasisComments(text: CharSequence): List<SqlEmphasisComment> {
        val source = text.toString()
        return emphasisPattern.findAll(source).mapNotNull { match ->
            val marker = match.groupValues[1]
            val colorMarker = match.groupValues[2].singleOrNull()?.lowercaseChar()
            if (marker.length == 1 && colorMarker == null) return@mapNotNull null

            val lineEnd = match.range.last + 1 - if (source.getOrNull(match.range.last) == '\r') 1 else 0
            SqlEmphasisComment(
                markerStartOffset = source.indexOf("--", match.range.first).coerceAtLeast(match.range.first),
                lineEndOffset = lineEnd,
                bold = marker.length == 2,
                colorMarker = colorMarker,
            )
        }.toList()
    }

    fun shorthandCommentPrefixOffset(line: CharSequence): Int? {
        if (!shorthandPattern.containsMatchIn(line)) return null
        return line.indexOfFirst { character -> character != ' ' && character != '\t' }
    }

    fun normalizeShorthandComment(line: String): String? {
        val offset = shorthandCommentPrefixOffset(line) ?: return null
        return line.substring(0, offset) + "-- " + line.substring(offset)
    }

}
