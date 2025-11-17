package com.back.pinco.domain.tag.dto.response

import com.back.pinco.domain.tag.dto.TagDto

data class GetTagsByPinResponse(
    val pinId: Long,
    val tags: List<TagDto>
)
