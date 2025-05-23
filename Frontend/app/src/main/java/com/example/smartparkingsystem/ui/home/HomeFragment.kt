package com.example.smartparkingsystem.ui.home

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.AppCompatEditText
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.ParkingListResponse
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

        // Arama barı için listener
        binding.etSearchParking.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterBySearchQuery(s?.toString() ?: "")
            }
        })
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

        // Kamera hareketlerini dinle
        googleMap.setOnCameraMoveListener {
            // Harita hareket ederken loading göster
            binding.progressBar.visibility = View.VISIBLE
        }

        googleMap.setOnCameraIdleListener {
            try {
                // Harita durduğunda görünen bölgeyi al
                val visibleRegion = googleMap.projection.visibleRegion
                
                // Koordinatları doğru sırayla al
                val southwest = LatLng(
                    minOf(visibleRegion.farLeft.latitude, visibleRegion.nearRight.latitude),
                    minOf(visibleRegion.farLeft.longitude, visibleRegion.nearRight.longitude)
                )
                
                val northeast = LatLng(
                    maxOf(visibleRegion.farLeft.latitude, visibleRegion.nearRight.latitude),
                    maxOf(visibleRegion.farLeft.longitude, visibleRegion.nearRight.longitude)
                )
                
                // Bounds'u oluştur
                val bounds = LatLngBounds(southwest, northeast)
                
                // Görünen bölgedeki otoparkları filtrele
                filterParkingsInBounds(bounds)
            } catch (e: Exception) {
                // Hata durumunda tüm otoparkları göster
                val allParkings = viewModel.parkings.value
                if (allParkings is UiState.Success) {
                    parkingAdapter.submitList(allParkings.data)
                    updateMapMarkers(allParkings.data)
                }
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }

        googleMap.setOnMarkerClickListener { marker ->
            
            val markerLatLng = marker.position
            
            val layoutManager = binding.rvParkings.layoutManager as LinearLayoutManager
            val adapter = binding.rvParkings.adapter as ParkingAdapter
            
            // Tıklanan marker'a en yakın otoparkı bul
            var closestParkingIndex = -1
            var minDistance = Double.MAX_VALUE
            
            adapter.getCurrentList().forEachIndexed { index, parking ->
                val parkingLatLng = LatLng(parking.latitude, parking.longitude)
                val distance = calculateDistance(markerLatLng, parkingLatLng)
                
                if (distance < minDistance) {
                    minDistance = distance
                    closestParkingIndex = index
                }
            }

            if (closestParkingIndex != -1) {
                val currentPosition = layoutManager.findFirstVisibleItemPosition()

                val scrollDistance = closestParkingIndex - currentPosition
                binding.rvParkings.smoothScrollToPosition(closestParkingIndex)
                adapter.setSelectedPosition(closestParkingIndex)
            }
            
            true
        }

        moveToUserLocation()
    }

    private fun filterParkingsInBounds(bounds: LatLngBounds) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val allParkings = viewModel.parkings.value
                if (allParkings is UiState.Success) {
                    val filteredParkings = allParkings.data.filter { parking ->
                        val parkingLatLng = LatLng(parking.latitude, parking.longitude)
                        bounds.contains(parkingLatLng)
                    }

                    parkingAdapter.submitList(filteredParkings)

                    updateMapMarkers(filteredParkings)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error filtering parkings: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateMapMarkers(parkings: List<ParkingListResponse>) {
        // Mevcut markerları temizle
        googleMap.clear()

        // Yeni markerları ekle
        parkings.forEach { parking ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(parking.latitude, parking.longitude))
                    .title(parking.name)
            )
        }
    }

    // İki nokta arasındaki mesafeyi hesapla
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0].toDouble()
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

    private fun filterBySearchQuery(query: String) {
        val allParkings = viewModel.parkings.value
        if (allParkings is UiState.Success) {
            val filtered = if (query.isBlank()) {
                allParkings.data
            } else {
                allParkings.data.filter { it.name.contains(query, ignoreCase = true) }
            }
            parkingAdapter.submitList(filtered)
            updateMapMarkers(filtered)

            if (filtered.isNotEmpty()) {
                if (filtered.size == 1) {
                    val parking = filtered.first()
                    val latLng = LatLng(parking.latitude, parking.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                } else {
                    // Birden fazla otopark varsa hepsini kapsayacak şekilde haritayı ayarla
                    val builder = LatLngBounds.Builder()
                    filtered.forEach { parking ->
                        builder.include(LatLng(parking.latitude, parking.longitude))
                    }
                    val bounds = builder.build()
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}