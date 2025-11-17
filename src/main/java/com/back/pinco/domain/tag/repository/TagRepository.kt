package com.back.pinco.domain.tag.repository

import com.back.pinco.domain.tag.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<Tag, Long> {
    // 키워드로 태그 조회
    fun findByKeyword(keyword: String?): Tag?

    // 키워드 존재 여부 확인
    fun existsByKeyword(keyword: String): Boolean
}

