package com.example.smartparkingsystem.di

import com.example.smartparkingsystem.data.remote.ChatbotService
import com.example.smartparkingsystem.data.remote.UserService
import com.example.smartparkingsystem.utils.StringConverterFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideStringConverterFactory(): StringConverterFactory {
        return StringConverterFactory()
    }

    // Chatbot servisi için Retrofit instance'ı
    @Provides
    @Singleton
    @Named("chatbotRetrofit")
    fun provideChatbotRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8001")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // User servisi için Retrofit instance'ı
    @Provides
    @Singleton
    @Named("userRetrofit")
    fun provideUserRetrofit(
        okHttpClient: OkHttpClient, 
        gson: Gson,
        stringConverterFactory: StringConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080")
            .client(okHttpClient)
            .addConverterFactory(stringConverterFactory)
            .addConverterFactory(GsonConverterFactory.create(gson))
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