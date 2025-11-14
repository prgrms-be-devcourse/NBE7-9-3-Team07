package com.back.pinco.domain.bookmark.dto

import com.back.pinco.domain.bookmark.entity.Bookmark
import com.back.pinco.domain.pin.dto.PinDto
import java.time.LocalDateTime

/**
 * 북마크 DTO
 * @param id 북마크 ID
 * @param pin 핀 정보
 * @param createdAt 생성일
 */

@JvmRecord
data class BookmarkDto(
    val id: Long?,
    val pin: PinDto,
    val createdAt: LocalDateTime
) {
    // 생성자 내에서 Java Getter 대신 Kotlin 프로퍼티 접근 사용
    constructor(bookmark: Bookmark) : this(
        bookmark.id,
        PinDto(bookmark.pin),
        bookmark.createdAt
    )
}