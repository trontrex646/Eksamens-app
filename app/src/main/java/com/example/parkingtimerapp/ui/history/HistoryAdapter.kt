package com.example.parkingtimerapp.ui.history

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.parkingtimerapp.R
import com.example.parkingtimerapp.data.HistoryEntry
import com.example.parkingtimerapp.databinding.ItemHistoryBinding
import com.example.parkingtimerapp.utils.LanguageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private val onDeleteClick: (HistoryEntry) -> Unit) :
    ListAdapter<HistoryEntry, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryEntry) {
            val wrappedContext = LanguageUtils.wrapContext(binding.root.context)
            val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.ROOT)
            
            val description = when (item.messageType) {
                "TIME_SET" -> wrappedContext.getString(R.string.time_set_to, item.timeValue)
                "MESSAGE_SENT" -> wrappedContext.getString(R.string.message_sent_desc, item.messageValue)
                else -> ""
            }
            
            binding.historyText.text = buildString {
                append(description)
                append("\n")
                append(wrappedContext.getString(R.string.set_on, dateFormat.format(Date(item.timestamp))))
            }
            
            binding.deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<HistoryEntry>() {
        override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry) = oldItem == newItem
    }
} 