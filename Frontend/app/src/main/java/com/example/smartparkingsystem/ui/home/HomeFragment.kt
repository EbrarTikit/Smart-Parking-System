package com.example.smartparkingsystem.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.smartparkingsystem.data.model.Parking
import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private val parkingAdapter = ParkingAdapter()

    private val sampleParkings = listOf(
        Parking(
            id = "1",
            name = "ParkSecure",
            image = R.drawable.img,
            price = 5.0,
            availableSpots = 25,
            totalSpots = 50,
            latitude = 41.0082,
            longitude = 28.9784
        ),
        Parking(
            id = "2",
            name = "SpacePark",
            image = R.drawable.img,
            price = 6.0,
            availableSpots = 15,
            totalSpots = 30,
            latitude = 41.0122,
            longitude = 28.9760
        ),
        Parking(
            id = "3",
            name = "SmartPark",
            image = R.drawable.img,
            price = 4.5,
            availableSpots = 8,
            totalSpots = 40,
            latitude = 41.0160,
            longitude = 28.9795
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        
        setupMap()
        setupRecyclerView()
        loadParkings()
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

    private fun loadParkings() {
        parkingAdapter.submitList(sampleParkings)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // İstanbul
        val istanbul = LatLng(41.0082, 28.9784)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(istanbul, 13f))

        // Otoparkları haritaya ekle
        sampleParkings.forEach { parking ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(parking.latitude, parking.longitude))
                    .title(parking.name)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}