package com.maumpeace.safeapp.ui.role

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maumpeace.safeapp.databinding.ItemContactBinding
import com.maumpeace.safeapp.util.PhoneFormatter

/**
 * 연락처 목록을 표시하는 RecyclerView Adapter
 * 각 아이템은 이름, 전화번호, 등록 버튼을 포함한다.
 */
class ContactAdapter(
    private val onClick: (String, String) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private val items = mutableListOf<Pair<String, String>>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<Pair<String, String>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // diffUtil 적용 가능
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(name: String, phone: String) {
            binding.tvName.text = name
            binding.tvPhone.text = PhoneFormatter.format(phone)

            binding.btnRegister.setOnClickListener {
                val rawPhone = PhoneFormatter.unformat(phone)
                onClick(name, rawPhone)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val (name, phone) = items[position]
        holder.bind(name, phone)
    }

    override fun getItemCount(): Int = items.size
}