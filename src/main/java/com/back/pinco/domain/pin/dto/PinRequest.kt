package com.back.pinco.domain.pin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class PinRequest(
    @field:NotNull
    @field:Min(-90)
    @field:Max(90)
    val latitude: Double,

    @field:NotNull
    @field:Min(-180)
    @field:Max(180)
    val longitude: Double,

    @field:NotBlank
    val content: String

) 