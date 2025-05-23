package com.example.smartparkingsystem.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.OnboardingPage
import com.example.smartparkingsystem.databinding.FragmentOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import android.content.res.Resources
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val onboardingPages = listOf(
        OnboardingPage(
            R.raw.realtime_parking,
            "Real-time Parking Status",
            "Find available parking spots instantly with our real-time monitoring system"
        ),
        OnboardingPage(
            R.raw.smart_navigation,
            "Smart Navigation",
            "Get guided to the nearest available parking spot with turn-by-turn directions"
        ),
        OnboardingPage(
            R.raw.ai_assistant,
            "AI-Powered Assistant",
            "Get 24/7 support with our intelligent chatbot for all your parking needs and queries"
        )
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)
        
        setupOnboarding()
        setupPageIndicators()
        setupClickListeners()
    }

    private fun setupOnboarding() {
        val adapter = OnboardingPagerAdapter(onboardingPages)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.nextButton.text = if (position == onboardingPages.size - 1) {
                    "Get Started"
                } else {
                    "Next"
                }
            }
        })
    }

    private fun setupPageIndicators() {
        val indicators = listOf(binding.indicator1, binding.indicator2, binding.indicator3)
        
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                indicators.forEachIndexed { index, view ->
                    view.layoutParams.width = if (index == position) 8.dpToPx() else 6.dpToPx()
                    view.layoutParams.height = if (index == position) 8.dpToPx() else 6.dpToPx()
                    view.requestLayout()
                    view.background = if (index == position) 
                        ContextCompat.getDrawable(requireContext(), R.drawable.indicator_active)
                    else 
                        ContextCompat.getDrawable(requireContext(), R.drawable.indicator_inactive)
                }
            }
        })
    }

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun setupClickListeners() {
        binding.skipButton.setOnClickListener {
            navigateToAuth()
        }

        binding.nextButton.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem == onboardingPages.size - 1) {
                navigateToAuth()
            } else {
                binding.viewPager.currentItem = currentItem + 1
            }
        }
    }

    private fun navigateToAuth() {
        findNavController().navigate(R.id.action_onboardingFragment_to_authFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}