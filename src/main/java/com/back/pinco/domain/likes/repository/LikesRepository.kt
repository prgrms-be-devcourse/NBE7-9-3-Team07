package com.back.pinco.domain.likes.repository

import com.back.pinco.domain.likes.entity.Likes
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface LikesRepository : JpaRepository<Likes, Long> {
    /** 특정 핀에 대해 특정 사용자의 좋아요 엔티티 조회  */
    @Query("SELECT l FROM Likes l WHERE l.pin.id = :pinId AND l.user.id = :userId")
    fun findByPinIdAndUserId(@Param("pinId") pinId: Long, @Param("userId") userId: Long): Likes?

    /** 특정 핀에 대한 좋아요 수 조회  */
    @Query("SELECT COUNT(l) FROM Likes l WHERE l.pin.id = :pinId")
    fun countByPinId(@Param("pinId") pinId: Long): Long

    /** 특정 사용자가 누른 좋아요 수 조회  */
    @Query("SELECT COUNT(l) FROM Likes l WHERE l.user.id = :userId")
    fun countByUserId(@Param("userId") userId: Long): Long

    /** 특정 핀에 좋아요를 누른 모든 사용자 조회  */
    @Query("SELECT DISTINCT l.user FROM Likes l WHERE l.pin.id = :pinId")
    fun findUsersByPinId(@Param("pinId") pinId: Long): List<User>

    /** 특정 사용자가 좋아요한 모든 핀 엔티티 조회  */
    @Query("SELECT DISTINCT l.pin FROM Likes l WHERE l.user.id = :userId")
    fun findPinsByUserId(@Param("userId") userId: Long): List<Pin>

    /** 탈퇴한 사용자의 좋아요 삭제  */
    @Modifying
    @Query("DELETE FROM Likes l WHERE l.user.id = :userId")
    fun deleteAllByUserId(@Param("userId") userId: Long)
}
