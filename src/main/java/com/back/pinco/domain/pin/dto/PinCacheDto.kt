package com.back.pinco.domain.pin.dto

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.tag.entity.PinTag
import com.back.pinco.domain.tag.entity.Tag


data class PinCacheDto(
    val id: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val content: String = "",
    val userId: Long = 0L,
    val pinTags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val public: Boolean = false,
    val createdAt: String = "",
    val modifiedAt: String? = null
) {
    constructor(pin: Pin) : this(
        pin.id ?: 0L,
        pin.point.getY(),
        pin.point.getX(),
        pin.content,
        pin.user.id ?: 0L,
        pin.pinTags.map(PinTag::tag).map(Tag::keyword),
        pin.likeCount,
        pin.isPublic,
        pin.createdAt?.toString() ?: "",
        pin.modifiedAt?.toString()
    )
}
