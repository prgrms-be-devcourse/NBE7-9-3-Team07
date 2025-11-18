package com.back.pinco.domain.tag.dto.request

import jakarta.validation.constraints.NotBlank

data class AddTagToPinRequest(
    @field:NotBlank
    val keyword: String
) 