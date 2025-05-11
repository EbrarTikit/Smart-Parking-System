package com.example.smartparkingsystem.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.databinding.FragmentDetailBinding
import com.example.smartparkingsystem.utils.loadImage
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DetailFragment : Fragment(R.layout.fragment_detail) {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()

    private lateinit var parking: ParkingListResponse

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailBinding.bind(view)

        arguments?.getParcelable<ParkingListResponse>("parking")?.let { parking ->
            this.parking = parking
            setupUI()
            setupClickListeners()
            trackView()
            observeViewerCount()
        }
    }

    private fun setupUI() {
        with(binding) {
            ivParkingImage.loadImage(parking.imageUrl)
            tvParkingName.text = parking.name
            tvParkingAddress.text = parking.location
            val availableSpots = parking.capacity - parking.parkingSpots.count { it.occupied }
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
            val gmmIntentUri = Uri.parse("google.navigation:q=${parking.latitude},${parking.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
            }

            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Google Maps is not installed on your device",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.btnSeeParkingSpots.setOnClickListener {
            findNavController().navigate(
                R.id.action_detailFragment_to_parkingLayoutFragment,
                Bundle().apply { putInt("parkingId", parking.id) })
        }
    }

    private fun isCurrentlyOpen(openHours: String, closeHours: String): Boolean {
        try {
            val currentTime = java.time.LocalTime.now()
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

            val openTime = java.time.LocalTime.parse(openHours, formatter)
            val closeTime = java.time.LocalTime.parse(closeHours, formatter)

            return if (closeTime.isAfter(openTime)) {
                currentTime.isAfter(openTime) && currentTime.isBefore(closeTime)
            } else {
                currentTime.isAfter(openTime) || currentTime.isBefore(closeTime)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    private fun observeViewerCount() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.viewerCount.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                    }
                    is UiState.Success -> {
                        binding.chipPeople.text = "${state.data.viewerCount} people"
                    }
                    is UiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun trackView() {
        val userId = getUserIdFromPrefsOrSession()
        viewModel.trackUserView(userId, parking.id)
    }

    private fun getUserIdFromPrefsOrSession(): Int {
        val sharedPreferences =
            requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}