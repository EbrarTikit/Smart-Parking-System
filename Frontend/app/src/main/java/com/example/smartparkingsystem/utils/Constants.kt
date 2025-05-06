package com.example.smartparkingsystem.utils

object Constants {
    //Endpoints
    const val CHAT = "/api/v1/chat"
    const val HISTORY = "/api/v1/chat/{session_id}/history"
    const val SIGNUP = "/api/auth/signup"
    const val SIGNIN = "/api/auth/signin"
    const val NAVIGATION = "rest/api/car_park/parking-location/{id}"
    const val NAVIGATION_LIST = "rest/api/car_park/parking-location/list"
    const val PARKING_LIST = "api/parkings"
    const val PARKING_DETAILS = "api/parkings/{parkingId}/layout"
    const val VIEWER_TRACK = "/api/parking-viewers/track"


    //Session Manager
    const val PREF_NAME = "smart_parking_session"
    const val KEY_USER_ID = "user_id"
    const val KEY_TOKEN = "token"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
}