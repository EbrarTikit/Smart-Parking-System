package com.example.smartparkingsystem.ui.locationaccess

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentLocationAccessBinding

class LocationAccessFragment : Fragment(R.layout.fragment_location_access) {
    private var _binding: FragmentLocationAccessBinding? = null
    private val binding get() = _binding!!

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
            findNavController().navigate(R.id.action_locationAccessFragment_to_homeFragment)
        }

        binding.enterLocationButton.setOnClickListener {
            findNavController().navigate(R.id.action_locationAccessFragment_to_locationFragment)
        }

    }
}