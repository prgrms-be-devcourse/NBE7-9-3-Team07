package com.back.pinco.domain.tag.dto.response

data class GetPinsByMultipleTagsResponse(
    val keywords: List<String>,
    val pins: List<GetFilteredPinResponse>
)
