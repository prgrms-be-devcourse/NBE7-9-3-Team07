package com.back.pinco.domain.bookmark.service

import com.back.pinco.domain.bookmark.dto.BookmarkDto
import com.back.pinco.domain.bookmark.entity.Bookmark
import com.back.pinco.domain.bookmark.repository.BookmarkRepository
import com.back.pinco.domain.pin.service.PinService
import com.back.pinco.domain.user.repository.UserRepository
import com.back.pinco.global.exception.ErrorCode
import com.back.pinco.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val userRepository: UserRepository,
    private val pinService: PinService
) {
    /**
     * 북마크 추가
     * - 이미 북마크가 존재하면:
     * - 삭제 상태가 아니면 -> 충돌(ALREADY_EXISTS) 예외 발생
     * - 삭제 상태이면 -> 복원(restore) 처리
     * - 북마크가 없으면 -> 새로 생성
     *
     * @param userId 사용자 ID
     * @param pinId 핀 ID
     * @return 생성된 북마크 DTO
     */
    @Transactional
    fun addBookmark(userId: Long, pinId: Long): BookmarkDto {
        val user = userRepository.findById(userId)
            .orElseThrow { ServiceException(ErrorCode.BOOKMARK_INVALID_USER_INPUT) }

        val pin = pinService.findById(pinId, user)

        val bookmark: Bookmark = bookmarkRepository.findByUserAndPin(user, pin)?.run {
            // (this == Bookmark)
            if (!deleted) {
                // 삭제되지 않은 북마크가 있다면 예외처리(중복 생성 방지)
                throw ServiceException(ErrorCode.BOOKMARK_ALREADY_EXISTS)
            }
            // 삭제 상태라면 복원
            restore()
            this
        } ?: run {
            // Elvis Operator (?:): 기존 북마크가 null일 때 (새로운 객체 생성)
            // Bookmark 엔티티의 주 생성자를 사용 (val user, val pin)
            Bookmark(user, pin)
        }

        val savedBookmark = bookmarkRepository.save<Bookmark>(bookmark)

        return BookmarkDto(savedBookmark)
    }

    /**
     * 사용자의 북마크 목록 조회
     *
     * @param userId 사용자 ID
     * @return 북마크 DTO 목록
     */
    fun getMyBookmarks(userId: Long): List<BookmarkDto?> {
        val user = userRepository.findById(userId)
            .orElseThrow { ServiceException(ErrorCode.BOOKMARK_INVALID_USER_INPUT) }

        // 삭제되지 않은 북마크 목록만 조회
        val bookmarks: List<Bookmark> = bookmarkRepository.findByUserAndDeletedFalse(user)

        return bookmarks.map { BookmarkDto(it) }
    }

    /**
     * 북마크 삭제 (소프트 삭제)
     *
     * @param userId 사용자 ID
     * @param bookmarkId 북마크 ID
     */
    @Transactional
    fun deleteBookmark(userId: Long?, bookmarkId: Long) {
        val bookmark = bookmarkRepository.findById(bookmarkId)
            .orElseThrow { ServiceException(ErrorCode.BOOKMARK_NOT_FOUND) }

        // 소유자가 아니면 찾을 수 없음으로 처리
        if (bookmark.user.id != userId) {
            throw ServiceException(ErrorCode.BOOKMARK_NOT_FOUND)
        }

        bookmark.setDeleted()
        bookmarkRepository.save(bookmark)
    }

    /**
     * 북마크 복원
     *
     * @param userId 사용자 ID
     * @param bookmarkId 북마크 ID
     */
    @Transactional
    fun restoreBookmark(userId: Long, bookmarkId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { ServiceException(ErrorCode.BOOKMARK_INVALID_USER_INPUT) }

        val bookmark = bookmarkRepository.findById(bookmarkId)
            .orElseThrow { ServiceException(ErrorCode.BOOKMARK_NOT_FOUND) }

        // 소유자가 아니면 찾을 수 없음으로 처리
        if (bookmark.user.id != user.id) {
            throw ServiceException(ErrorCode.BOOKMARK_NOT_FOUND)
        }

        bookmark.restore()
        bookmarkRepository.save(bookmark)
    }
}
