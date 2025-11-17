package com.back.pinco.domain.user.dto.UserResBody

import com.back.pinco.domain.user.dto.UserDto
import java.time.LocalDateTime

data class JoinResponse(
    val id: Long,
    val email: String,
    val userName: String,
    val createdAt: LocalDateTime?
) {
    constructor(userDto: UserDto) : this(
        userDto.id,
        userDto.email,
        userDto.userName,
        userDto.createdAt
    )
}

