package com.example.smartparkingsystem.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.FavoriteParking
import com.example.smartparkingsystem.databinding.ItemFavoriteParkingBinding
import com.example.smartparkingsystem.utils.loadImage

class FavoriteParkingAdapter(
    private val onItemClick: (FavoriteParking) -> Unit,
    private val onFavoriteClick: (FavoriteParking) -> Unit
) : ListAdapter<FavoriteParking, FavoriteParkingAdapter.FavoriteViewHolder>(FavoriteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val binding = ItemFavoriteParkingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FavoriteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavoriteViewHolder(private val binding: ItemFavoriteParkingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnFavorite.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFavoriteClick(getItem(position))
                }
            }
        }

        fun bind(parking: FavoriteParking) {
            with(binding) {
                tvParkingName.text = parking.name
                tvParkingLocation.text = parking.location
                tvParkingPrice.text =
                    "$10/h"
                ivParkingImage.loadImage(parking.imageUrl)
                btnFavorite.setImageResource(R.drawable.favorite)
            }
        }
    }

    class FavoriteDiffCallback : DiffUtil.ItemCallback<FavoriteParking>() {
        override fun areItemsTheSame(oldItem: FavoriteParking, newItem: FavoriteParking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: FavoriteParking,
            newItem: FavoriteParking
        ): Boolean {
            return oldItem == newItem
        }
    }
}
