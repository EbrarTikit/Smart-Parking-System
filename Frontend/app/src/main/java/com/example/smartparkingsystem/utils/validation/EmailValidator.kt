package com.example.smartparkingsystem.utils.validation

interface EmailValidator {
    fun isValid(email: String): Boolean
}