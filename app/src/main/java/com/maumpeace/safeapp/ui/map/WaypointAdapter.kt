package com.maumpeace.safeapp.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.maumpeace.safeapp.databinding.ItemWaypointBinding
import com.maumpeace.safeapp.model.MapMarkerInfoData

class WaypointAdapter(
    private val items: List<MapMarkerInfoData>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<WaypointAdapter.WaypointViewHolder>() {

    inner class WaypointViewHolder(private val binding: ItemWaypointBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MapMarkerInfoData) {
            binding.textWaypointName.text = item.address ?: "."
            binding.btnRemoveWaypoint.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onRemoveClick(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaypointViewHolder {
        val binding = ItemWaypointBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WaypointViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WaypointViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}