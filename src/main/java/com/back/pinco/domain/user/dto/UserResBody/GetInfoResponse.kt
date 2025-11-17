package com.back.pinco.domain.user.dto.UserResBody

import com.back.pinco.domain.user.dto.UserDto

data class GetInfoResponse(
    val id: Long?,
    val email: String,
    val userName: String
) {
    constructor(userDto: UserDto) : this(
        userDto.id,
        userDto.email,
        userDto.userName
    )
}
