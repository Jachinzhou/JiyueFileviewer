package com.jachin.jiyue.util

/**
 * Markdown → HTML 转换器
 * 移植自鸿蒙版本 MarkdownParser.ets
 */
class MarkdownParser {

    private var codeBlockCount = 0
    private var inlineCodeCount = 0
    private val codeBlocks = mutableListOf<String>()
    private val inlineCodes = mutableListOf<String>()

    private val darkCss = """
<style>
body {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Noto Sans CJK SC", sans-serif;
  font-size: %%BASE_FONT_SIZE%%px;
  font-weight: 400;
  line-height: 1.6;
  color: #FFFFFF;
  background-color: #000000;
  padding: 16px;
  margin: 0;
  word-wrap: break-word;
}
h1 { font-size: 30px; font-weight: 700; margin: 24px 0 16px; line-height: 1.2; border-bottom: 0.5px solid rgba(255,255,255,0.05); padding-bottom: 8px; }
h2 { font-size: 24px; font-weight: 700; margin: 20px 0 12px; line-height: 1.2; border-bottom: 0.5px solid rgba(255,255,255,0.05); padding-bottom: 6px; }
h3 { font-size: 20px; font-weight: 700; margin: 16px 0 8px; line-height: 1.3; }
h4, h5, h6 { font-size: 16px; font-weight: 700; margin: 12px 0 8px; line-height: 1.3; }
p { margin: 8px 0; }
a { color: #317AF7; text-decoration: none; }
a:hover { text-decoration: underline; }
code { background: #1E1E1E; color: #FFFFFF; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 14px; }
pre { background: #1E1E1E; padding: 16px; border-radius: 8px; overflow-x: auto; margin: 16px 0; }
pre code { background: none; padding: 0; color: #FFFFFF; }
blockquote { border-left: 3px solid #317AF7; padding-left: 16px; color: rgba(255,255,255,0.6); margin: 16px 0; }
ul, ol { padding-left: 24px; margin: 8px 0; }
li { margin: 4px 0; }
hr { border: none; border-top: 0.5px solid rgba(255,255,255,0.05); margin: 24px 0; }
img { max-width: 100%; height: auto; border-radius: 8px; margin: 8px 0; }
table { border-collapse: collapse; width: 100%; margin: 16px 0; }
th, td { border: 0.5px solid rgba(255,255,255,0.05); padding: 8px 12px; text-align: left; }
th { background: #1E1E1E; font-weight: 700; }
del { color: rgba(255,255,255,0.5); }
</style>
""".trimIndent()

    private val lightCss = """
<style>
body {
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Noto Sans CJK SC", sans-serif;
  font-size: %%BASE_FONT_SIZE%%px;
  font-weight: 400;
  line-height: 1.6;
  color: #000000;
  background-color: #FFFFFF;
  padding: 16px;
  margin: 0;
  word-wrap: break-word;
}
h1 { font-size: 30px; font-weight: 700; margin: 24px 0 16px; line-height: 1.2; border-bottom: 0.5px solid rgba(0,0,0,0.05); padding-bottom: 8px; }
h2 { font-size: 24px; font-weight: 700; margin: 20px 0 12px; line-height: 1.2; border-bottom: 0.5px solid rgba(0,0,0,0.05); padding-bottom: 6px; }
h3 { font-size: 20px; font-weight: 700; margin: 16px 0 8px; line-height: 1.3; }
h4, h5, h6 { font-size: 16px; font-weight: 700; margin: 12px 0 8px; line-height: 1.3; }
p { margin: 8px 0; }
a { color: #0A59F7; text-decoration: none; }
a:hover { text-decoration: underline; }
code { background: #F5F5F7; color: #000000; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 14px; }
pre { background: #F5F5F7; padding: 16px; border-radius: 8px; overflow-x: auto; margin: 16px 0; }
pre code { background: none; padding: 0; color: #000000; }
blockquote { border-left: 3px solid #0A59F7; padding-left: 16px; color: rgba(0,0,0,0.6); margin: 16px 0; }
ul, ol { padding-left: 24px; margin: 8px 0; }
li { margin: 4px 0; }
hr { border: none; border-top: 0.5px solid rgba(0,0,0,0.05); margin: 24px 0; }
img { max-width: 100%; height: auto; border-radius: 8px; margin: 8px 0; }
table { border-collapse: collapse; width: 100%; margin: 16px 0; }
th, td { border: 0.5px solid rgba(0,0,0,0.05); padding: 8px 12px; text-align: left; }
th { background: #F5F5F7; font-weight: 700; }
del { color: rgba(0,0,0,0.5); }
</style>
""".trimIndent()

