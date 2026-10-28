package com.mofangvideo.feature.backup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mofangvideo.core.repo.RemoteBackupFile
import com.mofangvideo.feature.backup.databinding.ItemRemoteBackupBinding

/**
 * WebDAV 远程备份文件列表适配器，点击回调选中用于恢复。
 */
class RemoteBackupAdapter(
    private val onSelected: (RemoteBackupFile) -> Unit
) : ListAdapter<RemoteBackupFile, RemoteBackupAdapter.VH>(DIFF) {

    private var selectedName: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemRemoteBackupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemRemoteBackupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: RemoteBackupFile) {
            val context = binding.root.context
            binding.fileNameText.text = file.name
            binding.fileMetaText.text = context.getString(
                R.string.backup_remote_item_format,
                formatSize(file.sizeBytes),
                file.modifiedAt
            )
            binding.root.isSelected = file.name == selectedName
            binding.root.setOnClickListener {
                selectedName = file.name
                onSelected(file)
                it.post { notifyDataSetChanged() }
            }
        }
    }

    fun selectedFile(): RemoteBackupFile? = currentList.find { it.name == selectedName }

    companion object {
        fun formatSize(bytes: Long): String = when {
            bytes <= 0 -> "0 B"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }

        private val DIFF = object : DiffUtil.ItemCallback<RemoteBackupFile>() {
            override fun areItemsTheSame(oldItem: RemoteBackupFile, newItem: RemoteBackupFile) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: RemoteBackupFile, newItem: RemoteBackupFile) =
                oldItem == newItem
        }
    }
}
