package com.example.smartparkingsystem.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.data.model.Parking
import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.databinding.ItemParkingBinding
import com.example.smartparkingsystem.utils.loadImage

class ParkingAdapter(
    private val onItemClick: (ParkingListResponse) -> Unit
) : RecyclerView.Adapter<ParkingAdapter.ParkingViewHolder>() {

    private val parkings = mutableListOf<ParkingListResponse>()
    private var selectedPosition = -1

    fun submitList(newParkings: List<ParkingListResponse>) {
        parkings.clear()
        parkings.addAll(newParkings)
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<ParkingListResponse> = parkings.toList()

    fun setSelectedPosition(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        if (oldPosition != -1) notifyItemChanged(oldPosition)
        if (position != -1) notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
        return ParkingViewHolder(
            ItemParkingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onItemClick
        )
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        holder.bind(parkings[position], position == selectedPosition)
    }

    override fun getItemCount() = parkings.size

    class ParkingViewHolder(
        private val binding: ItemParkingBinding,
        private val onItemClick: (ParkingListResponse) -> Unit = {}
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(parking: ParkingListResponse, isSelected: Boolean) {
            binding.apply {
                parkingImage.loadImage(parking.imageUrl)
                parkingName.text = parking.name
                priceText.text = "₺${parking.rate}/hr"
                val availableSpotsCount = parking.capacity - parking.parkingSpots.count { it.occupied }
                availableSpots.text = "$availableSpotsCount spots".uppercase()

                // Seçili duruma göre kartın görünümünü güncelle
                root.alpha = if (isSelected) 1f else 0.8f
                root.scaleX = if (isSelected) 1.05f else 1f
                root.scaleY = if (isSelected) 1.05f else 1f
                root.elevation = if (isSelected) 12f else 8f
            }

            binding.root.setOnClickListener {
                onItemClick(parking)
            }
        }
    }
}