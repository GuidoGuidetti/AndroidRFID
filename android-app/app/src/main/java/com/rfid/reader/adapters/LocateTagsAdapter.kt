package com.rfid.reader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rfid.reader.databinding.ItemLocateTagBinding

data class LocateTag(
    val epc: String,
    val rssi: Int,
    val isSelected: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

class LocateTagsAdapter(
    private val onTagClick: (LocateTag) -> Unit
) : ListAdapter<LocateTag, LocateTagsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocateTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLocateTagBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: LocateTag) {
            binding.tvEpc.text = tag.epc
            binding.tvRssi.text = "RSSI: ${tag.rssi} dBm"
            binding.ivSelected.visibility = if (tag.isSelected) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onTagClick(tag)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<LocateTag>() {
        override fun areItemsTheSame(oldItem: LocateTag, newItem: LocateTag): Boolean {
            return oldItem.epc == newItem.epc
        }

        override fun areContentsTheSame(oldItem: LocateTag, newItem: LocateTag): Boolean {
            return oldItem == newItem
        }
    }
}
