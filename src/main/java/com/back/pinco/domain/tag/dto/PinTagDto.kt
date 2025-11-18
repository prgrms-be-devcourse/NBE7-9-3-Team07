package com.back.pinco.domain.tag.dto

import com.back.pinco.domain.tag.entity.PinTag

data class PinTagDto(
    val id: Long,
    val pinId: Long,
    val tag: TagDto
) {
    constructor(pinTag: PinTag) : this(
        pinTag.id!!,
        pinTag.pin.id!!,
        TagDto(pinTag.tag)
    )
}
