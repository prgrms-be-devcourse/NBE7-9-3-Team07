package com.back.pinco.domain.likes.service

import com.back.pinco.domain.likes.dto.LikedUserResponse
import com.back.pinco.domain.likes.dto.LikeStatusResponse
import com.back.pinco.domain.likes.dto.LikedPinsResponse
import com.back.pinco.domain.likes.entity.Likes
import com.back.pinco.domain.likes.repository.LikesRepository
import com.back.pinco.domain.pin.entity.Pin
import com.back.pinco.domain.pin.repository.PinRepository
import com.back.pinco.domain.user.entity.User
import com.back.pinco.domain.user.repository.UserRepository
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikesService(
    private val likesRepository: LikesRepository,
    private val pinRepository: PinRepository,
    private val userRepository: UserRepository
) {
    // 특정 핀에 대한 좋아요 수 조회
    @Transactional(readOnly = true)
    fun getLikesCount(pinId: Long): Int =
        likesRepository.countByPinId(pinId).toInt()


    // 좋아요 등록
    @Transactional
    fun toggleLikeOn(pinId: Long, userId: Long): LikeStatusResponse {
        val user = validateUser(userId)
        val pin = validatePin(pinId, userId)

        saveLike(pin, user)
        val likeCount = refreshPinLikeCount(pinId)

        return LikeStatusResponse(true, likeCount)
    }

    private fun saveLike(pin: Pin, user: User): Likes {
        return try {
            likesRepository.save(Likes(pin, user))
        } catch (e: Exception) {
            throw ServiceException(ErrorCode.LIKES_CREATE_FAILED)
        }
    }


    // 좋아요 취소
    @Transactional
    fun toggleLikeOff(pinId: Long, userId: Long): LikeStatusResponse {
        val user = validateUser(userId)
        val pin = validatePin(pinId, userId)

        deleteLike(pin, user)
        val likeCount = refreshPinLikeCount(pinId)

        return LikeStatusResponse(false, likeCount)
    }

    private fun deleteLike(pin: Pin, user: User) {
        val likes = likesRepository.findByPinIdAndUserId(pinId =pin.id!!,userId = user.id!!)
            ?: throw ServiceException(ErrorCode.LIKES_NOT_FOUND)

        try {
            likesRepository.delete(likes)
        } catch (e: Exception) {
            throw ServiceException(ErrorCode.LIKES_REVOKE_FAILED)
        }
    }


    private fun validatePin(pinId: Long, userId: Long): Pin =
        pinRepository.findAccessiblePinById(pinId, userId)
            ?: throw ServiceException(ErrorCode.LIKES_INVALID_PIN_INPUT)


    private fun validateUser(userId: Long): User =
        userRepository.findByIdOrNull(userId)
            ?: throw ServiceException(ErrorCode.LIKES_INVALID_USER_INPUT)



    @Transactional
    fun refreshPinLikeCount(pinId: Long): Int {
        return try {
            pinRepository.refreshLikeCount(pinId)
            getLikesCount(pinId)
        } catch (e: Exception) {
            throw ServiceException(ErrorCode.LIKES_UPDATE_PIN_FAILED)
        }
    }


    // 해당 핀을 좋아요 누른 유저 ID 목록 전달
    @Transactional(readOnly = true)
    fun getUsersWhoLikedPin(pinId: Long): List<LikedUserResponse> =
        likesRepository.findUsersByPinId(pinId)
            .map { LikedUserResponse.fromEntity(it) }


    // 특정 사용자가 좋아요 누른 핀 목록 전달
    @Transactional(readOnly = true)
    fun getPinsLikedByUser(userId: Long): List<LikedPinsResponse> =
        likesRepository.findPinsByUserId(userId)
            .filter { it.user.id == userId || it.isPublic }
            .map { LikedPinsResponse.fromEntry(it) }


    // 탈퇴한 사용자의 좋아요 삭제
    @Transactional
    fun deleteWithdrawnUserLikes(userId: Long) {
        val likedPins: List<Pin> = likesRepository.findPinsByUserId(userId)

        if (likedPins.isEmpty()) return

        try {
            likesRepository.deleteAllByUserId(userId)

            val pinsIds = likedPins
                .mapNotNull { it.id }
                .toTypedArray()

            pinRepository.refreshLikeCountBatch(pinsIds)
        } catch (e: Exception) {
            throw ServiceException(ErrorCode.LIKES_UPDATE_PIN_FAILED)
        }
    }
}