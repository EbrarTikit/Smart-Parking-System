package com.example.smartparkingsystem.ui

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.navOptions
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.repository.NotificationRepository
import com.example.smartparkingsystem.databinding.ActivityMainBinding
import com.example.smartparkingsystem.utils.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var notificationRepository: NotificationRepository

    companion object {
        private const val TAG = "MainActivity"
    }

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

        // FCM token'ı al
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Token'ı al
                val token = task.result
                Log.d(TAG, "FCM Token: $token")

                // Token'ı backend'e gönder
                sendRegistrationToServer(token)
            }

        // Topic aboneliği
        FirebaseMessaging.getInstance().subscribeToTopic("parking_24")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: parking_24")
                } else {
                    Log.e(TAG, "Failed to subscribe to topic", task.exception)
                }
            }

        checkUserLoginStatus()
        setupNavigation()
    }

    private fun sendRegistrationToServer(token: String) {
        val userId = sessionManager.getUserId()
        if (userId > 0) {
            val deviceId = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = notificationRepository.registerFcmToken(userId.toInt(), token, deviceId)
                    result.fold(
                        onSuccess = {
                            Log.d(TAG, "FCM token registered successfully")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to register FCM token", error)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while registering FCM token", e)
                }
            }
        } else {
            Log.e(TAG, "Cannot register FCM token: Invalid userId ($userId)")
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.fade_out)
            .build()

        binding.bottomNavigationView.apply {
            setupWithNavController(navController)
            
            setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.navigation_home -> {
                        navController.navigate(R.id.navigation_home, null, navOptions)
                        true
                    }
                    R.id.navigation_booking -> {
                        navController.navigate(R.id.navigation_booking, null, navOptions)
                        true
                    }
                    R.id.navigation_chatbot -> {
                        navController.navigate(R.id.navigation_chatbot, null, navOptions)
                        true
                    }
                    R.id.navigation_favorites -> {
                        navController.navigate(R.id.navigation_favorites, null, navOptions)
                        true
                    }
                    R.id.navigation_profile -> {
                        navController.navigate(R.id.navigation_profile, null, navOptions)
                        true
                    }
                    else -> false
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            clearCurrentFragment()
            
            val hideBottomNav = when (destination.id) {
                R.id.splashFragment,
                R.id.onboardingFragment,
                R.id.authFragment,
                R.id.signInFragment,
                R.id.signUpFragment,
                R.id.detailFragment,
                R.id.locationAccessFragment,
                R.id.locationFragment -> true
                else -> false
            }
            
            binding.bottomNavigationView.visibility = if (hideBottomNav) View.GONE else View.VISIBLE
            
            binding.bottomNavigationView.menu.findItem(destination.id)?.isChecked = true
        }
    }

    private fun clearCurrentFragment() {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment !is NavHostFragment) {
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commitNow()
            }
        }
    }

    private fun checkUserLoginStatus() {
        val userId = sessionManager.getUserId()
        val isLoggedIn = sessionManager.isLoggedIn()
        Log.d("MainActivity", "Application started with userId: $userId, isLoggedIn: $isLoggedIn")

        if (userId <= 0 || !isLoggedIn) {
            Log.d("MainActivity", "No valid userId found in SessionManager")
        } else {
            Log.d("MainActivity", "User is logged in with userId: $userId")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}