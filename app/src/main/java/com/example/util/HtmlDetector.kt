package com.example.util

data class DetectedHtmlMeta(
    val title: String? = null,
    val description: String? = null,
    val author: String? = null,
    val themeColor: String? = null
)

object HtmlDetector {
    fun parseHtml(content: String): DetectedHtmlMeta {
        val titleRegex = "<title[^>]*>([^<]+)</title>".toRegex(RegexOption.IGNORE_CASE)
        val title = titleRegex.find(content)?.groupValues?.get(1)?.trim()

        val themeColorRegex = "<meta[^>]+name=[\"']theme-color[\"'][^>]+content=[\"']([^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
        val themeColor = themeColorRegex.find(content)?.groupValues?.get(1)?.trim()
            ?: "<meta[^>]+content=[\"']([^\"']+)[\"'][^>]+name=[\"']theme-color[\"']".toRegex(RegexOption.IGNORE_CASE)
                .find(content)?.groupValues?.get(1)?.trim()

        val descRegex = "<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
        val description = descRegex.find(content)?.groupValues?.get(1)?.trim()

        val authorRegex = "<meta[^>]+name=[\"']author[\"'][^>]+content=[\"']([^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
        val author = authorRegex.find(content)?.groupValues?.get(1)?.trim()

        return DetectedHtmlMeta(
            title = title,
            description = description,
            author = author,
            themeColor = themeColor
        )
    }
}
