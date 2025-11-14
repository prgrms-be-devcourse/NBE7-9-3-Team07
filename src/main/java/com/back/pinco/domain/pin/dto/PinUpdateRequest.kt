package com.back.pinco.domain.pin.dto

import jakarta.validation.constraints.NotBlank


data class PinUpdateRequest (

    @field:NotBlank
    val content: String
){}