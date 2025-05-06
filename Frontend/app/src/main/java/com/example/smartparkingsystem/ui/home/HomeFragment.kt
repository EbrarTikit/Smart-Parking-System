package com.example.smartparkingsystem.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.Parking
import com.example.smartparkingsystem.databinding.FragmentHomeBinding
import com.example.smartparkingsystem.utils.state.UiState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private val parkingAdapter = ParkingAdapter()
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val defaultLocation = LatLng(41.0082, 28.9784)
    private val defaultZoom = 13f
    private val userLocationZoom = 15f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupMap()
        setupRecyclerView()
        observeUiState()
        viewModel.fetchParkings()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupRecyclerView() {
        binding.rvParkings.apply {
            adapter = parkingAdapter
            setHasFixedSize(true)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    if (position == state.itemCount - 1) {
                        outRect.right = resources.getDimensionPixelSize(R.dimen.parking_item_margin)
                    }
                }
            })
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.parkings.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val parkingList = state.data
                        parkingAdapter.submitList(parkingList)
                        // Harita markerlarını da burada güncelleyebilirsin:
                        if (::googleMap.isInitialized) {
                            googleMap.clear()
                            parkingList.forEach { parking ->
                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(parking.latitude, parking.longitude))
                                        .title(parking.name)
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setPadding(0, 0, 0, 470)

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }

        moveToUserLocation()
    }

    private fun moveToUserLocation() {
        if (!hasLocationPermission()) {
            moveToLocation(defaultLocation, defaultZoom)
            return
        }

        try {
            googleMap.isMyLocationEnabled = true

            // Kullanıcının son bilinen konumunu al
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        moveToLocation(userLatLng, userLocationZoom)
                    } else {
                        moveToLocation(defaultLocation, defaultZoom)
                    }
                }
                .addOnFailureListener {
                    moveToLocation(defaultLocation, defaultZoom)
                }
        } catch (e: SecurityException) {
            // İzin hatası durumunda
            moveToLocation(defaultLocation, defaultZoom)
        }
    }

    private fun moveToLocation(location: LatLng, zoomLevel: Float) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}