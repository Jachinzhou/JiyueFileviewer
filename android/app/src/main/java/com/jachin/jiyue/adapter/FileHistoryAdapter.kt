package com.jachin.jiyue.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jachin.jiyue.R
import com.jachin.jiyue.model.FileItem

class FileHistoryAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onStarClick: (FileItem) -> Unit
) : ListAdapter<FileItem, FileHistoryAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutItem: View = itemView.findViewById(R.id.layoutItem)
        val tvIconText: TextView = itemView.findViewById(R.id.tvIconText)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileType: TextView = itemView.findViewById(R.id.tvFileType)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val btnStar: TextView = itemView.findViewById(R.id.btnStar)
        val divider: View = itemView.findViewById(R.id.divider)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        // 文件名
        holder.tvFileName.text = item.fileName

        // 文件类型
        holder.tvFileType.text = if (item.fileType == "md") "Markdown" else "HTML"

        // 时间
        holder.tvTime.text = FileItem.formatRelativeTime(item.lastOpened)

        // 图标
        val iconText = if (item.fileType == "md") "MD" else "HT"
        holder.tvIconText.text = iconText
        val iconColor = if (item.fileType == "md") {
            ContextCompat.getColor(context, R.color.md_icon_green)
        } else {
            ContextCompat.getColor(context, R.color.html_icon_blue)
        }
        val bg = holder.tvIconText.background as? GradientDrawable
        bg?.setColor(iconColor)
            ?: run {
                val newBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f
                    setColor(iconColor)
                }
                holder.tvIconText.background = newBg
            }

        // 星标
        holder.btnStar.text = if (item.starred) "★" else "☆"
        val starColor = if (item.starred) {
            ContextCompat.getColor(context, R.color.star_yellow)
        } else {
            ContextCompat.getColor(context, R.color.text_tertiary)
        }
        holder.btnStar.setTextColor(starColor)

        // 分隔线 - 最后一个不显示
        holder.divider.visibility = if (position == itemCount - 1) View.GONE else View.VISIBLE

        // 点击事件
        holder.layoutItem.setOnClickListener { onItemClick(item) }
        holder.btnStar.setOnClickListener { onStarClick(item) }
    }

    class DiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}
