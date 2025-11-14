package com.back.pinco.domain.likes.dto

import jakarta.validation.constraints.NotNull

data class PinLikesRequest(
    @field:NotNull
    val userId: Long?
)
