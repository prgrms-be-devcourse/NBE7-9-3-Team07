package com.back.pinco.domain.bookmark.repository

import com.back.pinco.domain.bookmark.entity.Bookmark
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface BookmarkRepository : JpaRepository<Bookmark, Long> {

    /**
     * 특정 사용자의 삭제되지 않은 북마크 목록 조회
     *
     * @param user 사용자 엔티티 (Non-nullable)
     * @return 삭제되지 않은 북마크 목록 (List<Bookmark>)
     */
    fun findByUserAndDeletedFalse(user: User): MutableList<Bookmark>

    /**
     * 특정 사용자가 특정 핀을 북마크했는지 확인 (삭제 여부와 관계없이)
     *
     * @param user 사용자 엔티티 (Non-nullable)
     * @param pin 핀 엔티티 (Non-nullable)
     * @return 북마크가 존재하면 Bookmark 객체, 없으면 null 반환
     */
    fun findByUserAndPin(user: User, pin: Pin): Bookmark?

    /**
     * 특정 사용자가 특정 핀을 이미 북마크했는지 확인 (삭제되지 않은 것만)
     *
     * @param user 사용자 엔티티 (Non-nullable)
     * @param pin 핀 엔티티 (Non-nullable)
     * @return 삭제되지 않은 북마크가 존재하면 Bookmark 객체, 없으면 null 반환
     */
    fun findByUserAndPinAndDeletedFalse(user: User, pin: Pin): Bookmark?

    /**
     * 특정 사용자가 특정 핀을 북마크했는지 여부 확인 (삭제 여부와 관계없이)
     */
    fun existsByUserAndPin(user: User, pin: Pin): Boolean
}