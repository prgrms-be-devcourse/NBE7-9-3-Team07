package com.back.pinco.domain.user.dto.UserResBody

import com.back.pinco.domain.user.dto.UserDto

data class MyPageResponse(
    val email: String,
    val userName: String,
    val myPinCount: Int,
    val bookmarkCount: Int,
    val likesCount: Long
) {
    constructor(
        userDto: UserDto,
        myPinCount: Int,
        bookmarkCount: Int,
        likesCount: Long
    ) : this(
        userDto.email,
        userDto.userName,
        myPinCount,
        bookmarkCount,
        likesCount
    )
}

