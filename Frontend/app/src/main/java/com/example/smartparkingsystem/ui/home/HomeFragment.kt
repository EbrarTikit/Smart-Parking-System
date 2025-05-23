package com.example.smartparkingsystem.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentHomeBinding
import com.example.smartparkingsystem.utils.SessionManager
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
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleMap: GoogleMap
    private lateinit var parkingAdapter: ParkingAdapter
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val defaultLocation = LatLng(41.0082, 28.9784)
    private val defaultZoom = 13f
    private val userLocationZoom = 15f

    @Inject
    lateinit var sessionManager: SessionManager

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            updateNotificationPreferences(true)
        } else {
            updateNotificationPreferences(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupAdapter()
        setupMap()
        setupRecyclerView()
        observeUiState()
        viewModel.fetchParkings()
        checkNotificationPermissions()
    }

    private fun setupAdapter() {
        parkingAdapter = ParkingAdapter { parking ->
            findNavController().navigate(
                R.id.action_navigation_home_to_detailFragment,
                Bundle().apply { putParcelable("parking", parking) })
        }
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

                        // Kullanıcı konumunu al ve sıralama yap
                        if (hasLocationPermission()) {
                            try {
                                fusedLocationClient.lastLocation
                                    .addOnSuccessListener { location ->
                                        if (location != null) {
                                            val userLatLng =
                                                LatLng(location.latitude, location.longitude)
                                            val sortedList = parkingList.sortedBy { parking ->
                                                distanceBetween(
                                                    userLatLng.latitude, userLatLng.longitude,
                                                    parking.latitude, parking.longitude
                                                )
                                            }
                                            parkingAdapter.submitList(sortedList)
                                        } else {
                                            // Konum alınamazsa, gelen sırayla göster
                                            parkingAdapter.submitList(parkingList)
                                        }
                                    }
                                    .addOnFailureListener {
                                        parkingAdapter.submitList(parkingList)
                                    }
                            } catch (e: SecurityException) {
                                // İzin yoksa, gelen sırayla göster
                                parkingAdapter.submitList(parkingList)
                            }
                        } else {
                            parkingAdapter.submitList(parkingList)
                        }

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

    private fun distanceBetween(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double
    ): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
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

    private fun checkNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // İzin zaten verilmiş, tercihleri güncelle
                    updateNotificationPreferences(true)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Kullanıcıya neden izin istediğimizi açıkla
                    showNotificationPermissionRationale()
                }
                else -> {
                    // İzin iste
                    requestNotificationPermission()
                }
            }
        }
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Bildirim İzni")
            .setMessage("Otopark doluluk durumu hakkında bildirim almak için bildirim iznine ihtiyacımız var.")
            .setPositiveButton("İzin Ver") { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
                updateNotificationPreferences(false)
            }
            .show()
    }

    private fun requestNotificationPermission() {
        notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun updateNotificationPreferences(isEnabled: Boolean) {
        val userId = sessionManager.getUserId()
        if (userId > 0) {
            viewModel.updateNotificationPreferences(userId.toInt(), isEnabled)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}