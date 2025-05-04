package com.example.smartparkingsystem.utils.validation

import android.util.Patterns
import javax.inject.Inject

class AndroidEmailValidator @Inject constructor() : EmailValidator {
    override fun isValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}