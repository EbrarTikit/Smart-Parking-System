package com.example.smartparkingsystem.data.repository

import android.app.Application
import com.example.smartparkingsystem.data.remote.ApiInterface
import com.example.smartparkingsystem.domain.AppRepository
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val apiInterface: ApiInterface,
    private val appContext: Application
) : AppRepository {
}