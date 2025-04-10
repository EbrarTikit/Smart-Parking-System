package com.example.smartparkingsystem.utils

object Constants {
    //Endpoints
    const val CHAT = "/api/v1/chat"
    const val HISTORY = "/api/v1/chat/{session_id}/history"
    const val SIGNUP = "/api/auth/signup"
    const val SIGNIN = "/api/auth/signin"

    //Session Manager
    const val PREF_NAME = "smart_parking_session"
    const val KEY_USER_ID = "user_id"
    const val KEY_TOKEN = "token"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
}