package com.example.smartparkingsystem.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.data.model.Parking
import com.example.smartparkingsystem.databinding.ItemParkingBinding

class ParkingAdapter : RecyclerView.Adapter<ParkingAdapter.ParkingViewHolder>() {

    private val parkings = mutableListOf<Parking>()

    fun submitList(newParkings: List<Parking>) {
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

        fun bind(parking: Parking) {
            binding.apply {
                parkingImage.setImageResource(parking.image)
                parkingName.text = parking.name
                priceText.text = "₺${parking.price}/hr"
                availableSpots.text = "${parking.availableSpots} spots available"
            }
        }
    }
}