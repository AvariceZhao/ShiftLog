package com.clockin.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleMarkdownTextTest {
    @Test
    fun parseSimpleMarkdown_recognizesIndentedGitHubReleaseNotes() {
        val rendered = parseSimpleMarkdown(
            "  ## 新功能\n\n  - 支持自动更新\n  1. 支持一位小数",
        )

        assertEquals("新功能\n\n• 支持自动更新\n1. 支持一位小数", rendered.text)
    }
}
