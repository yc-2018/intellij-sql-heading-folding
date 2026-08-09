package com.github.cgl.sqlheadings.toolwindow

import com.intellij.lang.Language

internal object SqlLanguageSupport {
    fun isSql(language: Language?): Boolean {
        var current = language
        while (current != null) {
            if (current.id.equals("SQL", ignoreCase = true)) return true
            current = current.baseLanguage
        }
        return false
    }
}
