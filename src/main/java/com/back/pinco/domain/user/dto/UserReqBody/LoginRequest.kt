package com.back.pinco.domain.user.dto.UserReqBody

data class LoginRequest(
    val email: String,
    val password: String
)
