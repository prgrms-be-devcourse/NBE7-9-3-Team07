package com.back.pinco.domain.user.dto.UserReqBody

data class JoinRequest(
    val email: String,
    val password: String,
    val userName: String
)
