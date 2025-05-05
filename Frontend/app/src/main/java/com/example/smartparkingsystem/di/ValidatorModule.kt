package com.example.smartparkingsystem.di

import com.example.smartparkingsystem.utils.validation.EmailValidator
import com.example.smartparkingsystem.utils.validation.AndroidEmailValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ValidatorModule {

    @Binds
    @Singleton
    abstract fun bindEmailValidator(
        androidEmailValidator: AndroidEmailValidator
    ): EmailValidator
}
