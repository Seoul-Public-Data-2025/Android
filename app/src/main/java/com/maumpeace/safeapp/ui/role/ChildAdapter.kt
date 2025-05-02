package com.maumpeace.safeapp.ui.role

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.maumpeace.safeapp.databinding.ItemChildBinding
import com.maumpeace.safeapp.model.RelationChildInfoData

class ChildAdapter(
    private val onAccept: (RelationChildInfoData) -> Unit,
    private val onReject: (RelationChildInfoData) -> Unit
) : ListAdapter<RelationChildInfoData, ChildAdapter.ChildViewHolder>(DiffCallback) {

    inner class ChildViewHolder(private val binding: ItemChildBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RelationChildInfoData) {
            binding.tvName.text = item.name
            binding.tvPhone.text = formatPhoneNumber(item.phone)
            binding.tvRole.text = if (item.role == "parent") "보호자" else "자녀"
            binding.btnContainer.visibility = if (!item.isApproved) View.VISIBLE else View.GONE

            binding.btnAccept.setOnClickListener {
                onAccept(item)
                binding.btnContainer.visibility = View.GONE
            }

            binding.btnReject.setOnClickListener {
                onReject(item)
                binding.btnContainer.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val binding =
            ItemChildBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChildViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<RelationChildInfoData>() {
            override fun areItemsTheSame(
                oldItem: RelationChildInfoData,
                newItem: RelationChildInfoData
            ) = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: RelationChildInfoData,
                newItem: RelationChildInfoData
            ) = oldItem == newItem
        }

        private fun formatPhoneNumber(phone: String): String {
            val clean = phone.replace("\\s".toRegex(), "")
                .replace("-", "")
                .replace("+82", "0")
                .filter { it.isDigit() }

            return when (clean.length) {
                10 -> clean.replaceFirst("(\\d{3})(\\d{3})(\\d{4})".toRegex(), "$1-$2-$3")
                11 -> clean.replaceFirst("(\\d{3})(\\d{4})(\\d{4})".toRegex(), "$1-$2-$3")
                else -> phone
            }
        }
    }
}