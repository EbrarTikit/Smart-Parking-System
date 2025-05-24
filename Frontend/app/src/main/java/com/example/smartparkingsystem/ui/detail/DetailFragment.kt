package com.example.smartparkingsystem.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.example.smartparkingsystem.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class DetailFragment : Fragment(R.layout.fragment_detail) {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var parking: ParkingListResponse

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailBinding.bind(view)

        val userId = sessionManager.getUserId()
        val isLoggedIn = sessionManager.isLoggedIn()
        val token = sessionManager.getToken()
        Log.d(
            "DetailFragment",
            "Session Info - userId: $userId, isLoggedIn: $isLoggedIn, token: $token"
        )

        arguments?.getParcelable<ParkingListResponse>("parking")?.let { parking ->
            this.parking = parking
            setupUI()
            setupClickListeners()
            trackView()
            observeViewerCount()
            checkFavoriteStatus()
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
            tvDescription.text = parking.description
            tvPrice.text = "₺${parking.rate}"
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            val userId = getUserIdFromPrefsOrSession()
            Log.d("DetailFragment", "Favorite button clicked, userId=$userId, parkingId=${parking.id}")
            viewModel.toggleFavorite(userId, parking.id)
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
                        Log.e("DetailFragment", "Error fetching viewer count: ${state.message}")
                        //Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun checkFavoriteStatus() {
        val userId = getUserIdFromPrefsOrSession()
        viewModel.checkIfFavorite(userId, parking.id)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.favoriteState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.btnFavorite.isEnabled = false
                    }
                    is UiState.Success -> {
                        binding.btnFavorite.isEnabled = true
                        updateFavoriteIcon(state.data)
                    }
                    is UiState.Error -> {
                        binding.btnFavorite.isEnabled = true
                        Log.e("DetailFragment", "Error checking favorite status: ${state.message}")
                        //Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        Log.d("DetailFragment", "Updating favorite icon, isFavorite=$isFavorite")
        binding.btnFavorite.setImageResource(
            if (isFavorite) R.drawable.favorite
            else R.drawable.ic_favorite_unfilled
        )
    }

    private fun trackView() {
        val userId = getUserIdFromPrefsOrSession()
        viewModel.trackUserView(userId, parking.id)
    }

    private fun getUserIdFromPrefsOrSession(): Int {
        val userId = sessionManager.getUserId()
        Log.d("DetailFragment", "Retrieved userId from SessionManager: $userId")
        if (userId <= 0) {
            Toast.makeText(
                requireContext(),
                "Oturum bilgisi bulunamadı. Lütfen tekrar giriş yapın.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Log.d("DetailFragment", "Using userId: $userId")
        }
        return userId.toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}