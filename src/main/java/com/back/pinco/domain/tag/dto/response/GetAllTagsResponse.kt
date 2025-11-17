package com.back.pinco.domain.tag.dto.response

import com.back.pinco.domain.tag.dto.TagDto

data class GetAllTagsResponse(
    val tags: List<TagDto>
)
