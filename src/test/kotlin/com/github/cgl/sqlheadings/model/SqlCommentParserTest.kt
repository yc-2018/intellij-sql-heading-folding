package com.github.cgl.sqlheadings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlCommentParserTest {
    @Test
    fun `parses color and bold emphasis comments`() {
        val text = "select 1;\n  -- @r Red note\n-- @@ Bold note\n-- @@G Bold green note"

        val comments = SqlCommentParser.parseEmphasisComments(text)

        assertEquals(3, comments.size)
        assertEquals('r', comments[0].colorMarker)
        assertEquals(false, comments[0].bold)
        assertEquals(null, comments[1].colorMarker)
        assertEquals(true, comments[1].bold)
        assertEquals('g', comments[2].colorMarker)
        assertEquals(true, comments[2].bold)
        assertEquals("Red note", comments[0].text)
        assertEquals("Bold note", comments[1].text)
        assertEquals(text.indexOf("-- @r"), comments[0].markerStartOffset)
        assertEquals(text.indexOf("\n-- @@"), comments[0].lineEndOffset)
        assertEquals(text.length, comments[2].lineEndOffset)
    }

    @Test
    fun `ignores inline and malformed emphasis markers`() {
        val text = "select '-- @r not a comment';\n--@r missing separator\n-- @ plain marker"

        assertEquals(emptyList<SqlEmphasisComment>(), SqlCommentParser.parseEmphasisComments(text))
    }

    @Test
    fun `supports every color marker without case sensitivity`() {
        val text = listOf('r', 'Y', 'b', 'G', 'c', 'O', 'p', 'M')
            .joinToString("\n") { marker -> "-- @$marker Note" }

        assertEquals(
            listOf('r', 'y', 'b', 'g', 'c', 'o', 'p', 'm'),
            SqlCommentParser.parseEmphasisComments(text).map { it.colorMarker },
        )
    }

    @Test
    fun `supports all configurable letter markers`() {
        val text = ('a'..'z').joinToString("\n") { marker -> "-- @$marker Note" }

        assertEquals(
            ('a'..'z').toList(),
            SqlCommentParser.parseEmphasisComments(text).map { it.colorMarker },
        )
    }

    @Test
    fun `recognizes line start shorthand with indentation`() {
        assertEquals(2, SqlCommentParser.shorthandCommentPrefixOffset("  @r Red note"))
        assertEquals(1, SqlCommentParser.shorthandCommentPrefixOffset("\t@@G Bold green note"))
        assertEquals(null, SqlCommentParser.shorthandCommentPrefixOffset("@r"))
        assertEquals(null, SqlCommentParser.shorthandCommentPrefixOffset("@@g"))
        assertEquals(null, SqlCommentParser.shorthandCommentPrefixOffset("@result"))
        assertEquals(null, SqlCommentParser.shorthandCommentPrefixOffset("@@ROWCOUNT"))
        assertEquals(null, SqlCommentParser.shorthandCommentPrefixOffset("@ plain note"))
        assertEquals(null, SqlCommentParser.shorthandCommentPrefixOffset("select @r"))
        assertEquals(null, SqlCommentParser.shorthandCommentPrefixOffset("@rNot separated"))
        assertEquals(0, SqlCommentParser.shorthandCommentPrefixOffset("@a Custom color"))
        assertEquals(0, SqlCommentParser.shorthandCommentPrefixOffset("@@z Bold custom color"))
    }

    @Test
    fun `normalizes shorthand into valid SQL comments`() {
        assertEquals("-- @r Red note", SqlCommentParser.normalizeShorthandComment("@r Red note"))
        assertEquals("  -- @@g Bold green", SqlCommentParser.normalizeShorthandComment("  @@g Bold green"))
        assertEquals(null, SqlCommentParser.normalizeShorthandComment("select @r"))
    }
}
