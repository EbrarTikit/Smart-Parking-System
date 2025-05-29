package com.example.smartparkingsystem.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.smartparkingsystem.utils.Constants.KEY_EMAIL
import com.example.smartparkingsystem.utils.Constants.KEY_IS_LOGGED_IN
import com.example.smartparkingsystem.utils.Constants.KEY_TOKEN
import com.example.smartparkingsystem.utils.Constants.KEY_USER_ID
import com.example.smartparkingsystem.utils.Constants.KEY_USERNAME
import com.example.smartparkingsystem.utils.Constants.PREF_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserSession(userId: Long, token: String, email: String, username: String) {
        Log.d("SessionManager", "Saving session - userId: $userId, token: $token, email: $email, username: $username")
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .putString(KEY_USERNAME, username)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()

        // Verify save worked
        val savedId = prefs.getLong(KEY_USER_ID, -1)
        val savedToken = prefs.getString(KEY_TOKEN, null)
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        val savedUsername = prefs.getString(KEY_USERNAME, null)
        val savedLogin = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d(
            "SessionManager",
            "Verification after save - userId: $savedId, token: $savedToken, email: $savedEmail, username: $savedUsername, isLoggedIn: $savedLogin"
        )
    }

    fun getUserId(): Long {
        val userId = prefs.getLong(KEY_USER_ID, -1)
        Log.d("SessionManager", "Getting userId: $userId")
        return userId
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        Log.d("SessionManager", "Getting token: $token")
        return token
    }

    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d("SessionManager", "Getting isLoggedIn: $isLoggedIn")
        return isLoggedIn
    }

    fun getEmail(): String? {
        val email = prefs.getString(KEY_EMAIL, null)
        Log.d("SessionManager", "Getting email: $email")
        return email
    }

    fun getUsername(): String? {
        val username = prefs.getString(KEY_USERNAME, null)
        Log.d("SessionManager", "Getting username: $username")
        return username
    }

    fun clearSession() {
        Log.d("SessionManager", "Clearing session")
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .remove(KEY_USERNAME)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
        val savedId = prefs.getLong(KEY_USER_ID, -1)
        val savedToken = prefs.getString(KEY_TOKEN, null)
        val savedEmail = prefs.getString(KEY_EMAIL, null)
        val savedUsername = prefs.getString(KEY_USERNAME, null)
        val savedLogin = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d(
            "SessionManager",
            "Verification after clear - userId: $savedId, token: $savedToken, email: $savedEmail, username: $savedUsername, isLoggedIn: $savedLogin"
        )
    }
}