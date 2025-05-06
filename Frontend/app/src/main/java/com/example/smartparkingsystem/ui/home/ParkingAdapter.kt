package com.example.smartparkingsystem.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.data.model.Parking
import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.databinding.ItemParkingBinding

class ParkingAdapter : RecyclerView.Adapter<ParkingAdapter.ParkingViewHolder>() {

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
            )
        )
    }

    override fun onBindViewHolder(holder: ParkingViewHolder, position: Int) {
        holder.bind(parkings[position])
    }

    override fun getItemCount() = parkings.size

    class ParkingViewHolder(
        private val binding: ItemParkingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(parking: ParkingListResponse) {
            binding.apply {
                parkingImage.setImageResource(com.example.smartparkingsystem.R.drawable.img) // image url backend'den gelirse Glide ile yükleyebilirsin
                parkingName.text = parking.name
                priceText.text = "₺${parking.rate}/hr"
                val availableSpotsCount = parking.capacity - parking.parkingSpots.count { it.occupied }
                availableSpots.text = "$availableSpotsCount spots available"
            }
        }
    }
}