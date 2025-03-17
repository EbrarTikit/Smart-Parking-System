package com.example.smartparkingsystem.utils

import com.example.smartparkingsystem.data.model.SignUpResponse
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class StringConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return if (type == SignUpResponse::class.java) {
            Converter<ResponseBody, SignUpResponse> { responseBody ->
                val message = responseBody.string()
                SignUpResponse(message)
            }
        } else {
            null
        }
    }
} 