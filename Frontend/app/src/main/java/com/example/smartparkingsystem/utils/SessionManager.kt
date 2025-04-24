package com.example.smartparkingsystem.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.smartparkingsystem.utils.Constants.KEY_IS_LOGGED_IN
import com.example.smartparkingsystem.utils.Constants.KEY_TOKEN
import com.example.smartparkingsystem.utils.Constants.KEY_USER_ID
import com.example.smartparkingsystem.utils.Constants.PREF_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserSession(userId: Long, token: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_TOKEN, token)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_TOKEN)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

}