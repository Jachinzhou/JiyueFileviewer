package com.jachin.jiyue

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.jachin.jiyue.databinding.ActivityReaderBinding
import com.jachin.jiyue.util.FileHistoryManager
import com.jachin.jiyue.util.FileUtils
import com.jachin.jiyue.util.MarkdownParser

/**
 * 文件阅读器页面
 * 直接使用 loadDataWithBaseURL 加载生成的 HTML，不依赖 asset 文件
 */
class ReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReaderBinding
    private val markdownParser = MarkdownParser()

    private var filePath: String = ""
    private var fileName: String = ""
    private var fileType: String = "html"
    private var isDark: Boolean = false
    private var fontSizeLevel: Int = 1 // 0=小 1=中 2=大

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 读取参数
        filePath = intent.getStringExtra("filePath") ?: ""
        fileName = intent.getStringExtra("fileName") ?: ""
        fileType = intent.getStringExtra("fileType") ?: "html"

        binding.tvFileName.text = fileName

        setupWebView()
        setupClickListeners()
        loadContent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        binding.webView.webViewClient = WebViewClient()
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.setBackgroundColor(
            if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        )
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnBackError.setOnClickListener { finish() }

        binding.btnMore.setOnClickListener {
            val visibility = if (binding.layoutToolbar.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
            binding.layoutToolbar.visibility = visibility
        }

        // 字号小
        binding.btnFontSmall.setOnClickListener {
            fontSizeLevel = maxOf(0, fontSizeLevel - 1)
            updateFontColors()
            refreshContent()
        }

        // 字号中
        binding.btnFontMedium.setOnClickListener {
            fontSizeLevel = 1
            updateFontColors()
            refreshContent()
        }

        // 字号大
        binding.btnFontLarge.setOnClickListener {
            fontSizeLevel = minOf(2, fontSizeLevel + 1)
            updateFontColors()
            refreshContent()
        }

        // 深色模式
        binding.btnDarkMode.setOnClickListener {
            isDark = !isDark
            binding.tvDarkMode.text = if (isDark) "☀" else "☾"
            binding.webView.setBackgroundColor(
                if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
            refreshContent()
        }
    }

    private fun updateFontColors() {
        val activeColor = getColor(R.color.app_primary)
        val inactiveColor = getColor(R.color.text_secondary)
        binding.tvFontSmall.setTextColor(if (fontSizeLevel == 0) activeColor else inactiveColor)
        binding.tvFontMedium.setTextColor(if (fontSizeLevel == 1) activeColor else inactiveColor)
        binding.tvFontLarge.setTextColor(if (fontSizeLevel == 2) activeColor else inactiveColor)
    }

    private fun getFontSize(): Int {
        return when (fontSizeLevel) {
            0 -> 14
            2 -> 20
            else -> 16
        }
    }

    /**
     * 生成完整的 HTML 字符串
     * 对于 MD 文件: MarkdownParser.parse() 已生成完整 HTML
     * 对于 HTML 文件: 需要包装 wrapHtmlContent 的片段为完整 HTML
     */
    private fun generateHtml(): String {
        val content = FileUtils.readFileContent(filePath)
        return if (fileType == "md") {
            markdownParser.parse(content, isDark, getFontSize())
        } else {
            // wrapHtmlContent 返回的是片段，需要包装成完整 HTML
            val fragment = markdownParser.wrapHtmlContent(content, isDark, getFontSize())
            if (fragment.trimStart().startsWith("<!DOCTYPE", ignoreCase = true) ||
                fragment.trimStart().startsWith("<html", ignoreCase = true)) {
                fragment
            } else {
                val bgColor = if (isDark) "#000000" else "#FFFFFF"
                val textColor = if (isDark) "#FFFFFF" else "#000000"
                "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<style>body{background:$bgColor;color:$textColor;padding:16px;margin:0;" +
                "font-family:-apple-system,BlinkMacSystemFont,sans-serif;}</style>" +
                "</head><body>$fragment</body></html>"
            }
        }
    }

    /**
     * 读取文件并直接加载到 WebView
     * 使用 loadDataWithBaseURL，不依赖 asset 文件和 JS 注入
     */
    private fun loadContent() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.webView.visibility = View.GONE
        binding.layoutError.visibility = View.GONE

        try {
            val html = generateHtml()
            binding.layoutLoading.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE

            // 直接加载生成的 HTML，不需要 empty.html 和 JS 注入
            binding.webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
            )
        } catch (e: Exception) {
            binding.layoutLoading.visibility = View.GONE
            binding.layoutError.visibility = View.VISIBLE
            binding.tvError.text = "文件已不存在，请重新导入"
            FileHistoryManager.remove(filePath)
        }
    }

    /**
     * 刷新内容（字号/深色模式切换）
     * 重新生成 HTML 并重新加载
     */
    private fun refreshContent() {
        try {
            val html = generateHtml()
            binding.webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
            )
        } catch (_: Exception) {}
    }
}
