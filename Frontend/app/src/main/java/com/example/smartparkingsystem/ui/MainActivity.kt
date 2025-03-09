package com.example.smartparkingsystem.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationView.apply {
            setupWithNavController(navController)
            
            setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.navigation_home -> {
                        navController.navigate(R.id.navigation_home)
                        true
                    }
                    R.id.navigation_booking -> {
                        navController.navigate(R.id.navigation_booking)
                        true
                    }
                    R.id.navigation_chatbot -> {
                        navController.navigate(R.id.navigation_chatbot)
                        true
                    }
                    R.id.navigation_favorites -> {
                        navController.navigate(R.id.navigation_favorites)
                        true
                    }
                    R.id.navigation_profile -> {
                        navController.navigate(R.id.navigation_profile)
                        true
                    }
                    else -> false
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideBottomNav = when (destination.id) {
                R.id.splashFragment,
                R.id.onboardingFragment,
                R.id.authFragment,
                R.id.signInFragment,
                R.id.signUpFragment,
                R.id.locationAccessFragment,
                R.id.locationFragment -> true
                else -> false
            }
            
            binding.bottomNavigationView.visibility = if (hideBottomNav) View.GONE else View.VISIBLE
            
            binding.bottomNavigationView.menu.findItem(destination.id)?.isChecked = true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}