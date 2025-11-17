package com.back.pinco.domain.pin.dto

import jakarta.validation.constraints.NotBlank


data class UpdatePinContentRequest (

    @field:NotBlank
    val content: String
){}