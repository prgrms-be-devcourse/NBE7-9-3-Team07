package com.back.pinco.domain.tag.dto.response

import com.back.pinco.domain.tag.dto.PinTagDto

data class AddTagToPinResponse(
    val pinId: Long,
    val pinTag: PinTagDto
) 