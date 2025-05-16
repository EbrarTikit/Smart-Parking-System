package com.example.smartparkingsystem.ui.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.FragmentProfileBinding
import com.example.smartparkingsystem.utils.SessionManager
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import com.example.smartparkingsystem.utils.state.UiState
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        val userId = SessionManager(requireContext()).getUserId().toInt()
        if (userId > 0) {
            viewModel.getNotificationPreferences(userId)
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (userId > 0) {
                viewModel.setNotificationPreferences(userId, isChecked)
            }
            updateSwitchColors(isChecked)
        }

        lifecycleScope.launch {
            viewModel.notificationState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> {  }
                    is UiState.Success -> {
                        binding.switchNotifications.isChecked = state.data.parkingFullNotification
                    }
                    is UiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            findNavController().navigate(
                R.id.action_navigation_profile_to_signInFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        }
    }

    private fun updateSwitchColors(isChecked: Boolean) {
        val thumbColor = if (isChecked) R.color.switch_thumb_enabled else R.color.switch_thumb_disabled
        val trackColor = if (isChecked) R.color.switch_track_enabled else R.color.switch_track_disabled
        binding.switchNotifications.thumbTintList = ContextCompat.getColorStateList(requireContext(), thumbColor)
        binding.switchNotifications.trackTintList = ContextCompat.getColorStateList(requireContext(), trackColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}