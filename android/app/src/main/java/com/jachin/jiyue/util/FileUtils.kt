package com.jachin.jiyue.util

import android.content.Context
import android.net.Uri
import com.jachin.jiyue.model.FileItem
import java.io.File
import java.io.FileOutputStream

/**
 * 文件工具类
 * 文件读取、类型识别、沙盒拷贝
 */
object FileUtils {

    /**
     * 根据 URI 后缀识别文件类型
     */
    fun getFileType(uri: Uri): String? {
        val name = uri.toString().lowercase()
        return when {
            name.endsWith(".md") || name.endsWith(".markdown") -> "md"
            name.endsWith(".html") || name.endsWith(".htm") -> "html"
            else -> null
        }
    }

    /**
     * 从 URI 中提取文件名
     */
    fun getFileName(context: Context, uri: Uri): String {
        // 先尝试从 ContentResolver 获取
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIdx >= 0) {
                name = cursor.getString(nameIdx)
            }
        }
        // fallback：从 URI path 提取
        if (name.isNullOrEmpty()) {
            val path = uri.path ?: uri.toString()
            name = path.split("/").lastOrNull()?.let {
                try { java.net.URLDecoder.decode(it, "UTF-8") } catch (_: Exception) { it }
            } ?: "unknown"
        }
        return name ?: "unknown"
    }

    /**
     * 读取 URI 对应的文件内容为字符串（UTF-8）
     */
    fun readFileContent(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw RuntimeException("无法读取文件")
    }

    /**
     * 读取本地文件路径的内容
     */
    fun readFileContent(filePath: String): String {
        return File(filePath).readText(Charsets.UTF_8)
    }

    /**
     * 检查文件是否存在
     */
    fun fileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 将 URI 指向的文件拷贝到应用沙盒目录
     * 返回沙盒中的文件路径
     */
    fun copyToSandbox(context: Context, uri: Uri, fileName: String): String {
        val filesDir = context.filesDir
        var destFile = File(filesDir, fileName)

        // 处理文件名冲突
        if (destFile.exists()) {
            val dotIdx = fileName.lastIndexOf('.')
            val baseName = if (dotIdx > 0) fileName.substring(0, dotIdx) else fileName
            val ext = if (dotIdx > 0) fileName.substring(dotIdx) else ""
            var suffix = 1
            while (destFile.exists()) {
                destFile = File(filesDir, "${baseName}_${suffix}${ext}")
                suffix++
            }
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw RuntimeException("无法读取源文件")

        return destFile.absolutePath
    }

    /**
     * 从 URI 构建 FileItem（拷贝到沙盒）
     */
    fun buildFileItem(context: Context, uri: Uri): FileItem? {
        val fileType = getFileType(uri) ?: return null
        val fileName = getFileName(context, uri)

        return try {
            val destPath = copyToSandbox(context, uri, fileName)
            val fileSize = File(destPath).length()
            FileItem(
                fileName = fileName,
                filePath = destPath,
                fileType = fileType,
                starred = false,
                lastOpened = System.currentTimeMillis(),
                fileSize = fileSize,
                sourceUri = uri.toString()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 删除沙盒中的文件
     */
    fun deleteSandboxFile(filePath: String) {
        try {
            File(filePath).delete()
        } catch (_: Exception) {}
    }
}
