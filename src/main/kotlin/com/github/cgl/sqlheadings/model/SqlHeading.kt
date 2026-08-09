package com.github.cgl.sqlheadings.model

internal data class SqlHeading(
    val level: Int,
    val title: String,
    val offset: Int,
    val foldStartOffset: Int,
    val contentStartOffset: Int,
    val sectionEndOffset: Int,
)
