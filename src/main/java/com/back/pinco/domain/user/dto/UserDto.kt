package com.back.pinco.domain.user.dto

import com.back.pinco.domain.user.entity.User
import java.time.LocalDateTime

data class UserDto(
    val id: Long,
    val email: String,
    val userName: String,
    val password: String,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime
) {
    constructor(user: User) : this(
        user.id!!,
        user.email,
        user.userName,
        user.password,
        user.createdAt!!,
        user.modifiedAt!!
    )
}

