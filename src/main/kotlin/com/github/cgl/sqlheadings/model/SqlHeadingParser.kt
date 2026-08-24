package com.github.cgl.sqlheadings.model

internal object SqlHeadingParser {
    private val headingPattern = Regex(
        pattern = "^[\\t ]*--[\\t ]+(#{1,5})(?:[\\t ]+(.*?))?[\\t ]*\\r?$",
        option = RegexOption.MULTILINE,
    )

    fun parse(text: CharSequence): List<SqlHeading> {
        val source = text.toString()
        val matches = headingPattern.findAll(source).toList()
        val boundaries = IntArray(matches.size)
        val nextOffsetAtLevel = IntArray(6) { source.length }

        for (index in matches.indices.reversed()) {
            val level = matches[index].groupValues[1].length
            boundaries[index] = (1..level).minOf { nextLevel -> nextOffsetAtLevel[nextLevel] }
            nextOffsetAtLevel[level] = matches[index].range.first
        }

        return matches.mapIndexed { index, match ->
            val level = match.groupValues[1].length
            val lineEnd = headingLineEnd(source, match.range.first)
            val titleRange = match.groups[2]?.range
            val markerStart = source.indexOf("--", match.range.first).coerceAtLeast(match.range.first)

            SqlHeading(
                level = level,
                title = match.groups[2]?.value?.trim().orEmpty(),
                offset = match.range.first,
                markerStartOffset = markerStart,
                labelFoldEndOffset = ((titleRange?.first ?: lineEnd) - 1).coerceAtLeast(markerStart),
                titleStartOffset = titleRange?.first ?: lineEnd,
                titleEndOffset = titleRange?.last?.plus(1) ?: lineEnd,
                foldStartOffset = lineEnd,
                contentStartOffset = contentStart(source, match.range.first),
                sectionEndOffset = sectionEnd(source, boundaries[index]),
            )
        }
    }

    private fun headingLineEnd(text: String, headingOffset: Int): Int {
        val lineFeed = text.indexOf('\n', headingOffset)
        var end = if (lineFeed == -1) text.length else lineFeed
        if (end > headingOffset && text[end - 1] == '\r') end--
        return end
    }

    private fun contentStart(text: String, headingOffset: Int): Int {
        val lineFeed = text.indexOf('\n', headingOffset)
        return if (lineFeed == -1) text.length else lineFeed + 1
    }

    private fun sectionEnd(text: String, boundaryOffset: Int): Int {
        var end = boundaryOffset
        if (end > 0 && text[end - 1] == '\n') end--
        if (end > 0 && text[end - 1] == '\r') end--

        val lastLineStart = text.lastIndexOf('\n', (end - 1).coerceAtLeast(0)).let { lineFeed ->
            if (lineFeed == -1) 0 else lineFeed + 1
        }
        if (text.substring(lastLineStart, end).isBlank() && lastLineStart > 0) {
            end = lastLineStart - 1
            if (end > 0 && text[end - 1] == '\r') end--
        }
        return end
    }
}
