package com.back.pinco.domain.tag.repository

import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.tag.entity.PinTag
import com.back.pinco.domain.tag.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PinTagRepository : JpaRepository<PinTag, Long> {
    // 특정 핀에 연결된 삭제되지 않은 PinTag 목록 조회
    @Query("SELECT t FROM PinTag pt JOIN pt.tag t WHERE pt.pin.id = :pinId")
    fun findTagsByPinId(@Param("pinId") pinId: Long): List<Tag>

    // 특정 핀과 태그에 대한 PinTag 조회
    fun findByPin_IdAndTag_Id(pinId: Long, tagId: Long): PinTag?

    // 특정 태그 키워드와 연결된 삭제되지 않은 핀 목록 조회
    @Query(
        ("SELECT pt.pin FROM PinTag pt " +
                "JOIN pt.tag t " +
                "WHERE t.keyword = :keyword")
    )
    fun findPinsByTagKeyword(@Param("keyword") keyword: String): List<Pin>
}

