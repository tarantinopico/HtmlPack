package com.example

import com.example.data.model.ManifestModelsAdapterHelper
import com.example.data.model.ProjectManifest
import com.example.util.HtmlDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlPackUnitTest {

    @Test
    fun testHtmlDetector() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <title>Moje Testovaci Aplikace</title>
              <meta name="description" content="Skvela HTML aplikace">
              <meta name="author" content="Jan Novak">
              <meta name="theme-color" content="#1E3A8A">
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val detected = HtmlDetector.parseHtml(html)
        assertEquals("Moje Testovaci Aplikace", detected.title)
        assertEquals("Skvela HTML aplikace", detected.description)
        assertEquals("Jan Novak", detected.author)
        assertEquals("#1E3A8A", detected.themeColor)
    }

    @Test
    fun testManifestSerialization() {
        val manifest = ProjectManifest()
        val json = ManifestModelsAdapterHelper.manifestToJson(manifest)
        assertNotNull(json)
        assertTrue(json.contains("schemaVersion"))

        val parsed = ManifestModelsAdapterHelper.manifestFromJson(json)
        assertNotNull(parsed)
        assertEquals(manifest.meta.schemaVersion, parsed?.meta?.schemaVersion)
    }
}
