package com.example.smartparkingsystem.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartparkingsystem.data.model.OnboardingPage
import com.example.smartparkingsystem.databinding.ItemOnboardingPageBinding

class OnboardingPagerAdapter(
    private val onboardingPages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingPageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingPageViewHolder {
        return OnboardingPageViewHolder(
            ItemOnboardingPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingPageViewHolder, position: Int) {
        holder.bind(onboardingPages[position])
    }

    override fun getItemCount() = onboardingPages.size

    class OnboardingPageViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(onboardingPage: OnboardingPage) {
            binding.apply {
                animationView.setAnimation(onboardingPage.animation)
                titleText.text = onboardingPage.title
                descriptionText.text = onboardingPage.description
            }
        }
    }
} 