    fun parse(markdown: String, isDark: Boolean = false, fontSize: Int = 16): String {
        codeBlockCount = 0
        inlineCodeCount = 0
        codeBlocks.clear()
        inlineCodes.clear()

        var md = markdown
        md = extractFencedCodeBlocks(md)
        md = extractInlineCode(md)
        md = convertTables(md)
        md = convertHeadings(md)
        md = convertHorizontalRules(md)
        md = convertBlockquotes(md)
        md = convertLists(md)
        md = convertImages(md)
        md = convertLinks(md)
        md = convertInlineFormatting(md)
        md = convertParagraphs(md)
        md = restoreInlineCode(md)
        md = restoreCodeBlocks(md)
        md = md.replace(Regex("\n{3,}"), "\n\n")

        val baseCss = if (isDark) darkCss else lightCss
        val css = baseCss.replace("%%BASE_FONT_SIZE%%", fontSize.toString())
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">$css</head><body>$md</body></html>"
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
    }

    private fun extractFencedCodeBlocks(md: String): String {
        return md.replace(Regex("```(\\w*)\\n?([\\s\\S]*?)```")) { match ->
            val lang = match.groupValues[1]
            val code = match.groupValues[2].trimEnd('\n')
            val idx = codeBlockCount++
            val escaped = escapeHtml(code)
            val langLabel = if (lang.isNotEmpty()) {
                "<div style=\"font-size:12px;color:rgba(0,0,0,0.4);margin-bottom:6px;font-weight:500;\">$lang</div>"
            } else ""
            codeBlocks.add("<pre><code>${langLabel}${escaped}</code></pre>")
            "%%CB_${idx}%%"
        }
    }

    private fun extractInlineCode(md: String): String {
        return md.replace(Regex("`([^`]+)`")) { match ->
            val code = match.groupValues[1]
            val idx = inlineCodeCount++
            inlineCodes.add("<code>${escapeHtml(code)}</code>")
            "%%IC_${idx}%%"
        }
    }

    private fun convertHeadings(md: String): String {
        return md.replace(Regex("^(#{1,6})\\s+(.+)$", RegexOption.MULTILINE)) { match ->
            val level = match.groupValues[1].length
            val text = match.groupValues[2]
            "<h${level}>${text}</h${level}>"
        }
    }

    private fun convertHorizontalRules(md: String): String {
        return md.replace(Regex("^(?:---|\\*\\*\\*|___)\\s*$", RegexOption.MULTILINE), "<hr>")
    }

    private fun convertBlockquotes(md: String): String {
        val lines = md.split("\n")
        val result = mutableListOf<String>()
        var inBlockquote = false

        for (line in lines) {
            val match = Regex("^>\\s*(.*)").find(line)
            if (match != null) {
                if (!inBlockquote) {
                    result.add("<blockquote>")
                    inBlockquote = true
                }
                result.add(match.groupValues[1])
            } else {
                if (inBlockquote) {
                    result.add("</blockquote>")
                    inBlockquote = false
                }
                result.add(line)
            }
        }
        if (inBlockquote) result.add("</blockquote>")
        return result.joinToString("\n")
    }

