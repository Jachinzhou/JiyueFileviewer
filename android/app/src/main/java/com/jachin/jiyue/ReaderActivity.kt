package com.jachin.jiyue

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.jachin.jiyue.databinding.ActivityReaderBinding
import com.jachin.jiyue.util.FileHistoryManager
import com.jachin.jiyue.util.FileUtils
import com.jachin.jiyue.util.MarkdownParser

/**
 * 文件阅读器页面
 * WebView 加载 empty.html，通过 runJavaScript 桥接注入内容
 */
class ReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReaderBinding
    private val markdownParser = MarkdownParser()

    private var filePath: String = ""
    private var fileName: String = ""
    private var fileType: String = "html"
    private var isDark: Boolean = false
    private var fontSizeLevel: Int = 1 // 0=小 1=中 2=大
    private var pageReady: Boolean = false
    private var htmlContent: String = ""

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
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pageReady = true
                if (htmlContent.isNotEmpty()) {
                    injectContent()
                }
            }
        }
        binding.webView.webChromeClient = WebChromeClient()
        binding.webView.setBackgroundColor(
            if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        )

        // 加载空壳页面
        binding.webView.loadUrl("file:///android_asset/empty.html")
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
     * 读取文件并生成 HTML 内容
     */
    private fun loadContent() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.webView.visibility = View.GONE
        binding.layoutError.visibility = View.GONE

        try {
            val content = FileUtils.readFileContent(filePath)
            htmlContent = if (fileType == "html") {
                markdownParser.wrapHtmlContent(content, isDark, getFontSize())
            } else {
                markdownParser.parse(content, isDark, getFontSize())
            }

            binding.layoutLoading.visibility = View.GONE
            binding.webView.visibility = View.VISIBLE

            if (pageReady) {
                injectContent()
            }
        } catch (e: Exception) {
            binding.layoutLoading.visibility = View.GONE
            binding.layoutError.visibility = View.VISIBLE
            binding.tvError.text = "文件已不存在，请重新导入"
            FileHistoryManager.remove(filePath)
        }
    }

    /**
     * 通过 JS 注入内容到 WebView
     */
    private fun injectContent() {
        if (htmlContent.isEmpty()) return
        val escaped = org.json.JSONObject.wrap(htmlContent).toString()
        binding.webView.evaluateJavascript("__setContent($escaped);", null)
    }

    /**
     * 刷新内容（字号/深色模式切换）
     */
    private fun refreshContent() {
        try {
            val content = FileUtils.readFileContent(filePath)
            htmlContent = if (fileType == "html") {
                markdownParser.wrapHtmlContent(content, isDark, getFontSize())
            } else {
                markdownParser.parse(content, isDark, getFontSize())
            }
            injectContent()
        } catch (_: Exception) {}
    }
}
