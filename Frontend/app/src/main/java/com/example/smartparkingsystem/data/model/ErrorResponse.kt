package com.example.smartparkingsystem.data.model

data class ErrorDetail(
    val loc: List<String>,
    val msg: String,
    val type: String
)

data class ErrorResponse(
    val detail: List<ErrorDetail>
)
