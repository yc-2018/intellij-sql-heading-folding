package com.github.cgl.sqlheadings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlCommentParserTest {
    @Test
    fun `parses color and bold emphasis comments`() {
        val text = "select 1;\n  -- @r Red note\n-- @@ Bold note\n-- @@G Bold green note"

        val comments = SqlCommentParser.parseEmphasisComments(text)

        assertEquals(3, comments.size)
        assertEquals(SqlEmphasisColor.RED, comments[0].color)
        assertEquals(false, comments[0].bold)
        assertEquals(null, comments[1].color)
        assertEquals(true, comments[1].bold)
        assertEquals(SqlEmphasisColor.GREEN, comments[2].color)
        assertEquals(true, comments[2].bold)
        assertEquals(text.indexOf("-- @r"), comments[0].markerStartOffset)
        assertEquals(text.indexOf("\n-- @@"), comments[0].lineEndOffset)
        assertEquals(text.length, comments[2].lineEndOffset)
    }

    @Test
    fun `ignores inline and malformed emphasis markers`() {
        val text = "select '-- @r not a comment';\n--@r missing separator\n-- @ plain marker\n-- @x unknown color"

        assertEquals(emptyList<SqlEmphasisComment>(), SqlCommentParser.parseEmphasisComments(text))
    }

    @Test
    fun `supports every color marker without case sensitivity`() {
        val text = listOf('r', 'Y', 'b', 'G', 'c', 'O', 'p', 'M')
            .joinToString("\n") { marker -> "-- @$marker Note" }

        assertEquals(
            SqlEmphasisColor.values().toList(),
            SqlCommentParser.parseEmphasisComments(text).map { it.color },
        )
    }
}