    private fun convertLists(md: String): String {
        var result = md.replace(Regex("^[\\s]*[-*+]\\s+(.+)$", RegexOption.MULTILINE),
            "<_LI_>__UN__$1</_LI_>")
        result = result.replace(Regex("^[\\s]*\\d+\\.\\s+(.+)$", RegexOption.MULTILINE),
            "<_LI_>$1</_LI_>")
        result = result.replace(Regex("((?:<_LI_>.*</_LI_>\\n?)+)")) { match ->
            val block = match.groupValues[1]
            if (block.contains("__UN__")) {
                "<ul>" + block.replaceFirst("<_LI_>__UN__", "<li>")
                    .replace(Regex("__</_LI_>"), "</li>")
                    .replace("<_LI_>", "<li>") + "</ul>"
            } else {
                "<ol>" + block.replace("<_LI_>", "<li>")
                    .replace("</_LI_>", "</li>") + "</ol>"
            }
        }
        return result
    }

    private fun convertTables(md: String): String {
        return md.replace(Regex("((?:^\\|.+\\|\\s*\\n?)+)", RegexOption.MULTILINE)) { match ->
            val tableBlock = match.groupValues[1]
            val rows = tableBlock.trim().split("\n")
            if (rows.size < 2) return@replace tableBlock

            val dataRows = rows.filter { !it.matches(Regex("^\\|[\\s\\-:|]+\\|$")) }
            if (dataRows.isEmpty()) return@replace tableBlock

            val html = StringBuilder("<table>")
            val headers = dataRows[0].split("|").filter { it.trim().isNotEmpty() }
            html.append("<tr>")
            headers.forEach { html.append("<th>${it.trim()}</th>") }
            html.append("</tr>")
            for (i in 1 until dataRows.size) {
                val cells = dataRows[i].split("|").filter { it.trim().isNotEmpty() }
                html.append("<tr>")
                cells.forEach { html.append("<td>${it.trim()}</td>") }
                html.append("</tr>")
            }
            html.append("</table>")
            html.toString()
        }
    }

    private fun convertImages(md: String): String {
        return md.replace(Regex("!\\[([^\\]]*)\\]\\(([^)]+)\\)"), "<img src=\"$2\" alt=\"$1\">")
    }

    private fun convertLinks(md: String): String {
        return md.replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")
    }

    private fun convertInlineFormatting(md: String): String {
        var result = md.replace(Regex("\\*\\*\\*(.+?)\\*\\*\\*"), "<strong><em>$1</em></strong>")
        result = result.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
        result = result.replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
        result = result.replace(Regex("~~(.+?)~~"), "<del>$1</del>")
        return result
    }

    private fun convertParagraphs(md: String): String {
        val blocks = md.split("\n\n")
        return blocks.joinToString("\n\n") { block ->
            val trimmed = block.trim()
            if (trimmed.isEmpty()) return@joinToString ""
            if (trimmed.matches(Regex("^<(h[1-6]|ul|ol|li|blockquote|pre|hr|table|img).*"))) {
                return@joinToString trimmed
            }
            val withBr = trimmed.replace("\n", "<br>")
            "<p>${withBr}</p>"
        }
    }

    private fun restoreInlineCode(md: String): String {
        var result = md
        for (i in inlineCodes.indices) {
            result = result.replace("%%IC_${i}%%", inlineCodes[i])
        }
        return result
    }

    private fun restoreCodeBlocks(md: String): String {
        var result = md
        for (i in codeBlocks.indices) {
            result = result.replace("%%CB_${i}%%", codeBlocks[i])
        }
        return result
    }

    /**
     * 为 HTML 文件注入深色模式和字号支持
     */
    fun wrapHtmlContent(html: String, isDark: Boolean, fontSize: Int): String {
        var bodyContent = html
        val bodyMatch = Regex("<body[^>]*>([\\s\\S]*)</body>", RegexOption.IGNORE_CASE).find(html)
        if (bodyMatch != null) {
            bodyContent = bodyMatch.groupValues[1]
        }

        var injectedStyle = ""
        injectedStyle += "* { font-size: inherit !important; } "
        injectedStyle += "html { font-size: ${fontSize}px !important; } "

        if (isDark) {
            injectedStyle += "html { filter: invert(1) hue-rotate(180deg); background: #fff; } "
            injectedStyle += "img, video, iframe, svg { filter: invert(1) hue-rotate(180deg); } "
        }

        return "<style>${injectedStyle}</style><div id=\"__content_wrapper\">${bodyContent}</div>"
    }
}
