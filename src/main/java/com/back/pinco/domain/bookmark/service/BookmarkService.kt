package com.back.pinco.domain.bookmark.service;

import com.back.pinco.domain.bookmark.dto.BookmarkDto;
import com.back.pinco.domain.bookmark.entity.Bookmark;
import com.back.pinco.domain.bookmark.repository.BookmarkRepository;
import com.back.pinco.domain.pin.entity.Pin;
import com.back.pinco.domain.pin.service.PinService;
import com.back.pinco.domain.user.entity.User;
import com.back.pinco.domain.user.repository.UserRepository;
import com.back.pinco.global.exception.ErrorCode;
import com.back.pinco.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PinService pinService;

    /**
     * 북마크 추가
     *
     * @param userId 사용자 ID
     * @param pinId 핀 ID
     * @return 생성된 북마크 DTO
     */
    @Transactional
    public BookmarkDto addBookmark(Long userId, Long pinId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BOOKMARK_INVALID_USER_INPUT));
        Pin pin = pinService.findById(pinId, user);

        Bookmark bookmark = bookmarkRepository.findByUserAndPin(user, pin)
                .map(existingBookmark -> {
                    if (!existingBookmark.getDeleted()) {
                        throw new ServiceException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
                    }
                    existingBookmark.restore();
                    return existingBookmark;
                })
                .orElseGet(() -> new Bookmark(user, pin));

        Bookmark savedBookmark = bookmarkRepository.save(bookmark);

        return new BookmarkDto(savedBookmark);
    }

    /**
     * 사용자의 북마크 목록 조회
     *
     * @param userId 사용자 ID
     * @return 북마크 DTO 목록
     */
    public List<BookmarkDto> getMyBookmarks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BOOKMARK_INVALID_USER_INPUT));

        // 삭제되지 않은 북마크 목록만 조회
        List<Bookmark> bookmarks = bookmarkRepository.findByUserAndDeletedFalse(user);

        return bookmarks.stream()
                .map(BookmarkDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 북마크 삭제 (소프트 삭제)
     *
     * @param userId 사용자 ID
     * @param bookmarkId 북마크 ID
     */
    @Transactional
    public void deleteBookmark(Long userId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BOOKMARK_NOT_FOUND));

        // 소유자가 아니면 찾을 수 없음으로 처리
        if (!bookmark.getUser().getId().equals(userId)) {
            throw new ServiceException(ErrorCode.BOOKMARK_NOT_FOUND);
        }

        try {
            bookmark.setDeleted();
            bookmarkRepository.save(bookmark);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.BOOKMARK_DELETE_FAILED);
        }
    }

    /**
     * 북마크 복원
     *
     * @param userId 사용자 ID
     * @param bookmarkId 북마크 ID
     */
    @Transactional
    public void restoreBookmark(Long userId, Long bookmarkId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BOOKMARK_INVALID_USER_INPUT));

        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ServiceException(ErrorCode.BOOKMARK_NOT_FOUND));

        // 소유자가 아니면 찾을 수 없음으로 처리
        if (!bookmark.getUser().getId().equals(user.getId())) {
            throw new ServiceException(ErrorCode.BOOKMARK_NOT_FOUND);
        }

        try {
            bookmark.restore();
            bookmarkRepository.save(bookmark);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.BOOKMARK_RESTORE_FAILED);
        }
    }


}
