package com.example.smartparkingsystem.ui.locationaccess

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentLocationAccessBinding

class LocationAccessFragment : Fragment(R.layout.fragment_location_access) {
    private var _binding: FragmentLocationAccessBinding? = null
    private val binding get() = _binding!!

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                findNavController().navigate(R.id.action_locationAccessFragment_to_homeFragment)
            } else {
                Toast.makeText(requireContext(), "Location permission is required or enter manually!", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationAccessBinding.inflate(inflater,container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()

    }

    fun setupClickListeners() {
        binding.allowLocationButton.setOnClickListener {
            requestLocationPermission()
        }

        binding.enterLocationButton.setOnClickListener {
            findNavController().navigate(R.id.action_locationAccessFragment_to_locationFragment)
        }

    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted()) {
            findNavController().navigate(R.id.action_locationAccessFragment_to_homeFragment)
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}