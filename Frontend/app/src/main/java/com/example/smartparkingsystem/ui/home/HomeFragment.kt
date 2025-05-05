package com.example.smartparkingsystem.ui.home

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.Parking
import com.example.smartparkingsystem.databinding.FragmentHomeBinding
import com.example.smartparkingsystem.utils.state.UiState
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        setupMap()
        setupRecyclerView()
        observeUiState()
        viewModel.fetchLocations()
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
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }

                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        val locations = state.data

                        if (::googleMap.isInitialized) {
                            googleMap.clear()
                            locations.forEach { loc ->
                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(loc.latitude, loc.longitude))
                                        .title(loc.name)
                                )
                            }
                        }
                        val parkingList = locations.map {
                            Parking(
                                id = it.id.toString(),
                                name = it.name,
                                image = R.drawable.img,
                                price = 0.0,
                                availableSpots = 0,
                                totalSpots = 0,
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                        }
                        parkingAdapter.submitList(parkingList)
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
        val istanbul = LatLng(41.0082, 28.9784)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(istanbul, 13f))

        googleMap.setPadding(0, 0, 0, 470)

        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}