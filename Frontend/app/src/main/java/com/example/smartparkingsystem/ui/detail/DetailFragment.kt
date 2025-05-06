package com.example.smartparkingsystem.ui.detail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.databinding.FragmentDetailBinding
import com.example.smartparkingsystem.utils.loadImage

class DetailFragment : Fragment(R.layout.fragment_detail) {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var parking: ParkingListResponse

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailBinding.bind(view)

        arguments?.getParcelable<ParkingListResponse>("parking")?.let { parking ->
            this.parking = parking
            setupUI()
            setupClickListeners()
        }
    }

    private fun setupUI() {
        with(binding) {
            ivParkingImage.loadImage(parking.imageUrl)
            tvParkingName.text = parking.name
            tvParkingAddress.text = parking.location
            val availableSpots = parking.capacity - parking.parkingSpots.count { it.occupied }
            chipPeople.text = "${parking.parkingSpots.count { it.occupied }} people"
            chipSpots.text = "$availableSpots spots"
            chipTime.text = "${parking.openingHours}-${parking.closingHours}"

            val isOpen = isCurrentlyOpen(parking.openingHours, parking.closingHours)
            chipStatus.text = if (isOpen) "Open" else "Closed"

            tvPrice.text = "₺${parking.rate}"
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {

        }

        binding.btnSeeLocation.setOnClickListener {

        }

        binding.btnSeeParkingSpots.setOnClickListener {
        }
    }

    private fun isCurrentlyOpen(openHours: String, closeHours: String): Boolean {
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}