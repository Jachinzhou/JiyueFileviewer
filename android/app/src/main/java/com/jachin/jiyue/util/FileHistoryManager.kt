package com.jachin.jiyue.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jachin.jiyue.model.FileItem

/**
 * 文件历史管理器
 * 基于 SharedPreferences + Gson 持久化存储
 */
object FileHistoryManager {
    private const val PREF_NAME = "file_history"
    private const val KEY_HISTORY = "file_history_list"
    private const val MAX_HISTORY = 50

    private val gson = Gson()
    private var prefs: SharedPreferences? = null
    private var cache: MutableList<FileItem> = mutableListOf()

    /**
     * 初始化（在 Application 或 MainActivity.onCreate 中调用）
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs?.getString(KEY_HISTORY, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<FileItem>>() {}.type
        cache = try {
            gson.fromJson<MutableList<FileItem>>(raw, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    /**
     * 获取所有历史条目（已排序）
     */
    fun getAll(): List<FileItem> {
        return FileItem.sortItems(cache)
    }

    /**
     * 添加或更新条目
     * 同 filePath 或同 fileName 则更新 lastOpened，否则新增
     */
    fun add(item: FileItem) {
        // 去重：先按 filePath 精确匹配，再按 fileName 匹配
        var existingIdx = cache.indexOfFirst { it.filePath == item.filePath }
        if (existingIdx < 0 && item.fileName.isNotEmpty()) {
            existingIdx = cache.indexOfFirst { it.fileName == item.fileName }
        }

        if (existingIdx >= 0) {
            val existing = cache[existingIdx]
            val updated = item.copy(
                starred = existing.starred,
                filePath = existing.filePath // 保留原始路径
            )
            cache[existingIdx] = updated
        } else {
            cache.add(item)
        }

        evict()
        persist()
    }

    /**
     * 切换星标
     */
    fun toggleStar(filePath: String) {
        val idx = cache.indexOfFirst { it.filePath == filePath }
        if (idx >= 0) {
            cache[idx] = cache[idx].copy(starred = !cache[idx].starred)
            persist()
        }
    }

    /**
     * 移除条目
     */
    fun remove(filePath: String) {
        cache.removeAll { it.filePath == filePath }
        persist()
    }

    /**
     * 清空历史（保留星标文件）
     */
    fun clear() {
        cache = cache.filter { it.starred }.toMutableList()
        persist()
    }

    private fun evict() {
        if (cache.size <= MAX_HISTORY) return
        val starred = cache.filter { it.starred }
        val unstarred = cache.filter { !it.starred }
        val keepCount = maxOf(0, MAX_HISTORY - starred.size)
        cache = (starred + unstarred.take(keepCount)).toMutableList()
    }

    private fun persist() {
        prefs?.edit()?.putString(KEY_HISTORY, gson.toJson(cache))?.apply()
    }
}
