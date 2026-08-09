package com.github.cgl.sqlheadings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlHeadingParserTest {
    @Test
    fun `parses one through five heading levels`() {
        val text = (1..5).joinToString("\n") { level ->
            "-- ${"#".repeat(level)} Heading $level"
        }

        val headings = SqlHeadingParser.parse(text)

        assertEquals(listOf(1, 2, 3, 4, 5), headings.map { it.level })
        assertEquals((1..5).map { "Heading $it" }, headings.map { it.title })
        assertEquals(text.indexOf('\n'), headings.first().foldStartOffset)
        assertEquals(text.indexOf("--"), headings.first().markerStartOffset)
        assertEquals(text.indexOf("Heading 1"), headings.first().titleStartOffset)
        assertEquals(text.indexOf("Heading 1") - 1, headings.first().labelFoldEndOffset)
        assertEquals(text.indexOf("Heading 1") + "Heading 1".length, headings.first().titleEndOffset)
    }

    @Test
    fun `parent section ends at next heading of same level`() {
        val text = """-- # First
select 1;
-- ## Child
select 2;
-- # Second
select 3;"""

        val headings = SqlHeadingParser.parse(text)

        assertEquals(text.indexOf("\n-- # Second"), headings[0].sectionEndOffset)
        assertEquals(text.indexOf("\n-- # Second"), headings[1].sectionEndOffset)
        assertEquals(text.length, headings[2].sectionEndOffset)
    }

    @Test
    fun `child section ends at next heading regardless of deeper content`() {
        val text = """-- # Parent
-- ## Child A
select 1;
-- ### Grandchild
select 2;
-- ## Child B
select 3;"""

        val headings = SqlHeadingParser.parse(text)

        assertEquals(text.indexOf("\n-- ## Child B"), headings[1].sectionEndOffset)
        assertEquals(text.indexOf("\n-- ## Child B"), headings[2].sectionEndOffset)
        assertEquals(text.length, headings[0].sectionEndOffset)
    }

    @Test
    fun `ignores malformed and inline markers`() {
        val text = """select '-- # not a heading';
--###### no separator
-- ###### too deep
  -- ### Valid
select 1;"""

        val headings = SqlHeadingParser.parse(text)

        assertEquals(1, headings.size)
        assertEquals(3, headings.single().level)
        assertEquals("Valid", headings.single().title)
    }

    @Test
    fun `handles crlf without including the separator before the next heading`() {
        val text = "-- # First\r\nselect 1;\r\n-- # Second\r\nselect 2;"

        val first = SqlHeadingParser.parse(text).first()

        assertEquals(text.indexOf("\r\n-- # Second"), first.sectionEndOffset)
        assertEquals(text.indexOf("\r\n"), first.foldStartOffset)
    }
}
