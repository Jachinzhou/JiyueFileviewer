package com.jachin.jiyue.model

/**
 * 文件历史记录条目
 */
data class FileItem(
    val fileName: String,
    val filePath: String,       // 沙盒中的文件路径
    val fileType: String,       // "md" 或 "html"
    var starred: Boolean = false,
    var lastOpened: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,    // 字节
    val sourceUri: String? = null // 源文件的 URI（用于去重）
) {
    companion object {
        /**
         * 排序：星标优先，其次按最近打开时间倒序
         */
        fun sortItems(items: List<FileItem>): List<FileItem> {
            return items.sortedWith(compareByDescending<FileItem> { it.starred }
                .thenByDescending { it.lastOpened })
        }

        /**
         * 格式化相对时间（中文）
         */
        fun formatRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val months = days / 30
            val years = months / 12

            return when {
                seconds < 60 -> "刚刚"
                minutes < 60 -> "${minutes}分钟前"
                hours < 24 -> "${hours}小时前"
                days < 30 -> "${days}天前"
                years < 1 -> "${months}个月前"
                else -> "${years}年前"
            }
        }
    }
}
