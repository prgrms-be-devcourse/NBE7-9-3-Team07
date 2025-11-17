package com.back.pinco.domain.tag.dto

import com.back.pinco.domain.tag.entity.Tag
import java.time.LocalDateTime

data class TagDto(
    val id: Long,
    val keyword: String,
    val createdAt: LocalDateTime
) {
    constructor(tag: Tag) : this(
        tag.id!!,
        tag.keyword,
        tag.createdAt!!
    )
}
