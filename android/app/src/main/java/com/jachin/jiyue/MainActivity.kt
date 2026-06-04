package com.jachin.jiyue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jachin.jiyue.adapter.FileHistoryAdapter
import com.jachin.jiyue.databinding.ActivityMainBinding
import com.jachin.jiyue.model.FileItem
import com.jachin.jiyue.util.FileHistoryManager
import com.jachin.jiyue.util.FileUtils

/**
 * 主页面 - 文件历史列表
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FileHistoryAdapter

    // 文件选择器（支持多选）
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        handleImportedFiles(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 FileHistory
        FileHistoryManager.init(this)

        setupRecyclerView()
        setupClickListeners()
        loadHistory()

        // 处理从外部打开的文件
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun setupRecyclerView() {
        adapter = FileHistoryAdapter(
            onItemClick = { item -> openReader(item) },
            onStarClick = { item ->
                FileHistoryManager.toggleStar(item.filePath)
                loadHistory()
            }
        )
        binding.rvFileList.layoutManager = LinearLayoutManager(this)
        binding.rvFileList.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnImport.setOnClickListener { importFile() }
        binding.btnSelectFile.setOnClickListener { importFile() }
    }

    private fun loadHistory() {
        val items = FileHistoryManager.getAll()
        adapter.submitList(items)
        updateVisibility(items.isEmpty())
    }

    private fun updateVisibility(isEmpty: Boolean) {
        binding.rvFileList.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.progressBar.visibility = View.GONE
    }

    /**
     * 使用 SAF (Storage Access Framework) 选择文件
     */
    private fun importFile() {
        filePickerLauncher.launch(
            arrayOf(
                "text/markdown",
                "text/html",
                "text/plain",
                "application/octet-stream",
                "*/*"
            )
        )
    }

    private fun handleImportedFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return

        var lastValidItem: FileItem? = null
        var found = false

        for (uri in uris) {
            val fileType = FileUtils.getFileType(uri)
            if (fileType == null) continue
            found = true

            val fileItem = FileUtils.buildFileItem(this, uri) ?: continue
            FileHistoryManager.add(fileItem)

            // 获取最终的条目（可能因去重被更新）
            val allItems = FileHistoryManager.getAll()
            val finalItem = allItems.find { it.fileName == fileItem.fileName }
            lastValidItem = finalItem ?: fileItem
        }

        loadHistory()

        // 打开最后一个有效文件
        lastValidItem?.let { openReader(it) }

        if (!found) {
            Toast.makeText(this, "不支持的文件类型，请选择 .md 或 .html 文件", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openReader(item: FileItem) {
        if (!FileUtils.fileExists(item.filePath)) {
            Toast.makeText(this, "文件已被删除，将从历史中移除", Toast.LENGTH_SHORT).show()
            FileHistoryManager.remove(item.filePath)
            loadHistory()
            return
        }

        val intent = Intent(this, ReaderActivity::class.java).apply {
            putExtra("filePath", item.filePath)
            putExtra("fileName", item.fileName)
            putExtra("fileType", item.fileType)
        }
        startActivity(intent)
    }

    /**
     * 处理从外部打开/分享的文件
     */
    private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) handleImportedFiles(listOf(uri))
            }
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri -> handleImportedFiles(listOf(uri)) }
            }
        }
    }
}
