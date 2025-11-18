package com.back.pinco.domain.pin.dto

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.tag.entity.PinTag
import com.back.pinco.domain.tag.entity.Tag
import java.time.LocalDateTime

data class PinDto(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val content: String,
    val userId: Long,
    val pinTags: List<String>,
    val likeCount: Int,
    val isPublic: Boolean,
    val createdAt: LocalDateTime,
    val modifiedAt: LocalDateTime?
) {
    constructor(pin: Pin) : this(
        pin.id!!,
        pin.point.getY(),
        pin.point.getX(),
        pin.content,
        pin.user.id!!,
        pin.pinTags
            .map(PinTag::tag)
            .map(Tag::keyword),
        pin.likeCount,
        pin.isPublic,
        pin.createdAt!!,
        pin.modifiedAt
    )
}
