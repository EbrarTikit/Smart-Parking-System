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

    fun submitList(newParkings: List<ParkingListResponse>) {
        parkings.clear()
        parkings.addAll(newParkings)
        notifyDataSetChanged()
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
        holder.bind(parkings[position])
    }

    override fun getItemCount() = parkings.size

    class ParkingViewHolder(
        private val binding: ItemParkingBinding,
        private val onItemClick: (ParkingListResponse) -> Unit = {}
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(parking: ParkingListResponse) {
            binding.apply {
                parkingImage.loadImage(parking.imageUrl)
                parkingName.text = parking.name
                priceText.text = "₺${parking.rate}/hr"
                val availableSpotsCount = parking.capacity - parking.parkingSpots.count { it.occupied }
                availableSpots.text = "$availableSpotsCount spots available"
            }

            binding.root.setOnClickListener {
                onItemClick(parking)
            }
        }
    }
}