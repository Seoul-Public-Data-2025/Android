package com.maumpeace.safeapp.ui.role

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maumpeace.safeapp.R
import com.maumpeace.safeapp.databinding.ItemGuardianBinding
import com.maumpeace.safeapp.model.RelationGuardianInfoData

/**
 * 보호자 목록을 표시하는 RecyclerView Adapter
 */
class GuardianAdapter(
    private val onDeleteClick: (RelationGuardianInfoData) -> Unit,
    private val onResendClick: (Int, String) -> Unit
) : RecyclerView.Adapter<GuardianAdapter.GuardianViewHolder>() {

    private val guardianList = mutableListOf<RelationGuardianInfoData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuardianViewHolder {
        val binding =
            ItemGuardianBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GuardianViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GuardianViewHolder, position: Int) {
        holder.bind(guardianList[position])
    }

    override fun getItemCount(): Int = guardianList.size

    fun submitList(list: List<RelationGuardianInfoData>) {
        guardianList.clear()
        guardianList.addAll(list)
        notifyDataSetChanged()
    }

    fun removeItem(item: RelationGuardianInfoData): Boolean {
        val index = guardianList.indexOf(item)
        return if (index != -1) {
            guardianList.removeAt(index)
            notifyItemRemoved(index)
            guardianList.isEmpty()
        } else false
    }

    inner class GuardianViewHolder(private val binding: ItemGuardianBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: RelationGuardianInfoData) {
            binding.tvName.text = data.name
            binding.tvPhone.text = data.phone

            binding.tvRelationResend.apply {
                if (data.isApproved) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        onResendClick(data.id, data.name)
                    }
                }
            }

            binding.tvApproveState.apply {
                if (data.isApproved) {
                    text = "해지 요청"
                    setBackgroundResource(R.drawable.rd_7dc2fb_8)
                    setTextColor(binding.root.context.getColor(R.color.blue_7dc2fb))
                    setOnClickListener {
                        onDeleteClick(data)
                    }
                } else {
                    text = "미등록"
                    setTextColor(binding.root.context.getColor(R.color.orange_ffaa62))
                    setOnClickListener(null) // 클릭 방지
                }
            }
        }
    }
}