package com.back.pinco.domain.likes.dto

import com.back.pinco.domain.pin.entity.Pin

/**
 * 사용자가 좋아요한 핀 목록
 * @param id 핀 ID
 * @param latitude 위도
 * @param longitude 경도
 * @param content 내용
 * @param userId 사용자 ID
 * @param pinTags 핀 태그
 * @param likeCount 좋아요 수
 * @param isPublic 공개여부
 */
data class LikedPinsResponse(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val content: String,
    val userId: Long,
    val pinTags: List<String>,
    val likeCount: Int,
    val isPublic: Boolean
) {
    companion object {
        fun fromEntry(pin: Pin) =
            LikedPinsResponse(
                id = pin.id!!,
                latitude = pin.point.y,
                longitude = pin.point.x,
                content = pin.content,
                userId = pin.user.id!!,
                pinTags = pin.pinTags.map { it.tag.keyword },
                likeCount = pin.likeCount,
                isPublic = pin.isPublic
            )
    }
}
