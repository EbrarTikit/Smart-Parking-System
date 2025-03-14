package com.example.smartparkingsystem.di

import com.example.smartparkingsystem.data.remote.ChatbotService
import com.example.smartparkingsystem.data.remote.UserService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Chatbot servisi için Retrofit instance'ı
    @Provides
    @Singleton
    @Named("chatbotRetrofit")
    fun provideChatbotRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8001")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // User servisi için Retrofit instance'ı
    @Provides
    @Singleton
    @Named("userRetrofit")
    fun provideUserRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080") // User service'in port numarasını doğru şekilde ayarlayın
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideChatbotService(@Named("chatbotRetrofit") retrofit: Retrofit): ChatbotService {
        return retrofit.create(ChatbotService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserService(@Named("userRetrofit") retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }
}